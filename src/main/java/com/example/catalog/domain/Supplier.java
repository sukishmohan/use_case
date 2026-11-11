package com.example.catalog.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "SUPPLIER")
@Getter
@Setter
@NoArgsConstructor
public class Supplier {

    @Id
    private String supplierCode;
    private String name;
    private String skuPrefix;
    private String apiToken;
    /** 'Y' or 'N'. */
    private String active;
}
