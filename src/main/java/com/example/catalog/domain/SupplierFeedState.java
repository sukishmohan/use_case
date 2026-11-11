package com.example.catalog.domain;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "SUPPLIER_FEED_STATE")
public class SupplierFeedState {

    @Id
    private String supplierCode;
    private long lastSequenceNo;
    private String lastFeedId;
    private Instant updatedAt;

    @Version
    private int version;

    public String getSupplierCode() {
        return supplierCode;
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    public long getLastSequenceNo() {
        return lastSequenceNo;
    }

    public void setLastSequenceNo(long lastSequenceNo) {
        this.lastSequenceNo = lastSequenceNo;
    }

    public String getLastFeedId() {
        return lastFeedId;
    }

    public void setLastFeedId(String lastFeedId) {
        this.lastFeedId = lastFeedId;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
