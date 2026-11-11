package com.example.catalog.seed;

import com.example.catalog.diff.ContentHasher;
import com.example.catalog.diff.VariantHashInput;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Replaces the placeholder content_hash values written by data.sql with real C11 hashes,
 * so the first feed compares against hashes produced by the same code that processes feeds.
 *
 * <p>Only rows still holding the placeholder are touched, so running it again is harmless.
 */
@Component
public class SeedHashLoader implements ApplicationRunner {

    static final String PLACEHOLDER_HASH = "0".repeat(64);

    private static final Logger log = LoggerFactory.getLogger(SeedHashLoader.class);

    private static final String SEED_VARIANTS_SQL = """
            SELECT v.sku, v.product_id, v.pack_qty, v.pack_unit, v.barcode,
                   p.brand, c.path AS category_path, t.name AS name_en,
                   h.mrp, h.selling_price_incl_tax, h.gst_percent
            FROM VARIANT v
            JOIN PRODUCT p       ON p.product_id = v.product_id
            JOIN CATEGORY c      ON c.category_id = p.category_id
            JOIN PRODUCT_TEXT t  ON t.product_id = p.product_id AND t.locale = 'en'
            JOIN PRICE_HISTORY h ON h.sku = v.sku AND h.is_current = 'Y'
            WHERE v.content_hash = ?
            """;

    private final JdbcTemplate jdbc;

    public SeedHashLoader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<SeedVariant> seeds = jdbc.query(SEED_VARIANTS_SQL, (rs, i) -> new SeedVariant(
                rs.getString("sku"),
                rs.getLong("product_id"),
                new VariantHashInput(
                        rs.getString("name_en"),
                        rs.getString("brand"),
                        rs.getString("category_path"),
                        Map.of(), // replaced below once attributes are loaded
                        rs.getBigDecimal("pack_qty"),
                        rs.getString("pack_unit"),
                        rs.getBigDecimal("mrp"),
                        rs.getBigDecimal("selling_price_incl_tax"),
                        rs.getBigDecimal("gst_percent"),
                        rs.getString("barcode"))),
                PLACEHOLDER_HASH);

        for (SeedVariant seed : seeds) {
            VariantHashInput in = seed.input();
            VariantHashInput withAttributes = new VariantHashInput(
                    in.nameEn(), in.brand(), in.categoryPath(), loadAttributes(seed.productId()),
                    in.packQty(), in.packUnit(), in.mrp(), in.sellingInclTax(),
                    in.gstPercent(), in.barcode());
            jdbc.update("UPDATE VARIANT SET content_hash = ? WHERE sku = ?",
                    ContentHasher.hash(withAttributes), seed.sku());
        }
        log.info("Seed content hashes computed for {} variant(s)", seeds.size());
    }

    private Map<String, String> loadAttributes(long productId) {
        Map<String, String> attributes = new LinkedHashMap<>();
        jdbc.query("SELECT attr_key, attr_value FROM PRODUCT_ATTRIBUTE WHERE product_id = ?",
                rs -> {
                    attributes.put(rs.getString("attr_key"), rs.getString("attr_value"));
                },
                productId);
        return attributes;
    }

    private record SeedVariant(String sku, long productId, VariantHashInput input) {
    }
}
