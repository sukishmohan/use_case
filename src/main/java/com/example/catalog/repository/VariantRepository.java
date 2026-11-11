package com.example.catalog.repository;

import com.example.catalog.domain.Variant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VariantRepository extends JpaRepository<Variant, String> {
}
