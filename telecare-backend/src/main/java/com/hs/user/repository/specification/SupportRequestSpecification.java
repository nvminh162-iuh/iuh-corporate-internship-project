package com.hs.user.repository.specification;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.hs.user.dto.request.SupportRequestAdminQuery;
import com.hs.user.model.SupportRequest;

import jakarta.persistence.criteria.Predicate;

public class SupportRequestSpecification {

    public static final ZoneId BUSINESS_ZONE_ID = ZoneId.of("Asia/Bangkok");

    private SupportRequestSpecification() {
    }

    public static Specification<SupportRequest> filterAdminRequests(SupportRequestAdminQuery query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query == null) {
                return criteriaBuilder.conjunction();
            }

            if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
                String pattern = "%" + query.getKeyword().trim().toLowerCase() + "%";
                Predicate ticketCodeMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("ticketCode")), pattern);
                Predicate subjectMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("subject")), pattern);
                Predicate contentMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("content")), pattern);
                Predicate phoneMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("contactPhone")), pattern);
                Predicate emailMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("contactEmail")), pattern);
                Predicate customerMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("customerId")), pattern);

                predicates.add(criteriaBuilder.or(ticketCodeMatch, subjectMatch, contentMatch, phoneMatch, emailMatch, customerMatch));
            }

            if (query.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), query.getStatus()));
            }

            if (query.getCategoryCode() != null && !query.getCategoryCode().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("code"), query.getCategoryCode().trim()));
            }

            if (query.getAssignedTo() != null && !query.getAssignedTo().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("assignedTo").get("id"), query.getAssignedTo().trim()));
            }

            if (query.getFromDate() != null) {
                Instant fromInstant = query.getFromDate().atStartOfDay(BUSINESS_ZONE_ID).toInstant();
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), fromInstant));
            }

            if (query.getToDate() != null) {
                Instant toInstant = query.getToDate().plusDays(1).atStartOfDay(BUSINESS_ZONE_ID).toInstant();
                predicates.add(criteriaBuilder.lessThan(root.get("createdAt"), toInstant));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<SupportRequest> filterCustomerRequests(String customerId, com.hs.user.dto.request.CustomerSupportRequestQuery query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always enforce customerId ownership
            predicates.add(criteriaBuilder.equal(root.get("customerId"), customerId));

            if (query != null) {
                if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
                    String pattern = "%" + query.getKeyword().trim().toLowerCase() + "%";
                    Predicate ticketCodeMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("ticketCode")), pattern);
                    Predicate subjectMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("subject")), pattern);
                    Predicate contentMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("content")), pattern);
                    predicates.add(criteriaBuilder.or(ticketCodeMatch, subjectMatch, contentMatch));
                }

                if (query.getStatus() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("status"), query.getStatus()));
                }

                if (query.getCategoryCode() != null && !query.getCategoryCode().isBlank()) {
                    predicates.add(criteriaBuilder.equal(root.get("category").get("code"), query.getCategoryCode().trim()));
                }

                if (query.getFromDate() != null) {
                    Instant fromInstant = query.getFromDate().atStartOfDay(BUSINESS_ZONE_ID).toInstant();
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), fromInstant));
                }

                if (query.getToDate() != null) {
                    Instant toInstant = query.getToDate().plusDays(1).atStartOfDay(BUSINESS_ZONE_ID).toInstant();
                    predicates.add(criteriaBuilder.lessThan(root.get("createdAt"), toInstant));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
