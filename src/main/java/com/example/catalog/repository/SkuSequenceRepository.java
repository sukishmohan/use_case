package com.example.catalog.repository;

import com.example.catalog.domain.SkuSequence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkuSequenceRepository extends JpaRepository<SkuSequence, String> {
}
