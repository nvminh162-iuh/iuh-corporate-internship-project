package com.hs.user.mapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.hs.user.dto.response.PlanFeatureResponse;
import com.hs.user.dto.response.ServicePlanDetailResponse;
import com.hs.user.dto.response.ServicePlanSummaryResponse;
import com.hs.user.model.PlanFeature;
import com.hs.user.model.ServicePlan;
import com.hs.user.model.User;

public class ServicePlanMapper {

    private ServicePlanMapper() {
    }

    public static ServicePlanSummaryResponse mapToSummaryResponse(ServicePlan plan) {
        if (plan == null) {
            return null;
        }

        return ServicePlanSummaryResponse.builder()
                .id(plan.getId())
                .code(plan.getCode())
                .slug(plan.getSlug())
                .name(plan.getName())
                .summary(plan.getSummary())
                .price(plan.getPrice())
                .currency(plan.getCurrency())
                .billingCycle(plan.getBillingCycle())
                .status(plan.getStatus())
                .categoryId(plan.getCategory() != null ? plan.getCategory().getId() : null)
                .categoryName(plan.getCategory() != null ? plan.getCategory().getName() : null)
                .categoryCode(plan.getCategory() != null ? plan.getCategory().getCode() : null)
                .highlighted(plan.getHighlighted())
                .displayOrder(plan.getDisplayOrder())
                .active(plan.getActive())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }

    public static ServicePlanDetailResponse mapToDetailResponse(ServicePlan plan, Map<String, User> actors) {
        if (plan == null) {
            return null;
        }

        List<PlanFeatureResponse> featureResponses = plan.getFeatures() == null ? Collections.emptyList() :
                plan.getFeatures().stream()
                        .map(ServicePlanMapper::mapFeatureToResponse)
                        .toList();

        return ServicePlanDetailResponse.builder()
                .id(plan.getId())
                .code(plan.getCode())
                .slug(plan.getSlug())
                .name(plan.getName())
                .summary(plan.getSummary())
                .description(plan.getDescription())
                .price(plan.getPrice())
                .currency(plan.getCurrency())
                .billingCycle(plan.getBillingCycle())
                .status(plan.getStatus())
                .categoryId(plan.getCategory() != null ? plan.getCategory().getId() : null)
                .categoryName(plan.getCategory() != null ? plan.getCategory().getName() : null)
                .categoryCode(plan.getCategory() != null ? plan.getCategory().getCode() : null)
                .highlighted(plan.getHighlighted())
                .displayOrder(plan.getDisplayOrder())
                .active(plan.getActive())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .createdBy(UserMapper.resolveActor(plan.getCreatedBy(), actors))
                .updatedBy(UserMapper.resolveActor(plan.getUpdatedBy(), actors))
                .features(featureResponses)
                .build();
    }

    public static PlanFeatureResponse mapFeatureToResponse(PlanFeature feature) {
        if (feature == null) {
            return null;
        }

        return PlanFeatureResponse.builder()
                .id(feature.getId())
                .code(feature.getCode())
                .name(feature.getName())
                .value(feature.getValue())
                .unit(feature.getUnit())
                .displayOrder(feature.getDisplayOrder())
                .build();
    }
}
