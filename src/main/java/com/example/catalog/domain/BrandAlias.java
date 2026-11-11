package com.example.catalog.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "BRAND_ALIAS")
@Getter
@Setter
@NoArgsConstructor
public class BrandAlias {

    @Id
    private String aliasKey;
    private String brandName;
}
