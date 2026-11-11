package com.example.catalog.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "SKU_SEQUENCE")
public class SkuSequence {

    @Id
    private String supplierCode;
    private long nextValue;

    public String getSupplierCode() {
        return supplierCode;
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    public long getNextValue() {
        return nextValue;
    }

    public void setNextValue(long nextValue) {
        this.nextValue = nextValue;
    }
}
