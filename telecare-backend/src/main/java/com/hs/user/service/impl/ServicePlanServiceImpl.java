package com.hs.user.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hs.user.advice.base.AppException;
import com.hs.user.constant.base.ErrorCode;
import com.hs.user.dto.request.PlanFeatureRequest;
import com.hs.user.dto.request.ServicePlanCreateRequest;
import com.hs.user.dto.request.ServicePlanStatusUpdateRequest;
import com.hs.user.dto.request.ServicePlanUpdateRequest;
import com.hs.user.dto.response.ServicePlanDetailResponse;
import com.hs.user.dto.response.ServicePlanSummaryResponse;
import com.hs.user.mapper.ServicePlanMapper;
import com.hs.user.model.PlanFeature;
import com.hs.user.model.ServiceCategory;
import com.hs.user.model.ServicePlan;
import com.hs.user.model.User;
import com.hs.user.model.constant.PlanStatus;
import com.hs.user.repository.ServiceCategoryRepository;
import com.hs.user.repository.ServicePlanRepository;
import com.hs.user.repository.UserRepository;
import com.hs.user.repository.specification.ServicePlanSpecification;
import com.hs.user.service.ServicePlanService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
public class ServicePlanServiceImpl implements ServicePlanService {

    ServicePlanRepository servicePlanRepository;
    ServiceCategoryRepository serviceCategoryRepository;
    UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<@NonNull ServicePlanSummaryResponse> findAllPlans(
            String keyword,
            String categoryId,
            PlanStatus status,
            Boolean active,
            Pageable pageable) {

        Pageable effectivePageable = pageable.getPageSize() > 100
                ? PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort())
                : pageable;

        Specification<ServicePlan> spec = ServicePlanSpecification.filterPlans(keyword, categoryId, status, active);
        Page<ServicePlan> plansPage = servicePlanRepository.findAll(spec, effectivePageable);

        return plansPage.map(ServicePlanMapper::mapToSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ServicePlanDetailResponse findPlanDetailById(String id) {
        ServicePlan plan = servicePlanRepository.findDetailById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_EXISTED));

