package com.hs.user.dto.response;

import lombok.Builder;

@Builder
public record PlanFeatureResponse(
        String id,
        String code,
        String name,
        String value,
        String unit,
        Integer displayOrder
) {
}
