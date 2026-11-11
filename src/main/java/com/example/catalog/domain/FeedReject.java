package com.example.catalog.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "FEED_REJECT")
public class FeedReject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rejectId;
    private String feedId;
    private String supplierProductId;
    private String supplierVariantId;
    private String code;
    private String message;

    public Long getRejectId() {
        return rejectId;
    }

    public void setRejectId(Long rejectId) {
        this.rejectId = rejectId;
    }

    public String getFeedId() {
        return feedId;
    }

    public void setFeedId(String feedId) {
        this.feedId = feedId;
    }

    public String getSupplierProductId() {
        return supplierProductId;
    }

    public void setSupplierProductId(String supplierProductId) {
        this.supplierProductId = supplierProductId;
    }

    public String getSupplierVariantId() {
        return supplierVariantId;
    }

    public void setSupplierVariantId(String supplierVariantId) {
        this.supplierVariantId = supplierVariantId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