        Map<String, User> actors = getActorMap(getAuditActorIds(plan));
        return ServicePlanMapper.mapToDetailResponse(plan, actors);
    }

    @Override
    public ServicePlanDetailResponse createPlan(ServicePlanCreateRequest request) {
        String code = request.code().trim().toUpperCase();
        String slug = request.slug().trim().toLowerCase();

        if (servicePlanRepository.existsByCode(code)) {
            throw new AppException(ErrorCode.PLAN_CODE_EXISTED);
        }
        if (servicePlanRepository.existsBySlug(slug)) {
            throw new AppException(ErrorCode.PLAN_SLUG_EXISTED);
        }

        ServiceCategory category = serviceCategoryRepository.findByIdAndActiveTrue(request.categoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXISTED));

        validateFeatureCodes(request.features());

        PlanStatus targetStatus = request.status() != null ? request.status() : PlanStatus.DRAFT;

        ServicePlan plan = ServicePlan.builder()
                .code(code)
                .slug(slug)
                .name(request.name().trim())
                .summary(request.summary() != null ? request.summary().trim() : null)
                .description(request.description() != null ? request.description().trim() : null)
                .price(request.price())
                .currency(request.currency() != null ? request.currency().trim().toUpperCase() : "VND")
                .billingCycle(request.billingCycle())
                .status(targetStatus)
                .category(category)
                .highlighted(Boolean.TRUE.equals(request.highlighted()))
                .displayOrder(request.displayOrder() != null ? request.displayOrder() : 0)
                .build();

        if (targetStatus == PlanStatus.PUBLISHED) {
            validatePublishability(plan);
        }

        List<PlanFeature> featureEntities = buildFeatureEntities(request.features());
        plan.replaceFeatures(featureEntities);

        ServicePlan savedPlan = servicePlanRepository.save(plan);
        log.info("Created service plan {} with code {}", savedPlan.getId(), savedPlan.getCode());

        Map<String, User> actors = getActorMap(getAuditActorIds(savedPlan));
        return ServicePlanMapper.mapToDetailResponse(savedPlan, actors);
    }

    @Override
    public ServicePlanDetailResponse updatePlan(String id, ServicePlanUpdateRequest request) {
        ServicePlan plan = servicePlanRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_EXISTED));

        if (Boolean.FALSE.equals(plan.getActive())) {
            throw new AppException(ErrorCode.DELETED_PLAN_CANNOT_BE_UPDATED);
        }

        String code = request.code().trim().toUpperCase();
        String slug = request.slug().trim().toLowerCase();

        if (servicePlanRepository.existsByCodeAndIdNot(code, id)) {
            throw new AppException(ErrorCode.PLAN_CODE_EXISTED);
        }
        if (servicePlanRepository.existsBySlugAndIdNot(slug, id)) {
            throw new AppException(ErrorCode.PLAN_SLUG_EXISTED);
        }

        ServiceCategory category = serviceCategoryRepository.findByIdAndActiveTrue(request.categoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXISTED));

        validateFeatureCodes(request.features());

        PlanStatus targetStatus = request.status() != null ? request.status() : plan.getStatus();

        plan.setCode(code);
        plan.setSlug(slug);
        plan.setName(request.name().trim());
        plan.setSummary(request.summary() != null ? request.summary().trim() : null);
        plan.setDescription(request.description() != null ? request.description().trim() : null);
        plan.setPrice(request.price());
        if (request.currency() != null) {
            plan.setCurrency(request.currency().trim().toUpperCase());
        }
        plan.setBillingCycle(request.billingCycle());
        plan.setCategory(category);
        if (request.highlighted() != null) {
            plan.setHighlighted(request.highlighted());
        }
        if (request.displayOrder() != null) {
            plan.setDisplayOrder(request.displayOrder());
        }

        if (targetStatus == PlanStatus.PUBLISHED) {
            validatePublishability(plan);
        }
        plan.setStatus(targetStatus);

        List<PlanFeature> featureEntities = buildFeatureEntities(request.features());
        plan.replaceFeatures(featureEntities);

        ServicePlan updatedPlan = servicePlanRepository.save(plan);
        log.info("Updated service plan {}", updatedPlan.getId());

        Map<String, User> actors = getActorMap(getAuditActorIds(updatedPlan));
        return ServicePlanMapper.mapToDetailResponse(updatedPlan, actors);
    }

    @Override
    public ServicePlanDetailResponse updatePlanStatus(String id, ServicePlanStatusUpdateRequest request) {
        ServicePlan plan = servicePlanRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_EXISTED));

        if (Boolean.FALSE.equals(plan.getActive())) {
            throw new AppException(ErrorCode.DELETED_PLAN_CANNOT_BE_UPDATED);
        }

        PlanStatus targetStatus = request.status();
        if (targetStatus == PlanStatus.PUBLISHED) {
            validatePublishability(plan);
        }

        plan.setStatus(targetStatus);
        ServicePlan updatedPlan = servicePlanRepository.save(plan);
        log.info("Updated status of service plan {} to {}", id, targetStatus);

        Map<String, User> actors = getActorMap(getAuditActorIds(updatedPlan));
        return ServicePlanMapper.mapToDetailResponse(updatedPlan, actors);
    }

    @Override
    public void softDeletePlan(String id) {
        ServicePlan plan = servicePlanRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_EXISTED));

        plan.setActive(false);
        servicePlanRepository.save(plan);
        log.info("Soft deleted service plan {}", id);
    }

    private void validateFeatureCodes(List<PlanFeatureRequest> features) {
        if (features == null || features.isEmpty()) {
            return;
        }
        Set<String> seenCodes = new HashSet<>();
        for (PlanFeatureRequest feature : features) {
            String code = feature.code().trim().toUpperCase();
            if (!seenCodes.add(code)) {
                throw new AppException(ErrorCode.DUPLICATE_PLAN_FEATURE);
            }
        }
    }

    private void validatePublishability(ServicePlan plan) {
        if (plan.getName() == null || plan.getName().isBlank()
                || plan.getCode() == null || plan.getCode().isBlank()
                || plan.getCategory() == null || !Boolean.TRUE.equals(plan.getCategory().getActive())
                || plan.getPrice() == null || plan.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(ErrorCode.INVALID_PLAN_STATUS_TRANSITION);
        }
    }

    private List<PlanFeature> buildFeatureEntities(List<PlanFeatureRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }
        List<PlanFeature> list = new ArrayList<>();
        for (PlanFeatureRequest req : requests) {
            PlanFeature feature = PlanFeature.builder()
                    .code(req.code().trim().toUpperCase())
                    .name(req.name().trim())
                    .value(req.value().trim())
                    .unit(req.unit() != null ? req.unit().trim() : null)
                    .displayOrder(req.displayOrder() != null ? req.displayOrder() : 0)
                    .build();
            list.add(feature);
        }
        return list;
    }

    private Set<String> getAuditActorIds(ServicePlan plan) {
        Set<String> ids = new HashSet<>();
        if (plan.getCreatedBy() != null) ids.add(plan.getCreatedBy());
        if (plan.getUpdatedBy() != null) ids.add(plan.getUpdatedBy());
        return ids;
    }

    private Map<String, User> getActorMap(Set<String> actorIds) {
        if (actorIds == null || actorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<String> cleanIds = actorIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());
        if (cleanIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRepository.findAllById(cleanIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }
}
