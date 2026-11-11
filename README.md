# Supplier Catalogue Delta Sync

A Spring Boot REST API that processes supplier catalogue feeds, assigns permanent SKUs, detects product changes, and keeps effective-dated price history.

## Requirements

- Java 17 or newer
- Maven 3.6 or newer

The app uses an in-memory H2 database. `schema.sql` and `data.sql` create and seed it at startup; no separate database installation is required.

## Build and run

From the project root:

```bash
mvn clean verify
mvn spring-boot:run
```

`mvn clean verify` compiles the app, runs the tests, creates the JaCoCo report, and checks coverage for the `rules`, `diff`, and `service` packages (at least 80% line and 70% branch coverage). The report is at `target/site/jacoco/index.html`.

The API runs at `http://localhost:8080`. The database resets when the app stops. Seed credentials for local use:

```text
Supplier code: ACMEFOODS
Token:         acme-test-token
```

## API

All endpoints are rooted at `/api/v1/catalog`.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/suppliers/{supplierCode}/feeds` | Validate and process a feed; requires `X-Supplier-Token` |
| GET | `/skus/{sku}/prices?asOf={ISO-8601 timestamp}` | Get the price valid at `asOf`; omit it for the current price |
| GET | `/feeds/{feedId}` | Read the feed log and its rejects |

### Submit a feed

The complete DELTA sample is in [`examples/sample-feed.json`](examples/sample-feed.json). It expects the seeded feed sequence (44), so restart the app before replaying it.

```bash
curl -i -X POST http://localhost:8080/api/v1/catalog/suppliers/ACMEFOODS/feeds \
  -H 'Content-Type: application/json' \
  -H 'X-Supplier-Token: acme-test-token' \
  --data-binary @examples/sample-feed.json
```

The response is `200` with status `COMPLETED` or `PARTIAL`, counts, SKU changes, and variant rejects.

### Read prices and feed logs

```bash
# Current price
curl -i http://localhost:8080/api/v1/catalog/skus/ACF-BAS-000001/prices

# Price valid at the requested instant
curl -i 'http://localhost:8080/api/v1/catalog/skus/ACF-BAS-000001/prices?asOf=2026-11-01T00:00:00Z'

# Feed result and rejects
curl -i http://localhost:8080/api/v1/catalog/feeds/FD-ACMEFOODS-000045
```

### Error responses

Errors are JSON objects with `code`, `message`, and `details`.

| HTTP | Example |
| --- | --- |
| 400 | Malformed JSON or structural validation failure |
| 401 | Missing or invalid supplier token |
| 404 | Unknown supplier, SKU, or feed ID |
| 409 | Duplicate or stale sequence number |
| 422 | Sequence gap or FULL-feed mass-delete guard |

Malformed JSON (`400`):

```bash
curl -i -X POST http://localhost:8080/api/v1/catalog/suppliers/ACMEFOODS/feeds \
  -H 'Content-Type: application/json' -H 'X-Supplier-Token: acme-test-token' \
  --data '{not-json'
```

Invalid token (`401`) and unknown supplier (`404`):

```bash
curl -i -X POST http://localhost:8080/api/v1/catalog/suppliers/ACMEFOODS/feeds \
  -H 'Content-Type: application/json' -H 'X-Supplier-Token: wrong-token' \
  --data-binary @examples/sample-feed.json

curl -i -X POST http://localhost:8080/api/v1/catalog/suppliers/NOT-A-SUPPLIER/feeds \
  -H 'Content-Type: application/json' -H 'X-Supplier-Token: acme-test-token' \
  --data-binary @examples/sample-feed.json
```

Unknown SKU or feed ID (`404`):

```bash
curl -i http://localhost:8080/api/v1/catalog/skus/NO-SUCH-SKU/prices
curl -i http://localhost:8080/api/v1/catalog/feeds/FD-ACMEFOODS-999999
```

For `409`, submit the sample feed twice without restarting the app; the second request repeats an already processed sequence. For `422`, restart the app first, then use either example below. A failed 422 feed is logged as `FAILED` and does not advance the supplier's feed state.

```bash
# Sequence gap
curl -i -X POST http://localhost:8080/api/v1/catalog/suppliers/ACMEFOODS/feeds \
  -H 'Content-Type: application/json' -H 'X-Supplier-Token: acme-test-token' \
  --data-binary @examples/sequence-gap-feed.json

# FULL feed that would deactivate more than 20% of active products
curl -i -X POST http://localhost:8080/api/v1/catalog/suppliers/ACMEFOODS/feeds \
  -H 'Content-Type: application/json' -H 'X-Supplier-Token: acme-test-token' \
  --data-binary @examples/full-feed-mass-delete-guard.json
```

## Design

- `rules/` and `diff/` hold the pure Java normalisation and hash logic. Money uses `BigDecimal`, scale 2, and `HALF_UP` rounding.
- `service/` processes each feed transactionally and locks supplier feed state before checking its sequence.
- Products, variants, and current prices are bulk loaded. Rejected variants do not consume SKU numbers.
- Price changes close the current history row and insert a new one. Stock-only changes do not create price rows.
- Products are deactivated rather than deleted. A FULL feed is guarded against deactivating more than 20% of active products.
- `SeedHashLoader` computes seed content hashes with the same C11 implementation used for incoming feeds.
- Database timestamps are stored and queried as UTC; the relevant Hibernate settings are in `application.properties`.
- JaCoCo thresholds are configured in `pom.xml` for `rules`, `diff`, and `service`.

## Assumptions

1. The seed data does not specify every product name. The seed uses `Premium Basmati Rice` for `AF-RICE-01` and `Sunflower Oil` for `AF-OIL-07` so C11 hashes can be computed.
2. Seed rows have explicit IDs. `data.sql` resets the CATEGORY, PRODUCT, and PRICE_HISTORY identity counters after those rows.
3. `data.sql` uses a 64-zero placeholder for `content_hash`; `SeedHashLoader` replaces it during startup. The hash includes English name, normalised brand, category path, sorted attributes, pack, MRP, selling price including tax, GST, and barcode. It excludes stock.
4. The seed feed state starts at sequence 44 with feed ID `FD-ACMEFOODS-000044`. Seed products and variants reference that feed ID.

## Project structure

```text
com.example.catalog
  api/         REST controllers, DTOs, validation, and error mapping
  domain/      JPA entities
  repository/  Spring Data repositories
  diff/        C11 canonical content hashing
  rules/       C2-C10 normalization and business rules
  service/     feed orchestration and failed-feed logging
  seed/        startup seed hash loader
```
