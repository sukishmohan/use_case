package com.example.catalog.repository;

import com.example.catalog.domain.BrandAlias;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandAliasRepository extends JpaRepository<BrandAlias, String> {
}
