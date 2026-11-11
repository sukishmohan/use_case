CREATE TABLE SUPPLIER (
  supplier_code VARCHAR(15) PRIMARY KEY,
  name          VARCHAR(80) NOT NULL,
  sku_prefix    CHAR(3)     NOT NULL,
  api_token     VARCHAR(64) NOT NULL,
  active        CHAR(1)     NOT NULL
);

CREATE TABLE SUPPLIER_FEED_STATE (
  supplier_code    VARCHAR(15) PRIMARY KEY REFERENCES SUPPLIER(supplier_code),
  last_sequence_no BIGINT      NOT NULL,
  last_feed_id     VARCHAR(30) NOT NULL,
  updated_at       TIMESTAMP   NOT NULL,
  version          INT         NOT NULL DEFAULT 0
);

CREATE TABLE BRAND_ALIAS (
  alias_key  VARCHAR(60) PRIMARY KEY,
  brand_name VARCHAR(60) NOT NULL
);

CREATE TABLE SKU_SEQUENCE (
  supplier_code VARCHAR(15) PRIMARY KEY REFERENCES SUPPLIER(supplier_code),
  next_value    BIGINT      NOT NULL
);

CREATE TABLE CATEGORY (
  category_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(60)  NOT NULL,
  parent_id   BIGINT REFERENCES CATEGORY(category_id),
  path        VARCHAR(250) NOT NULL UNIQUE,
  depth       INT          NOT NULL
);

CREATE TABLE PRODUCT (
  product_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  supplier_code       VARCHAR(15) NOT NULL REFERENCES SUPPLIER(supplier_code),
  supplier_product_id VARCHAR(30) NOT NULL,
  brand               VARCHAR(60) NOT NULL,
  category_id         BIGINT      NOT NULL REFERENCES CATEGORY(category_id),
  status              VARCHAR(8)  NOT NULL,
  last_feed_id        VARCHAR(30) NOT NULL,
  created_at          TIMESTAMP   NOT NULL,
  updated_at          TIMESTAMP   NOT NULL,
  CONSTRAINT uq_product UNIQUE (supplier_code, supplier_product_id)
);

CREATE TABLE PRODUCT_TEXT (
  product_id  BIGINT        NOT NULL REFERENCES PRODUCT(product_id),
  locale      CHAR(2)       NOT NULL,
  name        VARCHAR(150)  NOT NULL,
  description VARCHAR(1000),
  is_fallback CHAR(1)       NOT NULL,
  PRIMARY KEY (product_id, locale)
);

CREATE TABLE PRODUCT_ATTRIBUTE (
  product_id BIGINT       NOT NULL REFERENCES PRODUCT(product_id),
  attr_key   VARCHAR(40)  NOT NULL,
  attr_value VARCHAR(200) NOT NULL,
  PRIMARY KEY (product_id, attr_key)
);

CREATE TABLE VARIANT (
  sku                 VARCHAR(20)   PRIMARY KEY,
  product_id          BIGINT        NOT NULL REFERENCES PRODUCT(product_id),
  supplier_code       VARCHAR(15)   NOT NULL,
  supplier_variant_id VARCHAR(30)   NOT NULL,
  pack_qty            DECIMAL(10,3) NOT NULL,
  pack_unit           VARCHAR(3)    NOT NULL,
  barcode             CHAR(13)      NOT NULL,
  stock_qty           INT           NOT NULL,
  stock_status        VARCHAR(12)   NOT NULL,
  content_hash        CHAR(64)      NOT NULL,
  status              VARCHAR(8)    NOT NULL,
  last_feed_id        VARCHAR(30)   NOT NULL,
  CONSTRAINT uq_variant UNIQUE (supplier_code, supplier_variant_id)
);

CREATE TABLE PRICE_HISTORY (
  price_id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  sku                    VARCHAR(20)   NOT NULL REFERENCES VARIANT(sku),
  mrp                    DECIMAL(10,2) NOT NULL,
  base_price             DECIMAL(10,2) NOT NULL,
  gst_percent            DECIMAL(5,2)  NOT NULL,
  tax_amount             DECIMAL(10,2) NOT NULL,
  selling_price_incl_tax DECIMAL(10,2) NOT NULL,
  discount_pct           DECIMAL(5,2)  NOT NULL,
  unit_price             DECIMAL(10,2) NOT NULL,
  unit_basis             VARCHAR(3)    NOT NULL,
  valid_from             TIMESTAMP     NOT NULL,
  valid_to               TIMESTAMP,
  is_current             CHAR(1)       NOT NULL
);

CREATE TABLE FEED_LOG (
  feed_id              VARCHAR(30) PRIMARY KEY,
  supplier_code        VARCHAR(15) NOT NULL,
  sequence_no          BIGINT      NOT NULL,
  mode                 VARCHAR(5)  NOT NULL,
  effective_at         TIMESTAMP   NOT NULL,
  variants_received    INT         NOT NULL,
  inserted_count       INT         NOT NULL,
  updated_count        INT         NOT NULL,
  unchanged_count      INT         NOT NULL,
  products_deactivated INT         NOT NULL,
  rejected_count       INT         NOT NULL,
  categories_created   INT         NOT NULL,
  status               VARCHAR(10) NOT NULL,
  processed_at         TIMESTAMP   NOT NULL
);

CREATE TABLE FEED_REJECT (
  reject_id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  feed_id             VARCHAR(30)  NOT NULL REFERENCES FEED_LOG(feed_id),
  supplier_product_id VARCHAR(30)  NOT NULL,
  supplier_variant_id VARCHAR(30),
  code                VARCHAR(30)  NOT NULL,
  message             VARCHAR(200) NOT NULL
);
