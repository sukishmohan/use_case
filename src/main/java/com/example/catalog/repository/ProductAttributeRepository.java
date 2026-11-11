package com.example.catalog.repository;

import com.example.catalog.domain.ProductAttribute;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, ProductAttribute.Key> {
    @Modifying
    @Query("delete from ProductAttribute a where a.id.productId = :productId")
    void deleteForProduct(@Param("productId") Long productId);
}
