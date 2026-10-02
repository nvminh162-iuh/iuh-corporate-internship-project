package com.hs.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hs.user.model.User;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);

    boolean existsByRole_Name(String roleName);

    boolean existsByUsernameAndIdNot(String username, String id);

    boolean existsByEmailAndIdNot(String email, String id);

    boolean existsByPhoneAndIdNot(String phone, String id);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role r LEFT JOIN FETCH r.permissions p WHERE u.id = :id")
    Optional<User> findByIdWithRoleAndPermissions(@Param("id") String id);

    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN u.role r " +
           "LEFT JOIN r.permissions p " +
           "WHERE u.active = true " +
           "AND r.active = true " +
           "AND (UPPER(r.name) = 'ADMIN' OR UPPER(r.name) = 'ROLE_ADMIN' OR (p.name = 'SUPPORT_REQUEST_PROCESS' AND p.active = true)) " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "     LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY u.firstName ASC, u.lastName ASC, u.username ASC")
    List<User> findEligibleSupportAssignees(@Param("keyword") String keyword);
}
