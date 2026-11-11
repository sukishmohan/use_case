package com.example.catalog.domain;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.*;

@Entity
@Table(name = "PRODUCT_ATTRIBUTE")
public class ProductAttribute {

    @EmbeddedId
    private Key id;
    private String attrValue;

    @Embeddable
    public static class Key implements Serializable {
        private Long productId;
        private String attrKey;

        public Key() {
        }

        public Key(Long productId, String attrKey) {
            this.productId = productId;
            this.attrKey = attrKey;
        }

        public Long getProductId() {
            return productId;
        }

        public String getAttrKey() {
            return attrKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key other)) {
                return false;
            }
            return Objects.equals(productId, other.productId) && Objects.equals(attrKey, other.attrKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(productId, attrKey);
        }
    }

    public Key getId() {
        return id;
    }

    public void setId(Key id) {
        this.id = id;
    }

    public String getAttrValue() {
        return attrValue;
    }

    public void setAttrValue(String attrValue) {
        this.attrValue = attrValue;
    }
}
