package com.example.catalog.repository;

import com.example.catalog.domain.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findBySupplierCode(String supplierCode);
    List<Product> findBySupplierCodeAndStatus(String supplierCode, String status);
    List<Product> findBySupplierCodeAndSupplierProductIdIn(String supplierCode, List<String> supplierProductIds);
}
