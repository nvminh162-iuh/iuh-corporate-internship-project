package com.hs.user.dto.request;

import com.hs.user.model.constant.PlanStatus;

import jakarta.validation.constraints.NotNull;

public record ServicePlanStatusUpdateRequest(
        @NotNull(message = "Trạng thái không được để trống")
        PlanStatus status
) {
}
