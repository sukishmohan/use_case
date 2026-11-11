package com.example.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.catalog.api.FeedRequestValidator;
import com.example.catalog.api.dto.*;
import com.example.catalog.domain.*;
import com.example.catalog.repository.*;
import com.example.catalog.service.FeedService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Mandatory assessment worked-example, plus its reference scenarios against one seeded database. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:workedexample;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class WorkedExampleEndToEndTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired FeedService feedService;
    @Autowired FeedRequestValidator validator;
    @Autowired SupplierFeedStateRepository states;
    @Autowired SkuSequenceRepository sequences;
    @Autowired ProductRepository products;
    @Autowired ProductTextRepository texts;
    @Autowired ProductAttributeRepository attributes;
    @Autowired CategoryRepository categories;
    @Autowired VariantRepository variants;
    @Autowired PriceHistoryRepository prices;
    @Autowired FeedLogRepository feedLogs;
    @Autowired FeedRejectRepository rejects;

    @Test void C1_C3_C7_C11_C12_C13_workedExamplePersistsCatalogueAndReferenceScenarios() throws Exception {
        // A late database constraint failure must roll back any new category, product, SKU and state changes.
        FeedRequest overflow = request(45, "2026-11-06T11:30:00+05:30", List.of(overflowProduct()));
        validator.validateFeedRules(overflow);
        assertThatThrownBy(() -> feedService.process("ACMEFOODS", overflow)).isInstanceOf(RuntimeException.class);
        assertThat(states.findById("ACMEFOODS")).get().extracting(SupplierFeedState::getLastSequenceNo).isEqualTo(44L);
        assertThat(sequences.findById("ACMEFOODS")).get().extracting(SkuSequence::getNextValue).isEqualTo(3L);
        assertThat(products.findBySupplierCode("ACMEFOODS")).noneMatch(p -> p.getSupplierProductId().equals("AF-ROLLBACK"));
        assertThat(categories.findAll()).noneMatch(c -> c.getPath().equals("New Category"));
        assertThat(feedLogs.findById(feedId(45))).isEmpty();

        FeedRequest sample = sampleFeed();
        String sampleJson = json.writeValueAsString(sample);
        String responseJson = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/catalog/suppliers/ACMEFOODS/feeds")
                        .header("X-Supplier-Token", "acme-test-token")
                        .contentType(MediaType.APPLICATION_JSON).content(sampleJson))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();
        FeedResult result = json.readValue(responseJson, FeedResult.class);

        assertThat(result.feedId()).isEqualTo(feedId(45));
        assertThat(result.status()).isEqualTo("PARTIAL");
        assertThat(result.counts()).isEqualTo(new FeedResult.Counts(4, 2, 1, 0, 1, 1, 2));
        assertThat(result.variants()).extracting(FeedResult.VariantResult::supplierVariantId,
                FeedResult.VariantResult::sku, FeedResult.VariantResult::change)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("AF-RICE-01-1KG", "ACF-BAS-000001", "UPDATED"),
                        org.assertj.core.groups.Tuple.tuple("AF-RICE-01-5KG", "ACF-BAS-000003", "INSERTED"),
                        org.assertj.core.groups.Tuple.tuple("AF-DAL-03-1KG", "ACF-TOO-000004", "INSERTED"));
        assertThat(result.rejects()).singleElement().satisfies(r -> {
            assertThat(r.supplierVariantId()).isEqualTo("AF-DAL-03-500G");
            assertThat(r.code()).isEqualTo("PRICE_ABOVE_MRP");
            assertThat(r.message()).isEqualTo("Selling price incl. tax 120.00 > MRP 110.00");
        });

        SupplierFeedState feedState = states.findById("ACMEFOODS").orElseThrow();
        assertThat(feedState.getLastSequenceNo()).isEqualTo(45);
        assertThat(feedState.getLastFeedId()).isEqualTo(feedId(45));
        assertThat(sequences.findById("ACMEFOODS")).get().extracting(SkuSequence::getNextValue).isEqualTo(5L);
        assertThat(rejects.findAll()).singleElement().satisfies(r -> {
            assertThat(r.getFeedId()).isEqualTo(feedId(45));
            assertThat(r.getSupplierProductId()).isEqualTo("AF-DAL-03");
            assertThat(r.getSupplierVariantId()).isEqualTo("AF-DAL-03-500G");
            assertThat(r.getCode()).isEqualTo("PRICE_ABOVE_MRP");
        });

        Product rice = product("AF-RICE-01");
        assertThat(rice.getBrand()).isEqualTo("Golden Harvest");
        Category riceCategory = categories.findById(rice.getCategoryId()).orElseThrow();
        assertThat(riceCategory.getPath()).isEqualTo("Staples > Rice > Basmati");
        assertThat(texts.findById(new ProductText.Key(rice.getProductId(), "en")).orElseThrow().getName())
                .isEqualTo("Premium Basmati Rice");
        assertThat(texts.findById(new ProductText.Key(rice.getProductId(), "hi")).orElseThrow().getName())
                .isEqualTo("Premium Basmati Chawal");
        ProductText riceTa = texts.findById(new ProductText.Key(rice.getProductId(), "ta")).orElseThrow();
        assertThat(riceTa.getName()).isEqualTo("Premium Basmati Rice");
        assertThat(riceTa.getIsFallback()).isEqualTo("Y");
        assertThat(attributes.findAll()).filteredOn(a -> a.getId().getProductId().equals(rice.getProductId()))
                .extracting(a -> a.getId().getAttrKey(), ProductAttribute::getAttrValue)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("origin", "Punjab, India"),
                        org.assertj.core.groups.Tuple.tuple("is_organic", "Y"));

        Category pulses = categories.findAll().stream().filter(c -> c.getPath().equals("Staples > Pulses")).findFirst().orElseThrow();
        Category toorDal = categories.findAll().stream().filter(c -> c.getPath().equals("Staples > Pulses > Toor Dal")).findFirst().orElseThrow();
        assertThat(pulses.getDepth()).isEqualTo(2);
        assertThat(pulses.getParentId()).isEqualTo(categories.findAll().stream()
                .filter(c -> c.getPath().equals("Staples")).findFirst().orElseThrow().getCategoryId());
        assertThat(toorDal.getDepth()).isEqualTo(3);
        assertThat(toorDal.getParentId()).isEqualTo(pulses.getCategoryId());
        assertThat(categories.count()).isEqualTo(7);

        Variant rice1 = variants.findById("ACF-BAS-000001").orElseThrow();
        Variant rice5 = variants.findById("ACF-BAS-000003").orElseThrow();
        Variant dal1 = variants.findById("ACF-TOO-000004").orElseThrow();
        assertThat(rice1.getStockQty()).isEqualTo(120);
        assertThat(rice1.getStockStatus()).isEqualTo("IN_STOCK");
        assertThat(rice5.getStockQty()).isEqualTo(8);
        assertThat(rice5.getStockStatus()).isEqualTo("LOW");
        assertThat(dal1.getStockQty()).isEqualTo(40);
        assertThat(dal1.getStockStatus()).isEqualTo("IN_STOCK");
        assertThat(variants.findBySupplierCodeAndSupplierVariantIdIn("ACMEFOODS", List.of("AF-DAL-03-500G"))).isEmpty();

        List<PriceHistory> riceHistory = prices.findBySkuOrderByValidFromDesc(rice1.getSku());
        assertThat(riceHistory).hasSize(2);
        PriceHistory oldRice = riceHistory.stream().filter(p -> "N".equals(p.getIsCurrent())).findFirst().orElseThrow();
        assertThat(oldRice.getSellingPriceInclTax()).isEqualByComparingTo("160.00");
        assertThat(oldRice.getValidTo()).isEqualTo(Instant.parse("2026-11-06T06:00:00Z"));
        PriceHistory newRice = riceHistory.stream().filter(p -> "Y".equals(p.getIsCurrent())).findFirst().orElseThrow();
        assertThat(newRice.getValidFrom()).isEqualTo(Instant.parse("2026-11-06T06:00:00Z"));
        assertThat(newRice.getBasePrice()).isEqualByComparingTo("157.14");
        assertThat(newRice.getTaxAmount()).isEqualByComparingTo("7.86");
        assertThat(newRice.getDiscountPct()).isEqualByComparingTo("8.33");
        assertThat(newRice.getUnitPrice()).isEqualByComparingTo("165.00");
        assertThat(newRice.getUnitBasis()).isEqualTo("KG");
        PriceHistory rice5Price = prices.findBySkuOrderByValidFromDesc(rice5.getSku()).get(0);
        assertThat(rice5Price.getBasePrice()).isEqualByComparingTo("760.95");
        assertThat(rice5Price.getTaxAmount()).isEqualByComparingTo("38.05");
        assertThat(rice5Price.getDiscountPct()).isEqualByComparingTo("6.00");
        assertThat(rice5Price.getUnitPrice()).isEqualByComparingTo("159.80");
        PriceHistory dalPrice = prices.findBySkuOrderByValidFromDesc(dal1.getSku()).get(0);
        assertThat(dalPrice.getBasePrice()).isEqualByComparingTo("189.00");
        assertThat(dalPrice.getTaxAmount()).isEqualByComparingTo("9.45");
        assertThat(dalPrice.getSellingPriceInclTax()).isEqualByComparingTo("198.45");
        assertThat(dalPrice.getDiscountPct()).isEqualByComparingTo("5.50");
        assertThat(dalPrice.getUnitPrice()).isEqualByComparingTo("198.45");

        Product oil = product("AF-OIL-07");
        assertThat(oil.getStatus()).isEqualTo("INACTIVE");
        Variant oilVariant = variants.findById("ACF-SUN-000002").orElseThrow();
        assertThat(oilVariant.getStatus()).isEqualTo("INACTIVE");
        PriceHistory oilClosed = prices.findBySkuOrderByValidFromDesc(oilVariant.getSku()).get(0);
        assertThat(oilClosed.getIsCurrent()).isEqualTo("N");
        assertThat(oilClosed.getValidTo()).isEqualTo(Instant.parse("2026-11-06T06:00:00Z"));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/catalog/skus/ACF-BAS-000001/prices").param("asOf", "2026-11-01T00:00:00Z"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.sellingPriceInclTax").value(160.00));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/catalog/skus/ACF-BAS-000001/prices").param("asOf", "2026-11-06T06:00:00Z"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.sellingPriceInclTax").value(165.00));

        // Reference scenario: same feed is stale (409); a sequence gap persists only FAILED FEED_LOG (422).
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/catalog/suppliers/ACMEFOODS/feeds")
                        .header("X-Supplier-Token", "acme-test-token").contentType(MediaType.APPLICATION_JSON).content(sampleJson))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isConflict());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/catalog/suppliers/ACMEFOODS/feeds")
                        .header("X-Supplier-Token", "acme-test-token").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request(47, "2026-11-07T00:00:00Z", List.of(riceProduct(165, 120))))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnprocessableEntity())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value("SEQUENCE_GAP"));
        assertThat(feedLogs.findById(feedId(47))).get().satisfies(l -> assertThat(l.getStatus()).isEqualTo("FAILED"));
        assertThat(states.findById("ACMEFOODS")).get().extracting(SupplierFeedState::getLastSequenceNo).isEqualTo(45L);

        FeedResult stockOnly = submit(request(46, "2026-11-07T00:00:00Z", List.of(riceProduct(165, 5))));
        assertThat(stockOnly.counts().unchanged()).isEqualTo(1);
        assertThat(variants.findById("ACF-BAS-000001")).get().satisfies(v -> {
            assertThat(v.getStockQty()).isEqualTo(5); assertThat(v.getStockStatus()).isEqualTo("LOW");
        });
        assertThat(prices.findBySkuOrderByValidFromDesc("ACF-BAS-000001")).hasSize(2);

        FeedResult reactivated = submit(request(47, "2026-11-08T00:00:00Z", List.of(oilProduct())));
        assertThat(reactivated.counts().updated()).isEqualTo(1);
        assertThat(product("AF-OIL-07").getStatus()).isEqualTo("ACTIVE");
        assertThat(variants.findById("ACF-SUN-000002")).get().satisfies(v -> assertThat(v.getStatus()).isEqualTo("ACTIVE"));
        assertThat(prices.findBySkuOrderByValidFromDesc("ACF-SUN-000002")).hasSize(2)
                .anySatisfy(p -> assertThat(p.getIsCurrent()).isEqualTo("Y"));

        FeedRequest fullGuard = new FeedRequest(feedId(48), 48L, FeedMode.FULL,
                OffsetDateTime.parse("2026-11-09T00:00:00Z"), List.of(dalProduct()));
        validator.validateFeedRules(fullGuard);
        assertThatThrownBy(() -> feedService.process("ACMEFOODS", fullGuard))
                .isInstanceOf(com.example.catalog.api.ApiException.class)
                .satisfies(e -> assertThat(((com.example.catalog.api.ApiException) e).getCode()).isEqualTo("MASS_DELETE_GUARD"));
        assertThat(feedLogs.findById(feedId(48))).get().satisfies(l -> assertThat(l.getStatus()).isEqualTo("FAILED"));
        assertThat(states.findById("ACMEFOODS")).get().extracting(SupplierFeedState::getLastSequenceNo).isEqualTo(47L);
        assertThat(prices.findAll().stream().collect(java.util.stream.Collectors.groupingBy(PriceHistory::getSku,
                java.util.stream.Collectors.counting())).keySet())
                .allSatisfy(sku -> assertThat(prices.findBySkuOrderByValidFromDesc(sku).stream()
                        .filter(p -> "Y".equals(p.getIsCurrent())).count()).isLessThanOrEqualTo(1));
    }

    private FeedResult submit(FeedRequest request) {
        validator.validateFeedRules(request); return feedService.process("ACMEFOODS", request);
    }
    private Product product(String id) {
        return products.findBySupplierCodeAndSupplierProductIdIn("ACMEFOODS", List.of(id)).get(0);
    }
    private static FeedRequest sampleFeed() {
        return request(45, "2026-11-06T11:30:00+05:30", List.of(riceProductWithBothPacks(), deleteOil(), dalProduct()));
    }
    private static FeedRequest request(int sequence, String generatedAt, List<ProductRequest> products) {
        return new FeedRequest(feedId(sequence), (long) sequence, FeedMode.DELTA,
                OffsetDateTime.parse(generatedAt), products);
    }
    private static String feedId(int sequence) { return "FD-ACMEFOODS-%06d".formatted(sequence); }

    private static ProductRequest riceProductWithBothPacks() {
        ProductRequest base = riceProduct(165, 120);
        VariantRequest fiveKg = new VariantRequest("AF-RICE-01-5KG", new PackSizeRequest(new BigDecimal("5"), "KG"),
                new BigDecimal("850.00"), new BigDecimal("799.00"), true, 5, "8901234567807", 8);
        return new ProductRequest(base.supplierProductId(), ProductAction.UPSERT, " golden  harvest ",
                base.category(), base.names(), Map.of("en", "Aged long-grain basmati"), riceAttributes(),
                List.of(base.variants().get(0), fiveKg));
    }
    private static ProductRequest riceProduct(int sellingPrice, int stock) {
        CategoryRequest category = new CategoryRequest(" Basmati ",
                new CategoryRequest("Rice", new CategoryRequest(" staples ", null)));
        VariantRequest oneKg = new VariantRequest("AF-RICE-01-1KG", new PackSizeRequest(BigDecimal.ONE, "KG"),
                new BigDecimal("180.00"), BigDecimal.valueOf(sellingPrice), true, 5, "8901234567890", stock);
        Map<String, String> names = new LinkedHashMap<>(); names.put("en", "Premium Basmati Rice");
        names.put("hi", "Premium Basmati Chawal"); names.put("ta", null);
        return new ProductRequest("AF-RICE-01", ProductAction.UPSERT, " golden  harvest ", category, names,
                Map.of("en", "Aged long-grain basmati"), riceAttributes(), List.of(oneKg));
    }
    private static List<AttributeRequest> riceAttributes() {
        return List.of(new AttributeRequest("Origin", "India"), new AttributeRequest("Is Organic", "yes"),
                new AttributeRequest("origin", "Punjab, India"));
    }
    private static ProductRequest deleteOil() {
        return new ProductRequest("AF-OIL-07", ProductAction.DELETE, null, null, null, null, null, null);
    }
    private static ProductRequest oilProduct() {
        CategoryRequest category = new CategoryRequest("Sunflower", new CategoryRequest("Oils",
                new CategoryRequest("Staples", null)));
        VariantRequest variant = new VariantRequest("AF-OIL-07-1L", new PackSizeRequest(BigDecimal.ONE, "L"),
                new BigDecimal("199.00"), new BigDecimal("188.00"), true, 5, "8901234567876", 25);
        return new ProductRequest("AF-OIL-07", ProductAction.UPSERT, "Acme Foods", category,
                Map.of("en", "Sunflower Oil"), Map.of(), List.of(), List.of(variant));
    }
    private static ProductRequest dalProduct() {
        CategoryRequest category = new CategoryRequest("Toor Dal", new CategoryRequest("Pulses",
                new CategoryRequest("staples", null)));
        VariantRequest rejected = new VariantRequest("AF-DAL-03-500G", new PackSizeRequest(new BigDecimal("500"), "G"),
                new BigDecimal("110.00"), new BigDecimal("120.00"), true, 5, "8901234567821", 15);
        VariantRequest oneKg = new VariantRequest("AF-DAL-03-1KG", new PackSizeRequest(BigDecimal.ONE, "KG"),
                new BigDecimal("210.00"), new BigDecimal("189.00"), false, 5, "8901234567814", 40);
        return new ProductRequest("AF-DAL-03", ProductAction.UPSERT, "Acme Foods", category,
                Map.of("en", "Unpolished Toor Dal"), Map.of(), List.of(), List.of(rejected, oneKg));
    }
    private static ProductRequest overflowProduct() {
        CategoryRequest category = new CategoryRequest("New Category", null);
        VariantRequest overflow = new VariantRequest("AF-ROLLBACK-1", new PackSizeRequest(new BigDecimal("100000000"), "KG"),
                new BigDecimal("100.00"), new BigDecimal("90.00"), true, 5, "8901234567890", 1);
        return new ProductRequest("AF-ROLLBACK", ProductAction.UPSERT, "Rollback Brand", category,
                Map.of("en", "Rollback product"), Map.of(), List.of(), List.of(overflow));
    }
}
