package com.hs.user.controller.admin;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hs.user.dto.base.ApiResponse;
import com.hs.user.dto.base.PageResponse;
import com.hs.user.dto.request.ServicePlanCreateRequest;
import com.hs.user.dto.request.ServicePlanStatusUpdateRequest;
import com.hs.user.dto.request.ServicePlanUpdateRequest;
import com.hs.user.dto.response.ServicePlanDetailResponse;
import com.hs.user.dto.response.ServicePlanSummaryResponse;
import com.hs.user.model.constant.PlanStatus;
import com.hs.user.service.ServicePlanService;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/plans")
// @PreAuthorize("hasAuthority('ADMIN')")
public class ServicePlanAdminController {

    ServicePlanService servicePlanService;

    @GetMapping
    // @PreAuthorize("hasAuthority('PLAN_VIEW')")
    public ApiResponse<PageResponse<ServicePlanSummaryResponse>> findAllPlans(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) PlanStatus status,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<ServicePlanSummaryResponse> page = new PageResponse<>(
                servicePlanService.findAllPlans(keyword, categoryId, status, active, pageable));
        return ApiResponse.<PageResponse<ServicePlanSummaryResponse>>builder()
                .result(page)
                .build();
    }

    @GetMapping("/{id}")
    // @PreAuthorize("hasAuthority('PLAN_VIEW')")
    public ApiResponse<ServicePlanDetailResponse> findPlanDetailById(@PathVariable String id) {
        return ApiResponse.<ServicePlanDetailResponse>builder()
                .result(servicePlanService.findPlanDetailById(id))
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    // @PreAuthorize("hasAuthority('PLAN_CREATE')")
    public ApiResponse<ServicePlanDetailResponse> createPlan(
            @RequestBody @Valid ServicePlanCreateRequest request) {
        return ApiResponse.<ServicePlanDetailResponse>builder()
                .message("Service plan created successfully")
                .result(servicePlanService.createPlan(request))
                .build();
    }

    @PutMapping("/{id}")
    // @PreAuthorize("hasAuthority('PLAN_UPDATE')")
    public ApiResponse<ServicePlanDetailResponse> updatePlan(
            @PathVariable String id,
            @RequestBody @Valid ServicePlanUpdateRequest request) {
        return ApiResponse.<ServicePlanDetailResponse>builder()
                .message("Service plan updated successfully")
                .result(servicePlanService.updatePlan(id, request))
                .build();
    }

    @PatchMapping("/{id}/status")
    // @PreAuthorize("hasAuthority('PLAN_UPDATE')")
    public ApiResponse<ServicePlanDetailResponse> updatePlanStatus(
            @PathVariable String id,
            @RequestBody @Valid ServicePlanStatusUpdateRequest request) {
        return ApiResponse.<ServicePlanDetailResponse>builder()
                .message("Service plan status updated successfully")
                .result(servicePlanService.updatePlanStatus(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    // @PreAuthorize("hasAuthority('PLAN_DELETE')")
    public ApiResponse<@NonNull Void> softDeletePlan(@PathVariable String id) {
        servicePlanService.softDeletePlan(id);
        return ApiResponse.<Void>builder()
                .message("Service plan deleted successfully")
                .build();
    }
}
