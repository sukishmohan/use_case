package com.example.catalog.api;

import com.example.catalog.api.dto.FeedLogView;
import com.example.catalog.api.dto.PriceHistoryView;
import com.example.catalog.domain.FeedLog;
import com.example.catalog.domain.FeedReject;
import com.example.catalog.domain.PriceHistory;
import com.example.catalog.repository.FeedLogRepository;
import com.example.catalog.repository.FeedRejectRepository;
import com.example.catalog.repository.PriceHistoryRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogQueryController {
    private final PriceHistoryRepository prices;
    private final FeedLogRepository feedLogs;
    private final FeedRejectRepository rejects;
    public CatalogQueryController(PriceHistoryRepository prices, FeedLogRepository feedLogs,
            FeedRejectRepository rejects) {
        this.prices = prices; this.feedLogs = feedLogs; this.rejects = rejects;
    }

    @GetMapping("/skus/{sku}/prices")
    public PriceHistoryView priceAt(@PathVariable String sku,
            @RequestParam(value = "asOf", required = false) OffsetDateTime asOf) {
        PriceHistory row = asOf == null
                ? prices.findFirstBySkuAndIsCurrent(sku, "Y").orElseThrow(() -> notFound("Price history not found"))
                : prices.findValidAt(sku, asOf.toInstant()).stream().findFirst()
                    .orElseThrow(() -> notFound("No price was valid for the requested instant"));
        return new PriceHistoryView(row.getSku(), row.getMrp(), row.getBasePrice(), row.getGstPercent(),
                row.getTaxAmount(), row.getSellingPriceInclTax(), row.getDiscountPct(), row.getUnitPrice(),
                row.getUnitBasis(), row.getValidFrom(), row.getValidTo(), "Y".equals(row.getIsCurrent()));
    }

    @GetMapping("/feeds/{feedId}")
    public FeedLogView feed(@PathVariable String feedId) {
        FeedLog log = feedLogs.findById(feedId).orElseThrow(() -> notFound("Feed not found"));
        List<FeedLogView.RejectView> feedRejects = rejects.findByFeedIdOrderByRejectId(feedId).stream()
                .map(r -> new FeedLogView.RejectView(r.getSupplierProductId(), r.getSupplierVariantId(), r.getCode(), r.getMessage())).toList();
        return new FeedLogView(log.getFeedId(), log.getSupplierCode(), log.getSequenceNo(), log.getMode(),
                log.getEffectiveAt(), log.getVariantsReceived(), log.getInsertedCount(), log.getUpdatedCount(),
                log.getUnchangedCount(), log.getProductsDeactivated(), log.getRejectedCount(),
                log.getCategoriesCreated(), log.getStatus(), log.getProcessedAt(), feedRejects);
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }
}
