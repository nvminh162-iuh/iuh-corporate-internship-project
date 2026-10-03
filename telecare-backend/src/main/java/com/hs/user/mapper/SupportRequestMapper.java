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

    public static com.hs.user.dto.response.SupportRequestAdminAssigneeResponse mapToAdminAssigneeResponse(com.hs.user.model.User user) {
        if (user == null) {
            return null;
        }
        String firstName = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String lastName = user.getLastName() != null ? user.getLastName().trim() : "";
        String fullName = (firstName + " " + lastName).trim();
        if (fullName.isBlank()) {
            fullName = user.getUsername();
        }

        return com.hs.user.dto.response.SupportRequestAdminAssigneeResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(fullName)
                .email(user.getEmail())
                .roleName(user.getRole() != null ? user.getRole().getName() : null)
                .build();
    }

    public static com.hs.user.dto.response.CustomerSupportRequestSummaryResponse mapToCustomerSummaryResponse(SupportRequest request) {
        if (request == null) {
            return null;
        }
        return com.hs.user.dto.response.CustomerSupportRequestSummaryResponse.builder()
                .id(request.getId())
                .ticketCode(request.getTicketCode())
                .categoryCode(request.getCategory() != null ? request.getCategory().getCode() : null)
                .categoryName(request.getCategory() != null ? request.getCategory().getName() : null)
                .subject(request.getSubject())
                .status(request.getStatus())
                .servicePlanName(request.getServicePlan() != null ? request.getServicePlan().getName() : null)
                .createdAt(request.getCreatedAt())
                .completedAt(request.getCompletedAt())
                .closedAt(request.getClosedAt())
                .build();
    }

    public static com.hs.user.dto.response.CustomerSupportRequestDetailResponse mapToCustomerDetailResponse(SupportRequest request) {
        if (request == null) {
            return null;
        }
        return com.hs.user.dto.response.CustomerSupportRequestDetailResponse.builder()
                .id(request.getId())
                .ticketCode(request.getTicketCode())
                .categoryCode(request.getCategory() != null ? request.getCategory().getCode() : null)
                .categoryName(request.getCategory() != null ? request.getCategory().getName() : null)
                .servicePlanCode(request.getServicePlan() != null ? request.getServicePlan().getCode() : null)
                .servicePlanName(request.getServicePlan() != null ? request.getServicePlan().getName() : null)
                .subject(request.getSubject())
                .content(request.getContent())
                .contactPhone(request.getContactPhone())
                .contactEmail(request.getContactEmail())
                .status(request.getStatus())
                .resolution(request.getResolution())
                .receivedAt(request.getReceivedAt())
                .completedAt(request.getCompletedAt())
                .closedAt(request.getClosedAt())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }

    public static com.hs.user.dto.response.CustomerSupportRequestHistoryResponse mapToCustomerHistoryResponse(SupportRequestHistory history) {
        if (history == null) {
            return null;
        }
        String safeMessage;
        if (history.getAction() == com.hs.user.model.constant.SupportRequestHistoryAction.CREATED) {
            safeMessage = "Yêu cầu hỗ trợ đã được tạo thành công";
        } else if (history.getToStatus() == com.hs.user.model.constant.SupportRequestStatus.WAITING_CUSTOMER) {
            safeMessage = history.getNote() != null ? history.getNote() : "Yêu cầu bổ sung thông tin từ khách hàng";
        } else if (history.getToStatus() == com.hs.user.model.constant.SupportRequestStatus.COMPLETED) {
            safeMessage = (history.getSupportRequest() != null && history.getSupportRequest().getResolution() != null)
                    ? history.getSupportRequest().getResolution()
                    : (history.getNote() != null ? history.getNote() : "Yêu cầu hỗ trợ đã xử lý hoàn tất");
        } else if (history.getToStatus() == com.hs.user.model.constant.SupportRequestStatus.RECEIVED) {
            safeMessage = "Yêu cầu đã được tiếp nhận";
        } else if (history.getToStatus() == com.hs.user.model.constant.SupportRequestStatus.IN_PROGRESS) {
            safeMessage = "Yêu cầu đang được nhân viên kỹ thuật xử lý";
        } else if (history.getToStatus() == com.hs.user.model.constant.SupportRequestStatus.CLOSED) {
            safeMessage = "Yêu cầu hỗ trợ đã được đóng";
        } else {
            safeMessage = history.getNote() != null ? history.getNote() : "Cập nhật trạng thái yêu cầu hỗ trợ";
        }

        return com.hs.user.dto.response.CustomerSupportRequestHistoryResponse.builder()
                .id(history.getId())
                .action(history.getAction())
                .fromStatus(history.getFromStatus())
                .toStatus(history.getToStatus())
                .message(safeMessage)
                .createdAt(history.getCreatedAt())
                .build();
    }
}
