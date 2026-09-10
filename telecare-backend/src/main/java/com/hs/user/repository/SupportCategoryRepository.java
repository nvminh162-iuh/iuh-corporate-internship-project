package com.hs.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hs.user.model.SupportCategory;

@Repository
public interface SupportCategoryRepository extends JpaRepository<SupportCategory, String> {

    Optional<SupportCategory> findByCodeAndActiveTrue(String code);

    List<SupportCategory> findAllByActiveTrueOrderByDisplayOrderAsc();

    boolean existsByCode(String code);
}
