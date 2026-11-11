-- Seed data for the ACMEFOODS supplier (minimum seed from the assessment).
-- content_hash values below are placeholders (64 zeros). SeedHashLoader replaces
-- them at startup using the real C11 hashing code.

INSERT INTO SUPPLIER VALUES ('ACMEFOODS', 'Acme Foods', 'ACF', 'acme-test-token', 'Y');

INSERT INTO SUPPLIER_FEED_STATE (supplier_code, last_sequence_no, last_feed_id, updated_at, version)
VALUES ('ACMEFOODS', 44, 'FD-ACMEFOODS-000044', TIMESTAMP '2026-10-31 12:00:00', 0);

INSERT INTO SKU_SEQUENCE VALUES ('ACMEFOODS', 3);

INSERT INTO BRAND_ALIAS VALUES ('goldenharvest', 'Golden Harvest');

INSERT INTO CATEGORY (category_id, name, parent_id, path, depth) VALUES
  (1, 'Staples',   NULL, 'Staples',                     1),
  (2, 'Rice',      1,    'Staples > Rice',              2),
  (3, 'Basmati',   2,    'Staples > Rice > Basmati',    3),
  (4, 'Oils',      1,    'Staples > Oils',              2),
  (5, 'Sunflower', 4,    'Staples > Oils > Sunflower',  3);

INSERT INTO PRODUCT (product_id, supplier_code, supplier_product_id, brand, category_id,
                     status, last_feed_id, created_at, updated_at) VALUES
  (1, 'ACMEFOODS', 'AF-RICE-01', 'Golden Harvest', 3, 'ACTIVE', 'FD-ACMEFOODS-000044',
      TIMESTAMP '2026-10-01 00:00:00', TIMESTAMP '2026-10-01 00:00:00'),
  (2, 'ACMEFOODS', 'AF-OIL-07',  'Acme Foods',     5, 'ACTIVE', 'FD-ACMEFOODS-000044',
      TIMESTAMP '2026-10-01 00:00:00', TIMESTAMP '2026-10-01 00:00:00');

-- Names are not given in the assessment's seed table; see README "Assumptions".
INSERT INTO PRODUCT_TEXT (product_id, locale, name, description, is_fallback) VALUES
  (1, 'en', 'Premium Basmati Rice',     NULL, 'N'),
  (1, 'hi', 'Premium Basmati Chawal',   NULL, 'N'),
  (1, 'ta', 'Premium Basmati Rice',     NULL, 'Y'),
  (2, 'en', 'Sunflower Oil',            NULL, 'N'),
  (2, 'hi', 'Sunflower Oil',            NULL, 'Y'),
  (2, 'ta', 'Sunflower Oil',            NULL, 'Y');

INSERT INTO VARIANT (sku, product_id, supplier_code, supplier_variant_id, pack_qty, pack_unit,
                     barcode, stock_qty, stock_status, content_hash, status, last_feed_id) VALUES
  ('ACF-BAS-000001', 1, 'ACMEFOODS', 'AF-RICE-01-1KG', 1, 'KG', '8901234567890', 60, 'IN_STOCK',
      '0000000000000000000000000000000000000000000000000000000000000000', 'ACTIVE', 'FD-ACMEFOODS-000044'),
  ('ACF-SUN-000002', 2, 'ACMEFOODS', 'AF-OIL-07-1L',    1, 'L',  '8901234567876', 25, 'IN_STOCK',
      '0000000000000000000000000000000000000000000000000000000000000000', 'ACTIVE', 'FD-ACMEFOODS-000044');

INSERT INTO PRICE_HISTORY (price_id, sku, mrp, base_price, gst_percent, tax_amount,
                           selling_price_incl_tax, discount_pct, unit_price, unit_basis,
                           valid_from, valid_to, is_current) VALUES
  (1, 'ACF-BAS-000001', 180.00, 152.38, 5, 7.62, 160.00, 11.11, 160.00, 'KG',
      TIMESTAMP '2026-10-01 00:00:00', NULL, 'Y'),
  (2, 'ACF-SUN-000002', 199.00, 179.05, 5, 8.95, 188.00,  5.53, 188.00, 'L',
      TIMESTAMP '2026-10-01 00:00:00', NULL, 'Y');

-- Rows were inserted with explicit ids, so move the identity counters past them.
-- Without this the first service-created row would get id 1 and collide.
ALTER TABLE CATEGORY      ALTER COLUMN category_id RESTART WITH 6;
ALTER TABLE PRODUCT       ALTER COLUMN product_id  RESTART WITH 3;
ALTER TABLE PRICE_HISTORY ALTER COLUMN price_id    RESTART WITH 3;
