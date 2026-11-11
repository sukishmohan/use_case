package com.example.catalog.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "FEED_REJECT")
@Getter
@Setter
@NoArgsConstructor
public class FeedReject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rejectId;
    private String feedId;
    private String supplierProductId;
    private String supplierVariantId;
    private String code;
    private String message;
}
