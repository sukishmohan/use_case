package com.example.catalog.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "CATEGORY")
@Getter
@Setter
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;
    private String name;
    private Long parentId;
    /** Root-first path joined with " > ", e.g. "Staples > Rice > Basmati". */
    private String path;
    /** Root = 1. */
    private int depth;
}
