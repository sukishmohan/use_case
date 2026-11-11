package com.example.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.catalog.domain.PriceHistory;
import com.example.catalog.domain.Variant;
import com.example.catalog.repository.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Step 1 checkpoint: the application starts, schema.sql and data.sql load, every entity
 * maps onto its table, and the seed hash loader has replaced the placeholder hashes.
 */
@SpringBootTest
class SeededCatalogueSmokeTest {

    @Autowired SupplierRepository suppliers;
    @Autowired SupplierFeedStateRepository feedStates;
    @Autowired SkuSequenceRepository skuSequences;
    @Autowired BrandAliasRepository brandAliases;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired ProductTextRepository productTexts;
    @Autowired ProductAttributeRepository productAttributes;
    @Autowired VariantRepository variants;
    @Autowired PriceHistoryRepository priceHistory;
    @Autowired FeedLogRepository feedLogs;
    @Autowired FeedRejectRepository feedRejects;

    @Test
    void seedConfigurationIsLoaded() {
        assertThat(suppliers.findById("ACMEFOODS")).get()
                .satisfies(s -> {
                    assertThat(s.getSkuPrefix()).isEqualTo("ACF");
                    assertThat(s.getApiToken()).isEqualTo("acme-test-token");
                    assertThat(s.getActive()).isEqualTo("Y");
                });
        assertThat(feedStates.findById("ACMEFOODS")).get()
                .satisfies(s -> assertThat(s.getLastSequenceNo()).isEqualTo(44));
        assertThat(skuSequences.findById("ACMEFOODS")).get()
                .satisfies(s -> assertThat(s.getNextValue()).isEqualTo(3));
        assertThat(brandAliases.findById("goldenharvest")).get()
                .satisfies(a -> assertThat(a.getBrandName()).isEqualTo("Golden Harvest"));
    }

    @Test
    void seedCatalogueIsLoaded() {
        assertThat(categories.count()).isEqualTo(5);
        assertThat(products.count()).isEqualTo(2);
        assertThat(productTexts.count()).isEqualTo(6);
        assertThat(productAttributes.count()).isZero();
        assertThat(variants.count()).isEqualTo(2);
        assertThat(priceHistory.count()).isEqualTo(2);
        assertThat(feedLogs.count()).isZero();
        assertThat(feedRejects.count()).isZero();
    }

    @Test
    void seedPriceRowsAreCurrentAndOpenEnded() {
        List<PriceHistory> rows = priceHistory.findAll();

        assertThat(rows).allSatisfy(row -> {
            assertThat(row.getIsCurrent()).isEqualTo("Y");
            assertThat(row.getValidFrom()).isEqualTo(Instant.parse("2026-10-01T00:00:00Z"));
            assertThat(row.getValidTo()).isNull();
        });
    }

    @Test
    void C11_seedHashesAreComputedAtStartup() {
        List<Variant> all = variants.findAll();

        assertThat(all).hasSize(2).allSatisfy(v ->
                assertThat(v.getContentHash())
                        .matches("[0-9a-f]{64}")
                        .isNotEqualTo("0".repeat(64)));
        assertThat(all.get(0).getContentHash()).isNotEqualTo(all.get(1).getContentHash());
    }
}
