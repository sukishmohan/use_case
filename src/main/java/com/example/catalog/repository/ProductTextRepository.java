package com.example.catalog.repository;

import com.example.catalog.domain.ProductText;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductTextRepository extends JpaRepository<ProductText, ProductText.Key> {
    @Modifying
    @Query("delete from ProductText t where t.id.productId = :productId")
    void deleteForProduct(@Param("productId") Long productId);
}
