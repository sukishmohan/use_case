package com.example.catalog.domain;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "FEED_LOG")
public class FeedLog {

    @Id
    private String feedId;
    private String supplierCode;
    private long sequenceNo;
    private String mode;
    private Instant effectiveAt;
    private int variantsReceived;
    private int insertedCount;
    private int updatedCount;
    private int unchangedCount;
    private int productsDeactivated;
    private int rejectedCount;
    private int categoriesCreated;
    /** COMPLETED, PARTIAL or FAILED. */
    private String status;
    private Instant processedAt;

    public String getFeedId() {
        return feedId;
    }

    public void setFeedId(String feedId) {
        this.feedId = feedId;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    public long getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(long sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Instant getEffectiveAt() {
        return effectiveAt;
    }

    public void setEffectiveAt(Instant effectiveAt) {
        this.effectiveAt = effectiveAt;
    }

    public int getVariantsReceived() {
        return variantsReceived;
    }

    public void setVariantsReceived(int variantsReceived) {
        this.variantsReceived = variantsReceived;
    }

    public int getInsertedCount() {
        return insertedCount;
    }

    public void setInsertedCount(int insertedCount) {
        this.insertedCount = insertedCount;
    }

    public int getUpdatedCount() {
        return updatedCount;
    }

    public void setUpdatedCount(int updatedCount) {
        this.updatedCount = updatedCount;
    }

    public int getUnchangedCount() {
        return unchangedCount;
    }

    public void setUnchangedCount(int unchangedCount) {
        this.unchangedCount = unchangedCount;
    }

    public int getProductsDeactivated() {
        return productsDeactivated;
    }

    public void setProductsDeactivated(int productsDeactivated) {
        this.productsDeactivated = productsDeactivated;
    }

    public int getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(int rejectedCount) {
        this.rejectedCount = rejectedCount;
    }

    public int getCategoriesCreated() {
        return categoriesCreated;
    }

    public void setCategoriesCreated(int categoriesCreated) {
        this.categoriesCreated = categoriesCreated;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}
