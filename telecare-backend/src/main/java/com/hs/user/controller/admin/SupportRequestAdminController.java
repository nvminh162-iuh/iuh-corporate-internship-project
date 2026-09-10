package com.hs.user.controller.admin;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hs.user.advice.base.AppException;
import com.hs.user.constant.base.ErrorCode;
import com.hs.user.dto.base.ApiResponse;
import com.hs.user.dto.base.PageResponse;
import com.hs.user.dto.request.AddSupportRequestHistoryRequest;
import com.hs.user.dto.request.AssignSupportRequestRequest;
import com.hs.user.dto.request.SupportRequestAdminQuery;
import com.hs.user.dto.request.UpdateSupportRequestStatusRequest;
import com.hs.user.dto.response.SupportRequestAdminDetailResponse;
import com.hs.user.dto.response.SupportRequestAdminSummaryResponse;
import com.hs.user.dto.response.SupportRequestHistoryResponse;
import com.hs.user.filter.UserContextHolder;
import com.hs.user.service.SupportRequestAdminService;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/support-requests")
public class SupportRequestAdminController {

    SupportRequestAdminService supportRequestAdminService;

    @GetMapping
    public ApiResponse<PageResponse<SupportRequestAdminSummaryResponse>> findAllAdminSupportRequests(
            @ModelAttribute SupportRequestAdminQuery query,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<SupportRequestAdminSummaryResponse> page = new PageResponse<>(
                supportRequestAdminService.findAllAdminSupportRequests(query, pageable)
        );
        return ApiResponse.<PageResponse<SupportRequestAdminSummaryResponse>>builder()
                .result(page)
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<SupportRequestAdminDetailResponse> findAdminSupportRequestById(@PathVariable String id) {
        return ApiResponse.<SupportRequestAdminDetailResponse>builder()
                .result(supportRequestAdminService.findAdminSupportRequestById(id))
                .build();
    }

    @PatchMapping("/{id}/receive")
    public ApiResponse<SupportRequestAdminDetailResponse> receiveSupportRequest(@PathVariable String id) {
        String actorId = resolveActorId();
        return ApiResponse.<SupportRequestAdminDetailResponse>builder()
                .message("Tiếp nhận yêu cầu hỗ trợ thành công")
                .result(supportRequestAdminService.receiveSupportRequest(id, actorId))
                .build();
    }

    @PatchMapping("/{id}/assign")
    public ApiResponse<SupportRequestAdminDetailResponse> assignSupportRequest(
            @PathVariable String id,
            @RequestBody @Valid AssignSupportRequestRequest request
    ) {
        String actorId = resolveActorId();
        return ApiResponse.<SupportRequestAdminDetailResponse>builder()
                .message("Phân công nhân viên xử lý thành công")
                .result(supportRequestAdminService.assignSupportRequest(id, request, actorId))
                .build();
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<SupportRequestAdminDetailResponse> updateSupportRequestStatus(
            @PathVariable String id,
            @RequestBody @Valid UpdateSupportRequestStatusRequest request
    ) {
        String actorId = resolveActorId();
        return ApiResponse.<SupportRequestAdminDetailResponse>builder()
                .message("Cập nhật trạng thái ticket thành công")
                .result(supportRequestAdminService.updateSupportRequestStatus(id, request, actorId))
                .build();
    }

    @PostMapping("/{id}/histories")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SupportRequestHistoryResponse> addSupportRequestHistoryNote(
            @PathVariable String id,
            @RequestBody @Valid AddSupportRequestHistoryRequest request
    ) {
        String actorId = resolveActorId();
        return ApiResponse.<SupportRequestHistoryResponse>builder()
                .message("Thêm ghi chú xử lý thành công")
                .result(supportRequestAdminService.addSupportRequestHistoryNote(id, request, actorId))
                .build();
    }

    @GetMapping("/{id}/histories")
    public ApiResponse<List<SupportRequestHistoryResponse>> findSupportRequestHistories(@PathVariable String id) {
        return ApiResponse.<List<SupportRequestHistoryResponse>>builder()
                .result(supportRequestAdminService.findSupportRequestHistories(id))
                .build();
    }

    private String resolveActorId() {
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
