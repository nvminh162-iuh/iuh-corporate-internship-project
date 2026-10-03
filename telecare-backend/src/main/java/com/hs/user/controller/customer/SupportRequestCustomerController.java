package com.hs.user.controller.customer;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hs.user.advice.base.AppException;
import com.hs.user.constant.base.ErrorCode;
import com.hs.user.dto.base.ApiResponse;
import com.hs.user.dto.request.CreateSupportRequestRequest;
import com.hs.user.dto.response.SupportRequestResponse;
import com.hs.user.filter.UserContextHolder;
import com.hs.user.service.SupportRequestService;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.hs.user.dto.base.PageResponse;
import com.hs.user.dto.request.CustomerSupportRequestQuery;
import com.hs.user.dto.response.CustomerSupportRequestDetailResponse;
import com.hs.user.dto.response.CustomerSupportRequestHistoryResponse;
import com.hs.user.dto.response.CustomerSupportRequestSummaryResponse;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/support-requests")
public class SupportRequestCustomerController {

    SupportRequestService supportRequestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SupportRequestResponse> createSupportRequest(
            @RequestBody @Valid CreateSupportRequestRequest request
    ) {
        String customerId = resolveCustomerId();

        SupportRequestResponse response = supportRequestService.createSupportRequest(request, customerId);

        return ApiResponse.<SupportRequestResponse>builder()
                .message("Yêu cầu hỗ trợ đã được tạo thành công")
                .result(response)
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<PageResponse<CustomerSupportRequestSummaryResponse>> findMySupportRequests(
            CustomerSupportRequestQuery query,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        String customerId = resolveCustomerId();

        PageResponse<CustomerSupportRequestSummaryResponse> response = new PageResponse<>(
                supportRequestService.findMySupportRequests(customerId, query, pageable)
        );

        return ApiResponse.<PageResponse<CustomerSupportRequestSummaryResponse>>builder()
                .result(response)
                .build();
    }

    @GetMapping("/me/{id}")
    public ApiResponse<CustomerSupportRequestDetailResponse> findMySupportRequestDetail(
            @PathVariable String id
    ) {
        String customerId = resolveCustomerId();

        CustomerSupportRequestDetailResponse response = supportRequestService.findMySupportRequestDetail(customerId, id);

        return ApiResponse.<CustomerSupportRequestDetailResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/me/{id}/histories")
    public ApiResponse<List<CustomerSupportRequestHistoryResponse>> findMySupportRequestHistories(
            @PathVariable String id
    ) {
        String customerId = resolveCustomerId();

        List<CustomerSupportRequestHistoryResponse> response = supportRequestService.findMySupportRequestHistories(customerId, id);

        return ApiResponse.<List<CustomerSupportRequestHistoryResponse>>builder()
                .result(response)
                .build();
    }

    private String resolveCustomerId() {
        if (UserContextHolder.get() != null && hasText(UserContextHolder.get().getUserId())) {
            return UserContextHolder.get().getUserId();
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth && hasText(jwtAuth.getToken().getSubject())) {
            return jwtAuth.getToken().getSubject();
        }

        if (authentication != null && authentication.isAuthenticated() && hasText(authentication.getName())) {
            return authentication.getName();
        }

        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
