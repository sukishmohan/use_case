package com.example.catalog.repository;

import com.example.catalog.domain.Variant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VariantRepository extends JpaRepository<Variant, String> {
    List<Variant> findBySupplierCode(String supplierCode);
    List<Variant> findByProductIdIn(List<Long> productIds);
    List<Variant> findBySupplierCodeAndSupplierVariantIdIn(String supplierCode, List<String> supplierVariantIds);
    List<Variant> findByBarcodeInAndStatusAndSupplierCodeNot(List<String> barcodes,
            String status, String excludedSupplierCode);
}
