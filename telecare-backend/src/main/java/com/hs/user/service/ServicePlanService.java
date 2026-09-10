package com.hs.user.service;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.hs.user.dto.request.ServicePlanCreateRequest;
import com.hs.user.dto.request.ServicePlanStatusUpdateRequest;
import com.hs.user.dto.request.ServicePlanUpdateRequest;
import com.hs.user.dto.response.ServicePlanDetailResponse;
import com.hs.user.dto.response.ServicePlanSummaryResponse;
import com.hs.user.model.constant.PlanStatus;

public interface ServicePlanService {

    Page<@NonNull ServicePlanSummaryResponse> findAllPlans(
            String keyword,
            String categoryId,
            PlanStatus status,
            Boolean active,
            Pageable pageable);

    ServicePlanDetailResponse findPlanDetailById(String id);

    ServicePlanDetailResponse createPlan(ServicePlanCreateRequest request);

    ServicePlanDetailResponse updatePlan(String id, ServicePlanUpdateRequest request);

    ServicePlanDetailResponse updatePlanStatus(String id, ServicePlanStatusUpdateRequest request);

    void softDeletePlan(String id);
}
