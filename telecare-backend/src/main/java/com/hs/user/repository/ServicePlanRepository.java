package com.hs.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hs.user.model.ServicePlan;

@Repository
public interface ServicePlanRepository extends JpaRepository<ServicePlan, String>, JpaSpecificationExecutor<ServicePlan> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, String id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, String id);

    boolean existsByCategoryIdAndActiveTrue(String categoryId);

    @Query("SELECT DISTINCT p FROM ServicePlan p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.features WHERE p.id = :id")
    Optional<ServicePlan> findDetailById(@Param("id") String id);
}
