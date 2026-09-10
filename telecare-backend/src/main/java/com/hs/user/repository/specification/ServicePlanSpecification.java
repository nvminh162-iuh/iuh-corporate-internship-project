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
}
