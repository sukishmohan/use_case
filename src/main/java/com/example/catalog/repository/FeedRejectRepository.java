package com.example.catalog.repository;

import com.example.catalog.domain.FeedReject;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedRejectRepository extends JpaRepository<FeedReject, Long> {
    List<FeedReject> findByFeedIdOrderByRejectId(String feedId);
}
