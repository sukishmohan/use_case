package com.example.catalog.domain;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "PRODUCT")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;
    private String supplierCode;
    private String supplierProductId;
    private String brand;
    private Long categoryId;
    /** ACTIVE or INACTIVE. */
    private String status;
    private String lastFeedId;
    private Instant createdAt;
    private Instant updatedAt;
}
