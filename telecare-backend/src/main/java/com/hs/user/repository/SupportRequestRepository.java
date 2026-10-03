package com.hs.user.repository;

import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.hs.user.model.SupportRequest;

@Repository
public interface SupportRequestRepository extends JpaRepository<SupportRequest, String>, JpaSpecificationExecutor<SupportRequest> {

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"category", "servicePlan", "assignedTo"})
    Page<SupportRequest> findAll(Specification<SupportRequest> spec, @NonNull Pageable pageable);

    @EntityGraph(attributePaths = {"category", "servicePlan", "assignedTo"})
    Optional<SupportRequest> findByTicketCode(String ticketCode);

    @EntityGraph(attributePaths = {"category", "servicePlan", "assignedTo"})
    Optional<SupportRequest> findByIdAndCustomerId(String id, String customerId);

    boolean existsByTicketCode(String ticketCode);

    @Query(value = "SELECT nextval('support_request_ticket_seq')", nativeQuery = true)
    Long getNextTicketSequence();
}

