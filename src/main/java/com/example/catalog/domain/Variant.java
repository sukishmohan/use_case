package com.example.catalog.domain;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "VARIANT")
@Getter
@Setter
@NoArgsConstructor
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
}
