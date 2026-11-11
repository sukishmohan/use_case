package com.example.catalog.repository;

import com.example.catalog.domain.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, ProductAttribute.Key> {
}
