package com.hs.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hs.user.model.ServiceCategory;

@Repository
public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, String> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, String id);

    List<ServiceCategory> findAllByActiveTrueOrderByDisplayOrderAscCreatedAtDesc();

    Optional<ServiceCategory> findByIdAndActiveTrue(String id);
}
