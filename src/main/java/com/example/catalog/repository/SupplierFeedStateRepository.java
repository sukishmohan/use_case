package com.example.catalog.repository;

import com.example.catalog.domain.SupplierFeedState;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface SupplierFeedStateRepository extends JpaRepository<SupplierFeedState, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SupplierFeedState s where s.supplierCode = :supplierCode")
    Optional<SupplierFeedState> findForUpdate(@Param("supplierCode") String supplierCode);
}
