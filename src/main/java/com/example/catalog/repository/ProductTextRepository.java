package com.example.catalog.repository;

import com.example.catalog.domain.ProductText;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductTextRepository extends JpaRepository<ProductText, ProductText.Key> {
}
