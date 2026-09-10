package com.hs.user.repository.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.hs.user.model.ServicePlan;
import com.hs.user.model.constant.PlanStatus;

import jakarta.persistence.criteria.Predicate;

public class ServicePlanSpecification {

    private ServicePlanSpecification() {
    }

    public static Specification<ServicePlan> filterPlans(
            String keyword,
            String categoryId,
            PlanStatus status,
            Boolean active) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate codeLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), pattern);
                Predicate nameLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern);
                predicates.add(criteriaBuilder.or(codeLike, nameLike));
            }

            if (categoryId != null && !categoryId.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId.trim()));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (active != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), active));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<ServicePlan> filterPublicPlans(
            String keyword,
            String categoryCode,
            java.math.BigDecimal minPrice,
            java.math.BigDecimal maxPrice,
            com.hs.user.model.constant.BillingCycle billingCycle,
            Boolean highlighted) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Strictly enforce public visibility constraints
            predicates.add(criteriaBuilder.equal(root.get("status"), PlanStatus.PUBLISHED));
            predicates.add(criteriaBuilder.equal(root.get("active"), true));
            predicates.add(criteriaBuilder.equal(root.get("category").get("active"), true));

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate codeLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), pattern);
                Predicate nameLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern);
                Predicate summaryLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("summary")), pattern);
                Predicate descLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern);
                predicates.add(criteriaBuilder.or(codeLike, nameLike, summaryLike, descLike));
            }

            if (categoryCode != null && !categoryCode.isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("category").get("code")),
                        categoryCode.trim().toLowerCase()));
            }

            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            if (billingCycle != null) {
                predicates.add(criteriaBuilder.equal(root.get("billingCycle"), billingCycle));
            }

            if (highlighted != null) {
                predicates.add(criteriaBuilder.equal(root.get("highlighted"), highlighted));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
