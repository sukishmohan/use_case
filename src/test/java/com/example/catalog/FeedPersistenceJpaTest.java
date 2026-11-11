package com.example.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.catalog.api.FeedRequestValidator;
import com.example.catalog.api.dto.FeedMode;
import com.example.catalog.api.dto.FeedRequest;
import com.example.catalog.api.dto.ProductAction;
import com.example.catalog.api.dto.ProductRequest;
import com.example.catalog.domain.PriceHistory;
import com.example.catalog.domain.Product;
import com.example.catalog.repository.PriceHistoryRepository;
import com.example.catalog.repository.ProductRepository;
import com.example.catalog.repository.VariantRepository;
import com.example.catalog.service.FailedFeedLogWriter;
import com.example.catalog.service.FeedService;
import com.example.catalog.service.TransactionalFeedProcessor;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:feedjpa;DB_CLOSE_DELAY=-1")
@Import({FeedService.class, TransactionalFeedProcessor.class, FailedFeedLogWriter.class, FeedRequestValidator.class})
class FeedPersistenceJpaTest {
    @Autowired FeedService feedService;
    @Autowired FeedRequestValidator validator;
    @Autowired ProductRepository products;
    @Autowired VariantRepository variants;
    @Autowired PriceHistoryRepository prices;

    @Test void C13_deltaDelete_marksProductAndVariantsInactiveAndClosesCurrentPriceRow() {
        FeedRequest request = new FeedRequest("FD-ACMEFOODS-000045", 45L, FeedMode.DELTA,
                OffsetDateTime.parse("2026-11-06T06:00:00Z"),
                List.of(new ProductRequest("AF-OIL-07", ProductAction.DELETE, null, null, null, null, null, null)));
        validator.validateFeedRules(request);
        feedService.process("ACMEFOODS", request);

        Product oil = products.findBySupplierCodeAndSupplierProductIdIn("ACMEFOODS", List.of("AF-OIL-07")).get(0);
        assertThat(oil.getStatus()).isEqualTo("INACTIVE");
        assertThat(variants.findById("ACF-SUN-000002")).get().satisfies(v -> assertThat(v.getStatus()).isEqualTo("INACTIVE"));
        PriceHistory closed = prices.findBySkuOrderByValidFromDesc("ACF-SUN-000002").get(0);
        assertThat(closed.getIsCurrent()).isEqualTo("N");
        assertThat(closed.getValidTo()).isEqualTo(Instant.parse("2026-11-06T06:00:00Z"));
        assertThat(prices.findValidAt("ACF-SUN-000002", Instant.parse("2026-11-06T05:59:59Z"))).hasSize(1);
        assertThat(prices.findValidAt("ACF-SUN-000002", Instant.parse("2026-11-06T06:00:00Z"))).isEmpty();
    }
}
