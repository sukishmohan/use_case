package com.example.catalog.service;

import com.example.catalog.api.ApiException;
import com.example.catalog.api.dto.*;
import com.example.catalog.diff.ContentHasher;
import com.example.catalog.diff.VariantHashInput;
import com.example.catalog.domain.*;
import com.example.catalog.repository.*;
import com.example.catalog.rules.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** All catalogue mutations for one feed are committed or rolled back together. */
@Service
public class TransactionalFeedProcessor {
    private final SupplierRepository suppliers;
    private final SupplierFeedStateRepository feedStates;
    private final ProductRepository products;
    private final VariantRepository variants;
    private final CategoryRepository categories;
    private final BrandAliasRepository aliases;
    private final SkuSequenceRepository skuSequences;
    private final PriceHistoryRepository priceHistory;
    private final ProductTextRepository productTexts;
    private final ProductAttributeRepository productAttributes;
    private final FeedLogRepository feedLogs;
    private final FeedRejectRepository feedRejects;

    public TransactionalFeedProcessor(SupplierRepository suppliers, SupplierFeedStateRepository feedStates,
            ProductRepository products, VariantRepository variants, CategoryRepository categories,
            BrandAliasRepository aliases, SkuSequenceRepository skuSequences, PriceHistoryRepository priceHistory,
            ProductTextRepository productTexts, ProductAttributeRepository productAttributes,
            FeedLogRepository feedLogs, FeedRejectRepository feedRejects) {
        this.suppliers = suppliers; this.feedStates = feedStates; this.products = products;
        this.variants = variants; this.categories = categories; this.aliases = aliases;
        this.skuSequences = skuSequences; this.priceHistory = priceHistory;
        this.productTexts = productTexts; this.productAttributes = productAttributes;
        this.feedLogs = feedLogs; this.feedRejects = feedRejects;
    }

    @Transactional
    public FeedResult process(String supplierCode, FeedRequest request) {
        String code = supplierCode.toUpperCase(Locale.ROOT);
        Supplier supplier = suppliers.findForUpdate(code)
                .filter(s -> "Y".equals(s.getActive()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SUPPLIER_NOT_FOUND", "Unknown or inactive supplier"));
        Optional<SupplierFeedState> lockedState = feedStates.findForUpdate(code);
        long lastSequence = lockedState.map(SupplierFeedState::getLastSequenceNo).orElse(0L);
        if (request.sequenceNo() <= lastSequence) {
            throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE_OR_STALE", "sequenceNo must be greater than the last processed sequence");
        }
        if (request.mode() == FeedMode.DELTA && request.sequenceNo() != lastSequence + 1) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "SEQUENCE_GAP", "DELTA sequenceNo must equal last sequence + 1");
        }

        Instant effectiveAt = request.generatedAt().toInstant();
        List<String> requestedProductIds = request.products().stream().map(ProductRequest::supplierProductId).toList();
        List<Product> feedProducts = products.findBySupplierCodeAndSupplierProductIdIn(code, requestedProductIds);
        Map<String, Product> productBySourceId = feedProducts.stream()
                .collect(Collectors.toMap(Product::getSupplierProductId, Function.identity()));
        List<Product> activeProducts = request.mode() == FeedMode.FULL
                ? products.findBySupplierCodeAndStatus(code, "ACTIVE") : List.of();
        activeProducts.forEach(p -> productBySourceId.putIfAbsent(p.getSupplierProductId(), p));
        Set<String> productIdsInFeed = new HashSet<>(requestedProductIds);
        List<Product> fullMissing = request.mode() == FeedMode.FULL
                ? activeProducts.stream().filter(p -> !productIdsInFeed.contains(p.getSupplierProductId())).toList()
                : List.of();
        if (request.mode() == FeedMode.FULL && !activeProducts.isEmpty()
                && fullMissing.size() * 5L > activeProducts.size()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "MASS_DELETE_GUARD",
                    "FULL feed would deactivate more than 20% of active products");
        }

