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
public class SupportRequestAdminSummaryResponse {

    String id;
    String ticketCode;
    String customerId;
    UserResponse customer;
    SupportCategoryResponse category;
    PublicPlanSummaryResponse servicePlan;
    String subject;
    String contactPhone;
    String contactEmail;
    SupportRequestStatus status;
    UserResponse assignedTo;
    Instant createdAt;
    Instant receivedAt;
    Instant assignedAt;
    Instant completedAt;
    Instant closedAt;
}
