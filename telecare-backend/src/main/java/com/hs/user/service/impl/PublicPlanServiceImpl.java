package com.hs.user.service.impl;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hs.user.advice.base.AppException;
import com.hs.user.constant.base.ErrorCode;
import com.hs.user.dto.request.PublicPlanQuery;
import com.hs.user.dto.response.PublicCategoryResponse;
import com.hs.user.dto.response.PublicPlanDetailResponse;
import com.hs.user.dto.response.PublicPlanSummaryResponse;
import com.hs.user.mapper.PublicPlanMapper;
import com.hs.user.model.ServicePlan;
import com.hs.user.repository.ServiceCategoryRepository;
import com.hs.user.repository.ServicePlanRepository;
import com.hs.user.repository.specification.ServicePlanSpecification;
import com.hs.user.service.PublicPlanService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PublicPlanServiceImpl implements PublicPlanService {

    static Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt",
            "price",
            "displayOrder",
            "name",
            "code"
    );

    ServicePlanRepository servicePlanRepository;
    ServiceCategoryRepository serviceCategoryRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PublicPlanSummaryResponse> findAllPublicPlans(PublicPlanQuery query, Pageable pageable) {
        if (query != null && query.getMinPrice() != null && query.getMaxPrice() != null) {
            if (query.getMinPrice().compareTo(query.getMaxPrice()) > 0) {
                throw new AppException(ErrorCode.INVALID_PRICE_RANGE);
            }
        }

        validateSortFields(pageable.getSort());

        PublicPlanQuery safeQuery = query == null ? PublicPlanQuery.builder().build() : query;
        Specification<ServicePlan> spec = ServicePlanSpecification.filterPublicPlans(
                safeQuery.getKeyword(),
                safeQuery.getCategoryCode(),
                safeQuery.getMinPrice(),
                safeQuery.getMaxPrice(),
                safeQuery.getBillingCycle(),
                safeQuery.getHighlighted()
        );

        return servicePlanRepository.findAll(spec, pageable)
                .map(PublicPlanMapper::mapToPublicPlanSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicPlanDetailResponse findPublicPlanBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new AppException(ErrorCode.PLAN_NOT_EXISTED);
        }

        ServicePlan plan = servicePlanRepository.findPublicDetailBySlug(slug.trim())
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_EXISTED));

        return PublicPlanMapper.mapToPublicPlanDetailResponse(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicCategoryResponse> findAllPublicCategories() {
        return serviceCategoryRepository.findAllByActiveTrueOrderByDisplayOrderAscCreatedAtDesc()
                .stream()
                .map(PublicPlanMapper::mapToPublicCategoryResponse)
                .toList();
    }

    private void validateSortFields(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return;
        }

        for (Sort.Order order : sort) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new AppException(ErrorCode.INVALID_SORT_FIELD);
            }
        }
    }
}