        Set<Long> productIdsToDeactivate = new HashSet<>();
        if (request.mode() == FeedMode.FULL) fullMissing.forEach(p -> productIdsToDeactivate.add(p.getProductId()));
        else request.products().stream().filter(p -> p.action() == ProductAction.DELETE)
                .map(ProductRequest::supplierProductId).map(productBySourceId::get).filter(Objects::nonNull)
                .forEach(p -> productIdsToDeactivate.add(p.getProductId()));
        List<String> requestedVariantIds = request.products().stream().filter(p -> p.variants() != null)
                .flatMap(p -> p.variants().stream()).map(VariantRequest::supplierVariantId).toList();
        List<Variant> queriedVariants = requestedVariantIds.isEmpty() ? List.of()
                : variants.findBySupplierCodeAndSupplierVariantIdIn(code, requestedVariantIds);
        List<Variant> deactivationVariants = productIdsToDeactivate.isEmpty() ? List.of()
                : variants.findByProductIdIn(new ArrayList<>(productIdsToDeactivate));
        Map<String, Variant> variantBySourceId = new HashMap<>();
        queriedVariants.forEach(v -> variantBySourceId.put(v.getSupplierVariantId(), v));
        deactivationVariants.forEach(v -> variantBySourceId.put(v.getSupplierVariantId(), v));
        List<Variant> allAffectedVariants = new ArrayList<>(variantBySourceId.values());
        List<String> allSkus = allAffectedVariants.stream().map(Variant::getSku).toList();
        Map<String, PriceHistory> currentPrices = allSkus.isEmpty() ? new HashMap<>()
                : priceHistory.findBySkuInAndIsCurrent(allSkus, "Y").stream()
                    .collect(Collectors.toMap(PriceHistory::getSku, Function.identity()));
        Map<String, String> brandAliases = aliases.findAll().stream()
                .collect(Collectors.toMap(BrandAlias::getAliasKey, BrandAlias::getBrandName));
        Map<String, Category> categoryByPath = categories.findAll().stream()
                .collect(Collectors.toMap(c -> pathKey(c.getPath()), Function.identity(), (a, b) -> a));
        Set<String> conflictedBarcodes = loadConflictedBarcodes(code, request);

        SkuSequence sequence = skuSequences.findById(code).orElseGet(() -> {
            SkuSequence created = new SkuSequence(); created.setSupplierCode(code); created.setNextValue(1); return created;
        });
        long startingSkuNumber = sequence.getNextValue();
        MutableCounts counts = new MutableCounts();
        List<FeedResult.VariantResult> variantResults = new ArrayList<>();
        List<PendingReject> pendingRejects = new ArrayList<>();

        for (ProductRequest productRequest : request.products()) {
            String sourceProductId = productRequest.supplierProductId();
            Product product = productBySourceId.get(sourceProductId);
            ProductAction action = productRequest.action() == null ? ProductAction.UPSERT : productRequest.action();
            if (action == ProductAction.DELETE) {
                if (product == null) {
                    pendingRejects.add(new PendingReject(sourceProductId, null, "UNKNOWN_PRODUCT", "Unknown supplierProductId"));
                    counts.rejected++;
                } else if (deactivateProduct(product, allAffectedVariants, currentPrices, effectiveAt, request.feedId())) {
                    counts.productsDeactivated++;
                }
                continue;
            }
            counts.variantsReceived += productRequest.variants().size();
            processUpsert(code, supplier, request, productRequest, product, brandAliases, categoryByPath,
                    variantBySourceId, currentPrices, conflictedBarcodes, sequence, effectiveAt,
                    counts, variantResults, pendingRejects);
        }

        for (Product product : fullMissing) {
            if (deactivateProduct(product, allAffectedVariants, currentPrices, effectiveAt, request.feedId())) {
                counts.productsDeactivated++;
            }
        }

