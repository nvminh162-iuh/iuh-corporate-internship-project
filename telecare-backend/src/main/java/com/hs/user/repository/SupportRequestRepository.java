package com.hs.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.hs.user.model.SupportRequest;

@Repository
public interface SupportRequestRepository extends JpaRepository<SupportRequest, String>, JpaSpecificationExecutor<SupportRequest> {

    Optional<SupportRequest> findByTicketCode(String ticketCode);

    boolean existsByTicketCode(String ticketCode);

    @Query(value = "SELECT nextval('support_request_ticket_seq')", nativeQuery = true)
    Long getNextTicketSequence();
}

