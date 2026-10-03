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
public class CustomerSupportRequestDetailResponse {

    String id;

    String ticketCode;

    String categoryCode;

    String categoryName;

    String servicePlanCode;

    String servicePlanName;

    String subject;

    String content;

    String contactPhone;

    String contactEmail;

    SupportRequestStatus status;

    String resolution;

    Instant receivedAt;

    Instant completedAt;

    Instant closedAt;

    Instant createdAt;

    Instant updatedAt;
}
