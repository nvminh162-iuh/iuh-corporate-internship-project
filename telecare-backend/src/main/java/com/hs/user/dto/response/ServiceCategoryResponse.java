package com.hs.user.dto.response;

import java.time.Instant;

import lombok.Builder;

@Builder
public record ServiceCategoryResponse(
        String id,
        String code,
        String name,
        String description,
        Integer displayOrder,
        Boolean active,
        Instant createdAt,
        Instant updatedAt,
        UserAuditActorResponse createdBy,
        UserAuditActorResponse updatedBy
) {
}
