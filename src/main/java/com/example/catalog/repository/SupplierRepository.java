package com.example.catalog.repository;

import com.example.catalog.domain.Supplier;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface SupplierRepository extends JpaRepository<Supplier, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Supplier s where s.supplierCode = :supplierCode")
    Optional<Supplier> findForUpdate(@Param("supplierCode") String supplierCode);
}
