package com.hs.user.dto.response;

import java.time.Instant;

import com.hs.user.model.constant.SupportRequestStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerSupportRequestSummaryResponse {

    String id;

    String ticketCode;

    String categoryCode;

    String categoryName;

    String subject;

    SupportRequestStatus status;

    String servicePlanName;

    Instant createdAt;

    Instant completedAt;

    Instant closedAt;
}
