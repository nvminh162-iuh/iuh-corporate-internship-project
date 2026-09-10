package com.hs.user.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.hs.user.model.constant.BillingCycle;
import com.hs.user.model.constant.PlanStatus;

import lombok.Builder;

@Builder
public record ServicePlanDetailResponse(
        String id,
        String code,
        String slug,
        String name,
        String summary,
        String description,
        BigDecimal price,
        String currency,
        BillingCycle billingCycle,
        PlanStatus status,
        String categoryId,
        String categoryName,
        String categoryCode,
        Boolean highlighted,
        Integer displayOrder,
        Boolean active,
        Instant createdAt,
        Instant updatedAt,
        UserAuditActorResponse createdBy,
        UserAuditActorResponse updatedBy,
        List<PlanFeatureResponse> features
) {
}
