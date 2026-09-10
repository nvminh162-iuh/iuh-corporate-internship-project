package com.hs.user.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.hs.user.model.constant.BillingCycle;
import com.hs.user.model.constant.PlanStatus;

import lombok.Builder;

@Builder
public record ServicePlanSummaryResponse(
        String id,
        String code,
        String slug,
        String name,
        String summary,
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
        Instant updatedAt
) {
}
