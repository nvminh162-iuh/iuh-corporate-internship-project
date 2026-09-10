package com.hs.user.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hs.user.model.SupportRequestHistory;

@Repository
public interface SupportRequestHistoryRepository extends JpaRepository<SupportRequestHistory, String> {

    List<SupportRequestHistory> findBySupportRequestIdOrderByCreatedAtDesc(String supportRequestId);
}
