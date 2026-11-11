package com.example.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.catalog.api.ApiException;
import com.example.catalog.api.CatalogQueryController;
import com.example.catalog.api.FeedRequestValidator;
import com.example.catalog.api.dto.*;
import com.example.catalog.domain.FeedLog;
import com.example.catalog.domain.Product;
import com.example.catalog.domain.SkuSequence;
import com.example.catalog.domain.SupplierFeedState;
import com.example.catalog.repository.*;
import com.example.catalog.service.FeedService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:feedtest;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class FeedProcessingIntegrationTest {
    @Autowired FeedService feedService;
    @Autowired FeedRequestValidator validator;
    @Autowired SupplierFeedStateRepository states;
    @Autowired FeedLogRepository feedLogs;
    @Autowired ProductRepository products;
    @Autowired VariantRepository variants;
    @Autowired PriceHistoryRepository priceHistory;
    @Autowired SkuSequenceRepository skuSequences;
    @Autowired CatalogQueryController queries;
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test void C1_C12_C13_feedSequencingHistoryRollbackAndDeactivation() {
        FeedRequest update = new FeedRequest(feedId(45), 45L, FeedMode.DELTA,
                OffsetDateTime.parse("2026-11-06T11:30:00+05:30"), List.of(riceWithNewPack(165, 120), rejectedDal()));
        FeedResult first;
        try {
            String body = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .post("/api/v1/catalog/suppliers/ACMEFOODS/feeds")
                            .header("X-Supplier-Token", "acme-test-token")
                            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(update)))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andReturn().getResponse().getContentAsString();
            first = objectMapper.readValue(body, FeedResult.class);
        } catch (Exception e) {
            throw new AssertionError("Feed POST endpoint should process the valid request", e);
        }
        assertThat(first.status()).isEqualTo("PARTIAL");
        assertThat(first.counts().inserted()).isEqualTo(1);
        assertThat(first.counts().updated()).isEqualTo(1);
        assertThat(first.counts().rejected()).isEqualTo(1);
        assertThat(first.variants()).extracting(FeedResult.VariantResult::sku)
                .containsExactly("ACF-BAS-000001", "ACF-BAS-000003");
        assertThat(variants.findById("ACF-BAS-000003")).isPresent();
        assertThat(products.findBySupplierCode("ACMEFOODS")).noneMatch(p -> p.getSupplierProductId().equals("AF-DAL-BAD"));
        assertThat(feedLogs.findById(feedId(45))).get().satisfies(log -> assertThat(log.getStatus()).isEqualTo("PARTIAL"));
        assertThat(skuSequences.findById("ACMEFOODS")).get().satisfies(s -> assertThat(s.getNextValue()).isEqualTo(4));
        assertThat(priceHistory.findBySkuOrderByValidFromDesc("ACF-BAS-000001")).hasSize(2);
        assertThat(priceHistory.findValidAt("ACF-BAS-000001", Instant.parse("2026-11-05T00:00:00Z")))
                .singleElement().satisfies(p -> assertThat(p.getSellingPriceInclTax()).isEqualByComparingTo("160.00"));
        assertThat(priceHistory.findValidAt("ACF-BAS-000001", Instant.parse("2026-11-06T06:00:00Z")))
                .singleElement().satisfies(p -> assertThat(p.getSellingPriceInclTax()).isEqualByComparingTo("165.00"));
        try {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .get("/api/v1/catalog/skus/ACF-BAS-000001/prices")
                            .param("asOf", "2026-11-05T00:00:00Z"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.sellingPriceInclTax").value(160.00));
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .get("/api/v1/catalog/feeds/{feedId}", feedId(45)))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status").value("PARTIAL"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.rejects[0].supplierProductId").value("AF-DAL-BAD"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.rejects[0].code").value("PRICE_ABOVE_MRP"));
        } catch (Exception e) {
            throw new AssertionError("Price and feed log GET endpoints should return stored data", e);
        }

        FeedResult stockOnly = submit(delta(46, "2026-11-07T10:00:00Z", rice(165, 5)));
        assertThat(stockOnly.counts().unchanged()).isEqualTo(1);
        assertThat(variants.findById("ACF-BAS-000001")).get().satisfies(v -> {
            assertThat(v.getStockQty()).isEqualTo(5);
            assertThat(v.getStockStatus()).isEqualTo("LOW");
        });
        assertThat(priceHistory.findBySkuOrderByValidFromDesc("ACF-BAS-000001")).hasSize(2);
        assertThat(queries.priceAt("ACF-BAS-000001", null).sellingPriceInclTax()).isEqualByComparingTo("165.00");
        assertThat(queries.feed(feedId(46)).status()).isEqualTo("COMPLETED");

        FeedRequest fullSkip = new FeedRequest(feedId(50), 50L, FeedMode.FULL,
                OffsetDateTime.parse("2026-11-08T10:00:00Z"), List.of(rice(165, 5), oil()));
        FeedResult fullResult = submit(fullSkip);
        assertThat(fullResult.counts().unchanged()).isEqualTo(2);
        assertThat(fullResult.counts().productsDeactivated()).isZero();

        FeedRequest stale = delta(50, "2026-11-08T10:00:00Z", rice(160, 7), feedId(47));
        assertThatThrownBy(() -> submit(stale)).isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(feedLogs.findById(feedId(47))).isEmpty();

        FeedRequest gap = delta(52, "2026-11-09T10:00:00Z", rice(155, 8));
        assertThatThrownBy(() -> submit(gap)).isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThat(feedLogs.findById(feedId(52))).get().satisfies(log -> assertThat(log.getStatus()).isEqualTo("FAILED"));
        assertThat(states.findById("ACMEFOODS")).get().satisfies(s -> assertThat(s.getLastSequenceNo()).isEqualTo(50));
        assertThat(variants.findById("ACF-BAS-000001")).get()
                .satisfies(v -> assertThat(v.getStockQty()).isEqualTo(5));

        FeedRequest massDelete = new FeedRequest(feedId(51), 51L, FeedMode.FULL,
                OffsetDateTime.parse("2026-11-10T10:00:00Z"), List.of(rice(165, 5)));
        assertThatThrownBy(() -> submit(massDelete)).isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThat(feedLogs.findById(feedId(51))).get().satisfies(log -> assertThat(log.getStatus()).isEqualTo("FAILED"));
        assertThat(states.findById("ACMEFOODS")).get().satisfies(s -> assertThat(s.getLastSequenceNo()).isEqualTo(50));

        ProductRequest deleteOil = new ProductRequest("AF-OIL-07", ProductAction.DELETE,
                null, null, null, null, null, null);
        FeedResult deleted = submit(delta(51, "2026-11-11T10:00:00Z", deleteOil, feedId(49)));
        assertThat(deleted.counts().productsDeactivated()).isEqualTo(1);
        assertThat(products.findBySupplierCode("ACMEFOODS")).filteredOn(p -> p.getSupplierProductId().equals("AF-OIL-07"))
                .singleElement().satisfies(p -> assertThat(p.getStatus()).isEqualTo("INACTIVE"));
        assertThat(priceHistory.findBySkuOrderByValidFromDesc("ACF-SUN-000002"))
                .singleElement().satisfies(p -> {
                    assertThat(p.getIsCurrent()).isEqualTo("N");
                    assertThat(p.getValidTo()).isEqualTo(Instant.parse("2026-11-11T10:00:00Z"));
                });
        FeedResult reactivated = submit(delta(52, "2026-11-12T10:00:00Z", oil(), feedId(53)));
        assertThat(reactivated.counts().updated()).isEqualTo(1);
        assertThat(products.findBySupplierCode("ACMEFOODS")).filteredOn(p -> p.getSupplierProductId().equals("AF-OIL-07"))
                .singleElement().satisfies(p -> assertThat(p.getStatus()).isEqualTo("ACTIVE"));
        assertThat(priceHistory.findBySkuOrderByValidFromDesc("ACF-SUN-000002")).hasSize(2)
                .anySatisfy(p -> assertThat(p.getIsCurrent()).isEqualTo("Y"));
        assertThat(states.findById("ACMEFOODS")).get().satisfies(s -> assertThat(s.getLastSequenceNo()).isEqualTo(52));
    }

    private FeedResult submit(FeedRequest request) {
        validator.validateFeedRules(request);
        return feedService.process("ACMEFOODS", request);
    }

    private static FeedRequest delta(int sequence, String generatedAt, ProductRequest product) {
        return delta(sequence, generatedAt, product, feedId(sequence));
    }

    private static FeedRequest delta(int sequence, String generatedAt, ProductRequest product, String id) {
        return new FeedRequest(id, (long) sequence, FeedMode.DELTA,
                OffsetDateTime.parse(generatedAt), List.of(product));
    }

    private static ProductRequest rice(int price, int stock) {
        CategoryRequest category = new CategoryRequest("Basmati",
                new CategoryRequest("Rice", new CategoryRequest("Staples", null)));
        VariantRequest variant = new VariantRequest("AF-RICE-01-1KG",
                new PackSizeRequest(BigDecimal.ONE, "KG"), new BigDecimal("180.00"),
                BigDecimal.valueOf(price), true, 5, "8901234567890", stock);
        return new ProductRequest("AF-RICE-01", null, " golden harvest ", category,
                Map.of("en", "Premium Basmati Rice", "hi", "Premium Basmati Chawal"),
                Map.of("en", "Aged long-grain basmati"), List.of(), List.of(variant));
    }

    private static ProductRequest riceWithNewPack(int price, int stock) {
        ProductRequest oneKg = rice(price, stock);
        VariantRequest fiveKg = new VariantRequest("AF-RICE-01-5KG", new PackSizeRequest(new BigDecimal("5"), "KG"),
                new BigDecimal("850.00"), new BigDecimal("799.00"), true, 5, "8901234567807", 8);
        return new ProductRequest(oneKg.supplierProductId(), oneKg.action(), oneKg.brand(), oneKg.category(),
                oneKg.names(), oneKg.descriptions(), oneKg.attributes(), List.of(oneKg.variants().get(0), fiveKg));
    }

    private static ProductRequest rejectedDal() {
        CategoryRequest category = new CategoryRequest("Toor Dal", new CategoryRequest("Pulses",
                new CategoryRequest("Staples", null)));
        VariantRequest badPrice = new VariantRequest("AF-DAL-BAD", new PackSizeRequest(new BigDecimal("500"), "G"),
                new BigDecimal("110.00"), new BigDecimal("120.00"), true, 5, "8901234567821", 15);
        return new ProductRequest("AF-DAL-BAD", null, "Acme Foods", category, Map.of("en", "Bad price item"),
                Map.of(), List.of(), List.of(badPrice));
    }

    private static ProductRequest oil() {
        CategoryRequest category = new CategoryRequest("Sunflower", new CategoryRequest("Oils",
                new CategoryRequest("Staples", null)));
        VariantRequest variant = new VariantRequest("AF-OIL-07-1L", new PackSizeRequest(BigDecimal.ONE, "L"),
                new BigDecimal("199.00"), new BigDecimal("188.00"), true, 5, "8901234567876", 25);
        return new ProductRequest("AF-OIL-07", null, "Acme Foods", category,
                Map.of("en", "Sunflower Oil"), Map.of(), List.of(), List.of(variant));
    }

    private static String feedId(int sequence) { return "FD-ACMEFOODS-%06d".formatted(sequence); }
}
