package com.example.catalog.domain;

import java.math.BigDecimal;
import jakarta.persistence.*;

@Entity
@Table(name = "VARIANT")
public class Variant {

    @Id
    private String sku;
    private Long productId;
    private String supplierCode;
    private String supplierVariantId;
    private BigDecimal packQty;
    private String packUnit;
    private String barcode;
    private int stockQty;
    private String stockStatus;
    private String contentHash;
    /** ACTIVE or INACTIVE. */
    private String status;
    private String lastFeedId;

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    public String getSupplierVariantId() {
        return supplierVariantId;
    }

    public void setSupplierVariantId(String supplierVariantId) {
        this.supplierVariantId = supplierVariantId;
    }

    public BigDecimal getPackQty() {
        return packQty;
    }

    public void setPackQty(BigDecimal packQty) {
        this.packQty = packQty;
    }

    public String getPackUnit() {
        return packUnit;
    }

    public void setPackUnit(String packUnit) {
        this.packUnit = packUnit;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public int getStockQty() {
        return stockQty;
    }

    public void setStockQty(int stockQty) {
        this.stockQty = stockQty;
    }

    public String getStockStatus() {
        return stockStatus;
    }

    public void setStockStatus(String stockStatus) {
        this.stockStatus = stockStatus;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastFeedId() {
        return lastFeedId;
    }

    public void setLastFeedId(String lastFeedId) {
        this.lastFeedId = lastFeedId;
    }
}
