package com.hs.user.dto.response;

import java.time.Instant;

import com.hs.user.model.constant.NotificationType;

import lombok.Builder;

@Builder
public record NotificationResponse(
        String id,
        NotificationType type,
        String title,
        String message,
        String referenceType,
        String referenceId,
        Boolean read,
        Instant readAt,
        Instant createdAt
) {}
