# Supplier Catalogue Delta Sync

Spring Boot 3 / Java 17 service that ingests supplier product feeds, detects what changed
(content hash) and applies only the differences, keeping a full price history (SCD Type 2).

## Build and run

```
mvn clean verify          # build, test, JaCoCo report in target/site/jacoco/index.html
mvn spring-boot:run       # start on http://localhost:8080
```

## Status

- [x] Step 1: project setup, schema.sql, data.sql, entities, seed hash loader
- [ ] Step 2: DTOs, validation, token check, error handler
- [ ] Step 3: rules C2-C11 with unit tests
- [ ] Step 4: feed service (C1, C12, C13), transaction, as-of price endpoint
- [ ] Step 5: remaining tests and end-to-end test
- [ ] Step 6: refactor, curl samples, coverage thresholds

## Layout

```
com.example.catalog
  domain/       JPA entities (plain FK columns, no @ManyToOne, so no accidental N+1)
  repository/   Spring Data repositories
  diff/         pure Java: C11 content hashing (no Spring imports)
  seed/         startup loader that computes the seed content_hash values
```

## Assumptions

1. The assessment's seed table gives no product names, but the C11 hash needs `names.en`.
   Seed names used: AF-RICE-01 = "Premium Basmati Rice" (as in the sample feed, with `hi`
   "Premium Basmati Chawal" and `ta` as an English fallback); AF-OIL-07 = "Sunflower Oil"
   (`hi` and `ta` as English fallbacks). The section 8 expectations do not depend on them,
   because the sample feed changes the rice price anyway.
2. Seed rows are inserted with explicit ids, so `data.sql` restarts the identity counters of
   CATEGORY, PRODUCT and PRICE_HISTORY after them. Otherwise the first row the service
   creates would collide with an existing id.
3. All TIMESTAMP columns hold UTC wall-clock time (`hibernate.jdbc.time_zone=UTC`).
4. `data.sql` stores 64 zeros as a placeholder `content_hash`; `SeedHashLoader` replaces it at
   startup using the same C11 code that processes feeds. Canonical hash input: names.en,
   brand, category path, attributes sorted by key, pack quantity + unit, MRP, selling price
   incl. tax, GST %, barcode (stock excluded). Pack quantity and GST are written without
   trailing zeros so `1`, `1.0` and `1.000` hash the same.
5. The seed supplier feed state uses `last_feed_id = FD-ACMEFOODS-000044` and the seed
   products/variants use the same value for `last_feed_id`.
