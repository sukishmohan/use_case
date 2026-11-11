package com.example.catalog.repository;

import com.example.catalog.domain.PriceHistory;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    List<PriceHistory> findBySkuInAndIsCurrent(List<String> skus, String isCurrent);
    Optional<PriceHistory> findFirstBySkuAndIsCurrent(String sku, String isCurrent);
    List<PriceHistory> findBySkuOrderByValidFromDesc(String sku);
    @Query("select p from PriceHistory p where p.sku = :sku and p.validFrom <= :at "
            + "and (p.validTo is null or p.validTo > :at) order by p.validFrom desc")
    List<PriceHistory> findValidAt(@Param("sku") String sku, @Param("at") Instant at);
}
