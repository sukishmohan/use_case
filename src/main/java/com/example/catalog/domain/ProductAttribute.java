package com.example.catalog.domain;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "PRODUCT_ATTRIBUTE")
@Getter
@Setter
@NoArgsConstructor
public class ProductAttribute {

    @EmbeddedId
    private Key id;
    private String attrValue;

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private Long productId;
        private String attrKey;
    }
}
