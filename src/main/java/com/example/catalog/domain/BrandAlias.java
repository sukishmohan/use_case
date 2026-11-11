package com.example.catalog.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "BRAND_ALIAS")
public class BrandAlias {

    @Id
    private String aliasKey;
    private String brandName;

    public String getAliasKey() {
        return aliasKey;
    }

    public void setAliasKey(String aliasKey) {
        this.aliasKey = aliasKey;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }
}
