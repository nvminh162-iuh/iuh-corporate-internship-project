package com.hs.user.repository.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.hs.user.model.User;

import jakarta.persistence.criteria.Predicate;

public class UserSpecification {

    private UserSpecification() {
    }

    public static Specification<User> filterUsers(String keyword, Boolean active, String roleId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate usernameMatch = cb.like(cb.lower(root.get("username")), pattern);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), pattern);
                Predicate firstNameMatch = cb.like(cb.lower(root.get("firstName")), pattern);
                Predicate lastNameMatch = cb.like(cb.lower(root.get("lastName")), pattern);
                Predicate phoneMatch = cb.like(cb.lower(root.get("phone")), pattern);
                predicates.add(cb.or(usernameMatch, emailMatch, firstNameMatch, lastNameMatch, phoneMatch));
            }

            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            if (roleId != null && !roleId.isBlank()) {
                predicates.add(cb.equal(root.get("role").get("id"), roleId.trim()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
