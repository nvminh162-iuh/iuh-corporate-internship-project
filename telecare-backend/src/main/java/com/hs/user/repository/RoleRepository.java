package com.hs.user.repository;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.hs.user.model.Role;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, String> {

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"permissions"})
    Page<Role> findAll(@NonNull Pageable pageable);

    Optional<Role> findByName(String name);

    Optional<Role> findByIdAndActiveTrue(String id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, String id);
}
