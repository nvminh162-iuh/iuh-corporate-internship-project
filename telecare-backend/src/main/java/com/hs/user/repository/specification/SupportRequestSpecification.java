package com.hs.user.repository.specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.hs.user.dto.request.SupportRequestAdminQuery;
import com.hs.user.model.SupportRequest;
import com.hs.user.model.constant.SupportRequestStatus;

import jakarta.persistence.criteria.Predicate;

public class SupportRequestSpecification {

    private SupportRequestSpecification() {
    }

    public static Specification<SupportRequest> filterAdminRequests(SupportRequestAdminQuery query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
                String pattern = "%" + query.getKeyword().trim().toLowerCase() + "%";
                Predicate ticketCodeMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("ticketCode")), pattern);
                Predicate subjectMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("subject")), pattern);
                Predicate phoneMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("contactPhone")), pattern);
                Predicate emailMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("contactEmail")), pattern);
                Predicate customerMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("customerId")), pattern);

                predicates.add(criteriaBuilder.or(ticketCodeMatch, subjectMatch, phoneMatch, emailMatch, customerMatch));
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
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), query.getFromDate()));
            }

            if (query.getToDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), query.getToDate()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
