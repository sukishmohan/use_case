package com.example.catalog.domain;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "SUPPLIER_FEED_STATE")
@Getter
@Setter
@NoArgsConstructor
public class SupplierFeedState {

    @Id
    private String supplierCode;
    private long lastSequenceNo;
    private String lastFeedId;
    private Instant updatedAt;

    @Version
    private int version;
}
