package com.example.catalog.domain;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "PRODUCT_TEXT")
@Getter
@Setter
@NoArgsConstructor
public class ProductText {

    @EmbeddedId
    private Key id;
    private String name;
    private String description;
    /** 'Y' when the text was copied from English. */
    private String isFallback;

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private Long productId;
        private String locale;
    }
}
