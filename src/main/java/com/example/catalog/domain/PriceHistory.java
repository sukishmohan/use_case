package com.example.catalog.domain;

import java.math.BigDecimal;
import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "PRICE_HISTORY")
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long priceId;
    private String sku;
    private BigDecimal mrp;
    private BigDecimal basePrice;
    private BigDecimal gstPercent;
    private BigDecimal taxAmount;
    private BigDecimal sellingPriceInclTax;
    private BigDecimal discountPct;
    private BigDecimal unitPrice;
    private String unitBasis;
    /** Inclusive. */
    private Instant validFrom;
    /** Exclusive; null while the row is current. */
    private Instant validTo;
    /** 'Y' for the single open row of a SKU, otherwise 'N'. */
    private String isCurrent;

    public Long getPriceId() {
        return priceId;
    }

    public void setPriceId(Long priceId) {
        this.priceId = priceId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public BigDecimal getMrp() {
        return mrp;
    }

    public void setMrp(BigDecimal mrp) {
        this.mrp = mrp;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getGstPercent() {
        return gstPercent;
    }

    public void setGstPercent(BigDecimal gstPercent) {
        this.gstPercent = gstPercent;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getSellingPriceInclTax() {
        return sellingPriceInclTax;
    }

    public void setSellingPriceInclTax(BigDecimal sellingPriceInclTax) {
        this.sellingPriceInclTax = sellingPriceInclTax;
    }

    public BigDecimal getDiscountPct() {
        return discountPct;
    }

    public void setDiscountPct(BigDecimal discountPct) {
        this.discountPct = discountPct;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getUnitBasis() {
        return unitBasis;
    }

    public void setUnitBasis(String unitBasis) {
        this.unitBasis = unitBasis;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Instant validFrom) {
        this.validFrom = validFrom;
    }

    public Instant getValidTo() {
        return validTo;
    }

    public void setValidTo(Instant validTo) {
        this.validTo = validTo;
    }

    public String getIsCurrent() {
        return isCurrent;
    }

    public void setIsCurrent(String isCurrent) {
        this.isCurrent = isCurrent;
    }
}
