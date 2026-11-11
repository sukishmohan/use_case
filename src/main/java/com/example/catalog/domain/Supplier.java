package com.example.catalog.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "SUPPLIER")
public class Supplier {

    @Id
    private String supplierCode;
    private String name;
    private String skuPrefix;
    private String apiToken;
    /** 'Y' or 'N'. */
    private String active;

    public String getSupplierCode() {
        return supplierCode;
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSkuPrefix() {
        return skuPrefix;
    }

    public void setSkuPrefix(String skuPrefix) {
        this.skuPrefix = skuPrefix;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }
}
