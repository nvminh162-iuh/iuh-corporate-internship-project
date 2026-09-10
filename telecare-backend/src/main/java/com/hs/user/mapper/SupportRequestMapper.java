package com.hs.user.mapper;

import com.hs.user.dto.response.SupportCategoryResponse;
import com.hs.user.dto.response.SupportRequestAdminDetailResponse;
import com.hs.user.dto.response.SupportRequestAdminSummaryResponse;
import com.hs.user.dto.response.SupportRequestHistoryResponse;
import com.hs.user.dto.response.SupportRequestResponse;
import com.hs.user.dto.response.UserResponse;
import com.hs.user.model.SupportCategory;
import com.hs.user.model.SupportRequest;
import com.hs.user.model.SupportRequestHistory;

public class SupportRequestMapper {

    private SupportRequestMapper() {
    }

    public static SupportCategoryResponse mapToSupportCategoryResponse(SupportCategory category) {
        if (category == null) {
            return null;
        }
        return SupportCategoryResponse.builder()
                .id(category.getId())
                .code(category.getCode())
                .name(category.getName())
                .description(category.getDescription())
                .displayOrder(category.getDisplayOrder())
                .build();
    }

    public static SupportRequestResponse mapToSupportRequestResponse(SupportRequest request) {
        if (request == null) {
            return null;
        }
        return SupportRequestResponse.builder()
                .id(request.getId())
                .ticketCode(request.getTicketCode())
                .customerId(request.getCustomerId())
                .category(mapToSupportCategoryResponse(request.getCategory()))
                .servicePlan(PublicPlanMapper.mapToPublicPlanSummaryResponse(request.getServicePlan()))
                .subject(request.getSubject())
                .content(request.getContent())
                .contactPhone(request.getContactPhone())
                .contactEmail(request.getContactEmail())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }

    public static SupportRequestAdminSummaryResponse mapToAdminSummaryResponse(
            SupportRequest request,
            UserResponse customer,
            UserResponse assignedTo
    ) {
        if (request == null) {
            return null;
        }
        return SupportRequestAdminSummaryResponse.builder()
                .id(request.getId())
                .ticketCode(request.getTicketCode())
                .customerId(request.getCustomerId())
                .customer(customer)
                .category(mapToSupportCategoryResponse(request.getCategory()))
                .servicePlan(PublicPlanMapper.mapToPublicPlanSummaryResponse(request.getServicePlan()))
                .subject(request.getSubject())
                .contactPhone(request.getContactPhone())
                .contactEmail(request.getContactEmail())
                .status(request.getStatus())
                .assignedTo(assignedTo)
                .createdAt(request.getCreatedAt())
                .receivedAt(request.getReceivedAt())
                .assignedAt(request.getAssignedAt())
                .completedAt(request.getCompletedAt())
                .closedAt(request.getClosedAt())
                .build();
    }

    public static SupportRequestAdminDetailResponse mapToAdminDetailResponse(
            SupportRequest request,
            UserResponse customer,
            UserResponse assignedTo
    ) {
        if (request == null) {
            return null;
        }
        return SupportRequestAdminDetailResponse.builder()
                .id(request.getId())
                .ticketCode(request.getTicketCode())
                .customerId(request.getCustomerId())
                .customer(customer)
                .category(mapToSupportCategoryResponse(request.getCategory()))
                .servicePlan(PublicPlanMapper.mapToPublicPlanSummaryResponse(request.getServicePlan()))
                .subject(request.getSubject())
                .content(request.getContent())
                .contactPhone(request.getContactPhone())
                .contactEmail(request.getContactEmail())
                .status(request.getStatus())
                .assignedTo(assignedTo)
                .resolution(request.getResolution())
                .createdAt(request.getCreatedAt())
                .receivedAt(request.getReceivedAt())
                .assignedAt(request.getAssignedAt())
                .completedAt(request.getCompletedAt())
                .closedAt(request.getClosedAt())
                .build();
    }

    public static SupportRequestHistoryResponse mapToHistoryResponse(
            SupportRequestHistory history,
            UserResponse actor
    ) {
        if (history == null) {
            return null;
        }
        return SupportRequestHistoryResponse.builder()
                .id(history.getId())
                .supportRequestId(history.getSupportRequest() != null ? history.getSupportRequest().getId() : null)
                .fromStatus(history.getFromStatus())
                .toStatus(history.getToStatus())
                .action(history.getAction())
                .note(history.getNote())
                .actorId(history.getActorId())
                .actor(actor)
                .createdAt(history.getCreatedAt())
                .build();
    }
}
