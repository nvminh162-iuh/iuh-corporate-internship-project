package com.hs.user.mapper;

import java.util.Comparator;
import java.util.List;

import com.hs.user.dto.response.PublicCategoryResponse;
import com.hs.user.dto.response.PublicPlanDetailResponse;
import com.hs.user.dto.response.PublicPlanFeatureResponse;
import com.hs.user.dto.response.PublicPlanSummaryResponse;
import com.hs.user.model.PlanFeature;
import com.hs.user.model.ServiceCategory;
import com.hs.user.model.ServicePlan;

public class PublicPlanMapper {

    private PublicPlanMapper() {
    }

    public static PublicCategoryResponse mapToPublicCategoryResponse(ServiceCategory category) {
        if (category == null) {
            return null;
        }
        return PublicCategoryResponse.builder()
                .id(category.getId())
                .code(category.getCode())
                .name(category.getName())
                .description(category.getDescription())
                .displayOrder(category.getDisplayOrder())
                .build();
    }

    public static PublicPlanFeatureResponse mapToPublicPlanFeatureResponse(PlanFeature feature) {
        if (feature == null) {
            return null;
        }
        return PublicPlanFeatureResponse.builder()
                .code(feature.getCode())
                .name(feature.getName())
                .value(feature.getValue())
                .unit(feature.getUnit())
                .displayOrder(feature.getDisplayOrder())
                .build();
    }

    public static PublicPlanSummaryResponse mapToPublicPlanSummaryResponse(ServicePlan plan) {
        if (plan == null) {
            return null;
        }
        return PublicPlanSummaryResponse.builder()
                .id(plan.getId())
                .code(plan.getCode())
                .slug(plan.getSlug())
                .name(plan.getName())
                .summary(plan.getSummary())
                .price(plan.getPrice())
                .currency(plan.getCurrency())
                .billingCycle(plan.getBillingCycle())
                .highlighted(plan.getHighlighted())
                .displayOrder(plan.getDisplayOrder())
                .category(mapToPublicCategoryResponse(plan.getCategory()))
                .featureCount(plan.getFeatures() != null ? plan.getFeatures().size() : 0)
                .build();
    }

    public static PublicPlanDetailResponse mapToPublicPlanDetailResponse(ServicePlan plan) {
        if (plan == null) {
            return null;
        }

        List<PublicPlanFeatureResponse> features = plan.getFeatures() == null ? List.of() : plan.getFeatures()
                .stream()
                .sorted(Comparator.comparing(
                        PlanFeature::getDisplayOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(PublicPlanMapper::mapToPublicPlanFeatureResponse)
                .toList();

        return PublicPlanDetailResponse.builder()
                .id(plan.getId())
                .code(plan.getCode())
                .slug(plan.getSlug())
                .name(plan.getName())
                .summary(plan.getSummary())
                .description(plan.getDescription())
                .price(plan.getPrice())
                .currency(plan.getCurrency())
                .billingCycle(plan.getBillingCycle())
                .highlighted(plan.getHighlighted())
                .displayOrder(plan.getDisplayOrder())
                .category(mapToPublicCategoryResponse(plan.getCategory()))
                .features(features)
                .build();
    }
}