        if (sequence.getNextValue() != startingSkuNumber) skuSequences.save(sequence);
        SupplierFeedState state = lockedState.orElseGet(SupplierFeedState::new);
        state.setSupplierCode(code);
        state.setLastSequenceNo(request.sequenceNo());
        state.setLastFeedId(request.feedId());
        state.setUpdatedAt(Instant.now());
        feedStates.save(state);

        String status = counts.rejected == 0 ? "COMPLETED" : "PARTIAL";
        FeedLog feedLog = createLog(code, request, effectiveAt, counts, status);
        feedLogs.save(feedLog);
        List<FeedReject> rejectEntities = pendingRejects.stream().map(r -> {
            FeedReject reject = new FeedReject(); reject.setFeedId(request.feedId());
            reject.setSupplierProductId(r.productId()); reject.setSupplierVariantId(r.variantId());
            reject.setCode(r.code()); reject.setMessage(r.message()); return reject;
        }).toList();
        if (!rejectEntities.isEmpty()) feedRejects.saveAll(rejectEntities);
        List<FeedResult.RejectResult> rejectResults = pendingRejects.stream()
                .map(r -> new FeedResult.RejectResult(r.variantId(), r.code(), r.message())).toList();
        return new FeedResult(request.feedId(), status,
                new FeedResult.Counts(counts.variantsReceived, counts.inserted, counts.updated, counts.unchanged,
                        counts.productsDeactivated, counts.rejected, counts.categoriesCreated),
                List.copyOf(variantResults), rejectResults);
    }

    private void processUpsert(String supplierCode, Supplier supplier, FeedRequest feed, ProductRequest request,
            Product existingProduct, Map<String, String> brandAliases, Map<String, Category> categoryByPath,
            Map<String, Variant> variantBySourceId, Map<String, PriceHistory> currentPrices,
            Set<String> conflictedBarcodes, SkuSequence sequence, Instant effectiveAt,
            MutableCounts counts, List<FeedResult.VariantResult> variantResults, List<PendingReject> rejects) {
        String brand = BrandRules.normalize(request.brand(), brandAliases);
        List<String> categoryNames = CategoryRules.normalizedNames(request.category());
        String categoryPath = String.join(" > ", categoryNames);
        Map<String, String> normalizedAttributes = AttributeRules.normalize(request.attributes());
        Map<String, LocaleRules.Text> localeNames = LocaleRules.normalize(request.names());
        boolean reactivating = existingProduct != null && "INACTIVE".equals(existingProduct.getStatus());

        List<PreparedVariant> prepared = new ArrayList<>();
        List<SkuRules.Candidate> candidates = new ArrayList<>();
        for (VariantRequest input : request.variants()) {
            Variant existing = variantBySourceId.get(input.supplierVariantId());
            BarcodeRules.Result barcodeResult = BarcodeRules.validate(input.barcode(), supplierCode,
                    (barcode, ownerCode) -> conflictedBarcodes.contains(barcode));
            if (barcodeResult != BarcodeRules.Result.VALID) {
                String message = barcodeResult == BarcodeRules.Result.INVALID_BARCODE
                        ? "Barcode must be a valid EAN-13" : "Barcode is used by another supplier's active variant";
                rejects.add(new PendingReject(request.supplierProductId(), input.supplierVariantId(), barcodeResult.name(), message));
                counts.rejected++;
                prepared.add(PreparedVariant.rejected(input));
                candidates.add(new SkuRules.Candidate(input.supplierVariantId(), categoryNames.get(categoryNames.size() - 1), false, null));
                continue;
            }
            PricingRules.Price calculated;
            try {
                calculated = PricingRules.calculate(input.mrp(), input.sellingPrice(), input.priceIncludesTax(), input.gstPercent());
            } catch (PricingRules.PriceAboveMrpException e) {
                rejects.add(new PendingReject(request.supplierProductId(), input.supplierVariantId(), "PRICE_ABOVE_MRP", e.getMessage()));
                counts.rejected++;
                prepared.add(PreparedVariant.rejected(input));
                candidates.add(new SkuRules.Candidate(input.supplierVariantId(), categoryNames.get(categoryNames.size() - 1), false, null));
                continue;
            }
            StockRules.Stock stock = StockRules.normalize(input.stock());
            if (stock.correctedNegative()) {
                rejects.add(new PendingReject(request.supplierProductId(), input.supplierVariantId(),
                        "NEGATIVE_STOCK_CORRECTED", "Negative stock was corrected to 0"));
            }
            UnitPriceRules.UnitPrice unitPrice = UnitPriceRules.calculate(calculated.sellingInclTax(),
                    input.packSize().value(), input.packSize().unit());
            VariantHashInput hashInput = new VariantHashInput(localeNames.get("en").name(), brand, categoryPath,
                    normalizedAttributes, input.packSize().value(), input.packSize().unit(), input.mrp(),
                    calculated.sellingInclTax(), BigDecimal.valueOf(input.gstPercent()), input.barcode());
            String hash = ContentHasher.hash(hashInput);
            prepared.add(new PreparedVariant(input, existing, calculated, unitPrice, stock, hash, true));
            candidates.add(new SkuRules.Candidate(input.supplierVariantId(), categoryNames.get(categoryNames.size() - 1),
                    true, existing == null ? null : existing.getSku()));
        }

        if (prepared.stream().noneMatch(PreparedVariant::accepted)) return;
        SkuRules.Allocation allocation = SkuRules.allocate(supplier.getSkuPrefix(), sequence.getNextValue(), candidates);
        sequence.setNextValue(allocation.nextValue());
        Category category = resolveCategory(categoryNames, categoryByPath, counts);
        if (category == null) throw new IllegalStateException("Unable to resolve category " + categoryPath);

        boolean productWasNew = existingProduct == null;
        Product product = existingProduct == null ? new Product() : existingProduct;
        boolean productChanged = productWasNew || reactivating || !Objects.equals(product.getBrand(), brand)
                || !Objects.equals(product.getCategoryId(), category.getCategoryId());
        product.setSupplierCode(supplierCode);
        product.setSupplierProductId(request.supplierProductId());
        product.setBrand(brand);
        product.setCategoryId(category.getCategoryId());
        product.setStatus("ACTIVE");
        if (productWasNew) product.setCreatedAt(Instant.now());

        List<VariantWrite> writes = new ArrayList<>();
        for (int i = 0; i < prepared.size(); i++) {
            PreparedVariant item = prepared.get(i);
            if (!item.accepted()) continue;
            SkuRules.Assignment assignment = allocation.assignments().get(i);
            Variant variant = item.existing();
            String change;
            if (variant == null) change = "INSERTED";
            else if (reactivating || !"ACTIVE".equals(variant.getStatus())
                    || !Objects.equals(variant.getProductId(), existingProduct == null ? null : existingProduct.getProductId())
                    || !item.hash().equals(variant.getContentHash())) change = "UPDATED";
            else change = "UNCHANGED";
            if (change.equals("INSERTED")) counts.inserted++;
            else if (change.equals("UPDATED")) counts.updated++;
            else counts.unchanged++;
            if (!change.equals("UNCHANGED")) productChanged = true;
            writes.add(new VariantWrite(item, assignment.sku(), change));
        }

        if (productChanged) {
            product.setLastFeedId(feed.feedId());
            product.setUpdatedAt(Instant.now());
            product = products.save(product);
        }
        long productId = product.getProductId();
        boolean replaceTextAndAttributes = productWasNew || writes.stream().anyMatch(w -> "UPDATED".equals(w.change()));
        if (replaceTextAndAttributes) {
            replaceProductTexts(productId, request, localeNames);
            replaceAttributes(productId, normalizedAttributes);
        }
        for (VariantWrite write : writes) {
            Variant variant = write.prepared().existing();
            boolean isNew = variant == null;
            if (isNew) variant = new Variant();
            variant.setSku(write.sku());
            variant.setProductId(productId);
            variant.setSupplierCode(supplierCode);
            variant.setSupplierVariantId(write.prepared().input().supplierVariantId());
            if (!"UNCHANGED".equals(write.change())) {
                variant.setPackQty(write.prepared().input().packSize().value());
                variant.setPackUnit(write.prepared().input().packSize().unit());
                variant.setBarcode(write.prepared().input().barcode());
                variant.setContentHash(write.prepared().hash());
                variant.setStatus("ACTIVE");
                variant.setStockQty(write.prepared().stock().quantity());
                variant.setStockStatus(write.prepared().stock().status().name());
            } else {
                // C11: stock-only writes leave all content fields and price history untouched.
                variant.setStockQty(write.prepared().stock().quantity());
                variant.setStockStatus(write.prepared().stock().status().name());
            }
            variant.setLastFeedId(feed.feedId());
            variants.save(variant);
            variantBySourceId.put(variant.getSupplierVariantId(), variant);
            maybeWritePrice(variant, write.prepared(), currentPrices, effectiveAt);
            variantResults.add(new FeedResult.VariantResult(variant.getSupplierVariantId(), variant.getSku(), write.change()));
        }
    }

    private void replaceProductTexts(long productId, ProductRequest request, Map<String, LocaleRules.Text> names) {
        productTexts.deleteForProduct(productId);
        List<ProductText> rows = new ArrayList<>();
        for (String locale : List.of("en", "hi", "ta")) {
            ProductText text = new ProductText(); text.setId(new ProductText.Key(productId, locale));
            text.setName(names.get(locale).name());
            text.setDescription(request.descriptions() == null ? null : request.descriptions().get(locale));
            text.setIsFallback(names.get(locale).fallback() ? "Y" : "N"); rows.add(text);
        }
        productTexts.saveAll(rows);
    }

    private void replaceAttributes(long productId, Map<String, String> attributes) {
        productAttributes.deleteForProduct(productId);
        List<ProductAttribute> rows = attributes.entrySet().stream().map(entry -> {
            ProductAttribute attribute = new ProductAttribute();
            attribute.setId(new ProductAttribute.Key(productId, entry.getKey()));
            attribute.setAttrValue(entry.getValue()); return attribute;
        }).toList();
        if (!rows.isEmpty()) productAttributes.saveAll(rows);
    }

    private void maybeWritePrice(Variant variant, PreparedVariant prepared,
            Map<String, PriceHistory> currentPrices, Instant effectiveAt) {
        PricingRules.Price price = prepared.price();
        PriceHistory current = currentPrices.get(variant.getSku());
        boolean changed = current == null || current.getMrp().compareTo(prepared.input().mrp()) != 0
                || current.getBasePrice().compareTo(price.basePrice()) != 0
                || current.getGstPercent().compareTo(BigDecimal.valueOf(prepared.input().gstPercent())) != 0;
        if (!changed) return;
        if (current != null) {
            current.setValidTo(effectiveAt); current.setIsCurrent("N"); priceHistory.save(current);
        }
        PriceHistory row = new PriceHistory(); row.setSku(variant.getSku());
        row.setMrp(prepared.input().mrp().setScale(2, java.math.RoundingMode.HALF_UP));
        row.setBasePrice(price.basePrice()); row.setGstPercent(BigDecimal.valueOf(prepared.input().gstPercent()));
        row.setTaxAmount(price.taxAmount()); row.setSellingPriceInclTax(price.sellingInclTax());
        row.setDiscountPct(price.discountPct()); row.setUnitPrice(prepared.unitPrice().amount());
        row.setUnitBasis(prepared.unitPrice().basis()); row.setValidFrom(effectiveAt);
        row.setValidTo(null); row.setIsCurrent("Y");
        priceHistory.save(row); currentPrices.put(variant.getSku(), row);
    }

    private Category resolveCategory(List<String> names, Map<String, Category> categoryByPath, MutableCounts counts) {
        Long parentId = null;
        List<String> pathParts = new ArrayList<>();
        Category leaf = null;
        for (String name : names) {
            pathParts.add(name);
            String path = String.join(" > ", pathParts);
            String key = pathKey(path);
            Category category = categoryByPath.get(key);
            if (category == null) {
                category = new Category(); category.setName(name); category.setParentId(parentId);
                category.setPath(path); category.setDepth(pathParts.size());
                category = categories.save(category); categoryByPath.put(key, category); counts.categoriesCreated++;
            }
            parentId = category.getCategoryId(); leaf = category;
        }
        return leaf;
    }

    private Set<String> loadConflictedBarcodes(String supplierCode, FeedRequest request) {
        List<String> barcodes = request.products().stream().filter(p -> p.variants() != null)
                .flatMap(p -> p.variants().stream()).map(VariantRequest::barcode).filter(Objects::nonNull).distinct().toList();
        if (barcodes.isEmpty()) return Set.of();
        return variants.findByBarcodeInAndStatusAndSupplierCodeNot(barcodes, "ACTIVE", supplierCode).stream()
                .map(Variant::getBarcode).collect(Collectors.toSet());
    }

    private boolean deactivateProduct(Product product, List<Variant> allVariants,
            Map<String, PriceHistory> currentPrices, Instant effectiveAt, String feedId) {
        boolean wasActive = "ACTIVE".equals(product.getStatus());
        product.setStatus("INACTIVE"); product.setLastFeedId(feedId); product.setUpdatedAt(Instant.now());
        products.save(product);
        for (Variant variant : allVariants) {
            if (!Objects.equals(variant.getProductId(), product.getProductId())) continue;
            variant.setStatus("INACTIVE"); variant.setLastFeedId(feedId); variants.save(variant);
            PriceHistory current = currentPrices.get(variant.getSku());
            if (current != null && "Y".equals(current.getIsCurrent())) {
                current.setValidTo(effectiveAt); current.setIsCurrent("N");
                priceHistory.save(current); currentPrices.remove(variant.getSku());
            }
        }
        return wasActive;
    }

    private FeedLog createLog(String supplierCode, FeedRequest request, Instant effectiveAt,
            MutableCounts counts, String status) {
        FeedLog log = new FeedLog(); log.setFeedId(request.feedId()); log.setSupplierCode(supplierCode);
        log.setSequenceNo(request.sequenceNo()); log.setMode(request.mode().name()); log.setEffectiveAt(effectiveAt);
        log.setVariantsReceived(counts.variantsReceived); log.setInsertedCount(counts.inserted);
        log.setUpdatedCount(counts.updated); log.setUnchangedCount(counts.unchanged);
        log.setProductsDeactivated(counts.productsDeactivated); log.setRejectedCount(counts.rejected);
        log.setCategoriesCreated(counts.categoriesCreated); log.setStatus(status); log.setProcessedAt(Instant.now());
        return log;
    }

    private static String pathKey(String path) {
        return path.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private record PendingReject(String productId, String variantId, String code, String message) { }
    private record VariantWrite(PreparedVariant prepared, String sku, String change) { }
    private record PreparedVariant(VariantRequest input, Variant existing, PricingRules.Price price,
            UnitPriceRules.UnitPrice unitPrice, StockRules.Stock stock, String hash, boolean accepted) {
        static PreparedVariant rejected(VariantRequest request) {
            return new PreparedVariant(request, null, null, null, null, null, false);
        }
    }
    private static final class MutableCounts {
        int variantsReceived, inserted, updated, unchanged, productsDeactivated, rejected, categoriesCreated;
    }
}
