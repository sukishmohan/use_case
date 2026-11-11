package com.example.catalog.repository;

import com.example.catalog.domain.FeedLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedLogRepository extends JpaRepository<FeedLog, String> {
}
