package com.example.catalog.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "SKU_SEQUENCE")
@Getter
@Setter
@NoArgsConstructor
public class SkuSequence {

    @Id
    private String supplierCode;
    private long nextValue;
}
