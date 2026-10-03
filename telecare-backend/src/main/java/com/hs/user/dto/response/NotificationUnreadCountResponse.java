package com.hs.user.dto.response;

import lombok.Builder;

@Builder
public record NotificationUnreadCountResponse(
        long unreadCount
) {}
