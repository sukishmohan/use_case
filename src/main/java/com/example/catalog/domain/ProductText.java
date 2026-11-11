package com.example.catalog.domain;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.*;

@Entity
@Table(name = "PRODUCT_TEXT")
public class ProductText {

    @EmbeddedId
    private Key id;
    private String name;
    private String description;
    /** 'Y' when the text was copied from English. */
    private String isFallback;

    @Embeddable
    public static class Key implements Serializable {
        private Long productId;
        private String locale;

        public Key() {
        }

        public Key(Long productId, String locale) {
            this.productId = productId;
            this.locale = locale;
        }

        public Long getProductId() {
            return productId;
        }

        public String getLocale() {
            return locale;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key other)) {
                return false;
            }
            return Objects.equals(productId, other.productId) && Objects.equals(locale, other.locale);
        }

        @Override
        public int hashCode() {
            return Objects.hash(productId, locale);
        }
    }

    public Key getId() {
        return id;
    }

    public void setId(Key id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIsFallback() {
        return isFallback;
    }

    public void setIsFallback(String isFallback) {
        this.isFallback = isFallback;
    }
}
