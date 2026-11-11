package com.example.catalog.domain;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "FEED_LOG")
@Getter
@Setter
@NoArgsConstructor
public class FeedLog {

    @Id
    private String feedId;
    private String supplierCode;
    private long sequenceNo;
    private String mode;
    private Instant effectiveAt;
    private int variantsReceived;
    private int insertedCount;
    private int updatedCount;
    private int unchangedCount;
    private int productsDeactivated;
    private int rejectedCount;
    private int categoriesCreated;
    /** COMPLETED, PARTIAL or FAILED. */
    private String status;
    private Instant processedAt;
}
