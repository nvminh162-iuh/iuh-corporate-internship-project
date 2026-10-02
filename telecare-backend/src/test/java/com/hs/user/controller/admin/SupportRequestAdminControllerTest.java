package com.hs.user.controller.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hs.user.advice.base.AppException;
import com.hs.user.constant.base.ErrorCode;
import com.hs.user.dto.request.AddSupportRequestHistoryRequest;
import com.hs.user.dto.request.AssignSupportRequestRequest;
import com.hs.user.dto.request.UpdateSupportRequestStatusRequest;
import com.hs.user.dto.response.SupportCategoryResponse;
import com.hs.user.dto.response.SupportRequestAdminAssigneeResponse;
import com.hs.user.dto.response.SupportRequestAdminDetailResponse;
import com.hs.user.dto.response.SupportRequestAdminSummaryResponse;
import com.hs.user.dto.response.SupportRequestHistoryResponse;
import com.hs.user.model.constant.SupportRequestStatus;
import com.hs.user.service.SupportRequestAdminService;

@SpringBootTest
@AutoConfigureMockMvc
class SupportRequestAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SupportRequestAdminService supportRequestAdminService;

    // ==========================================
    // 1. GET /admin/support-requests
    // ==========================================
    @Nested
    @DisplayName("GET /admin/support-requests")
    class FindAllTests {

        @Test
        @DisplayName("Returns 401 UNAUTHORIZED when no token is provided")
        void findAll_WithoutToken_Returns401() throws Exception {
            mockMvc.perform(get("/admin/support-requests"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));
        }

        @Test
        @DisplayName("Returns 403 FORBIDDEN when user has customer USER role")
        void findAll_CustomerRole_Returns403() throws Exception {
            mockMvc.perform(get("/admin/support-requests")
                            .with(jwt().authorities(new SimpleGrantedAuthority("USER"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()));
        }

        @Test
        @DisplayName("Returns 200 OK when user has SUPPORT_REQUEST_VIEW permission")
        void findAll_WithViewPermission_Returns200() throws Exception {
            SupportRequestAdminSummaryResponse summary = SupportRequestAdminSummaryResponse.builder()
                    .id("sr-1")
                    .ticketCode("SR-20260910-00001")
                    .subject("Lỗi đường truyền Internet")
                    .category(SupportCategoryResponse.builder().code("CONNECTION").name("Kết nối mạng").build())
                    .status(SupportRequestStatus.NEW)
                    .createdAt(Instant.now())
                    .build();

            PageImpl<SupportRequestAdminSummaryResponse> page = new PageImpl<>(List.of(summary), PageRequest.of(0, 10), 1);
            when(supportRequestAdminService.findAllAdminSupportRequests(any(), any())).thenReturn(page);

            mockMvc.perform(get("/admin/support-requests")
                            .with(jwt().jwt(builder -> builder.subject("staff-1").claim("email", "staff@telecare.com"))
                                    .authorities(new SimpleGrantedAuthority("SUPPORT_REQUEST_VIEW"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.result[0].ticketCode").value("SR-20260910-00001"))
                    .andExpect(jsonPath("$.result.totalElements").value(1));
        }

        @Test
        @DisplayName("Returns 200 OK when user has ADMIN role")
        void findAll_WithAdminRole_Returns200() throws Exception {
            when(supportRequestAdminService.findAllAdminSupportRequests(any(), any()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(get("/admin/support-requests")
                            .with(jwt().jwt(builder -> builder.subject("admin-1").claim("email", "admin@telecare.com"))
                                    .authorities(new SimpleGrantedAuthority("ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.totalElements").value(0));
        }

        @Test
        @DisplayName("F1: Binds LocalDate query parameters fromDate & toDate successfully (200 OK)")
        void findAll_WithLocalDateParams_Returns200() throws Exception {
            when(supportRequestAdminService.findAllAdminSupportRequests(any(), any()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(get("/admin/support-requests?fromDate=2026-09-01&toDate=2026-10-31")
                            .with(jwt().authorities(new SimpleGrantedAuthority("SUPPORT_REQUEST_VIEW"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("F1: Binds fromDate only successfully (200 OK)")
        void findAll_WithFromDateOnly_Returns200() throws Exception {
            when(supportRequestAdminService.findAllAdminSupportRequests(any(), any()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(get("/admin/support-requests?fromDate=2026-09-01")
                            .with(jwt().authorities(new SimpleGrantedAuthority("SUPPORT_REQUEST_VIEW"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("F1: Binds toDate only successfully (200 OK)")
        void findAll_WithToDateOnly_Returns200() throws Exception {
            when(supportRequestAdminService.findAllAdminSupportRequests(any(), any()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(get("/admin/support-requests?toDate=2026-10-31")
                            .with(jwt().authorities(new SimpleGrantedAuthority("SUPPORT_REQUEST_VIEW"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("F1: Returns 400 BAD_REQUEST when fromDate is after toDate")
        void findAll_WithReversedDates_Returns400() throws Exception {
            when(supportRequestAdminService.findAllAdminSupportRequests(any(), any()))
                    .thenThrow(new AppException(ErrorCode.INVALID_DATE_RANGE));

            mockMvc.perform(get("/admin/support-requests?fromDate=2026-10-31&toDate=2026-09-01")
                            .with(jwt().authorities(new SimpleGrantedAuthority("SUPPORT_REQUEST_VIEW"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_DATE_RANGE.getCode()));
        }

        @Test
        @DisplayName("F1: Returns 400 BAD_REQUEST when date format is invalid")
        void findAll_WithInvalidDateFormat_Returns400() throws Exception {
            mockMvc.perform(get("/admin/support-requests?fromDate=invalid-date")
                            .with(jwt().authorities(new SimpleGrantedAuthority("SUPPORT_REQUEST_VIEW"))))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==========================================
    // 2. GET /admin/support-requests/{id}
    // ==========================================
    @Nested
    @DisplayName("GET /admin/support-requests/{id}")
    class FindByIdTests {

        @Test
        @DisplayName("Returns 401 UNAUTHORIZED when no token is provided")
        void findById_WithoutToken_Returns401() throws Exception {
            mockMvc.perform(get("/admin/support-requests/sr-1"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));
        }

        @Test
        @DisplayName("Returns 403 FORBIDDEN when user has customer USER role")
        void findById_CustomerRole_Returns403() throws Exception {
            mockMvc.perform(get("/admin/support-requests/sr-1")
                            .with(jwt().authorities(new SimpleGrantedAuthority("USER"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()));
        }

        @Test
        @DisplayName("Returns 200 OK when ticket exists and user has view permission")
        void findById_WithViewPermission_Returns200() throws Exception {
            SupportRequestAdminDetailResponse detail = SupportRequestAdminDetailResponse.builder()
                    .id("sr-1")
                    .ticketCode("SR-20260910-00001")
                    .subject("Lỗi đường truyền Internet")
                    .content("Mô tả chi tiết sự cố mạng bị chập chờn")
                    .status(SupportRequestStatus.NEW)
                    .build();

            when(supportRequestAdminService.findAdminSupportRequestById("sr-1")).thenReturn(detail);

            mockMvc.perform(get("/admin/support-requests/sr-1")
                            .with(jwt().authorities(new SimpleGrantedAuthority("SUPPORT_REQUEST_VIEW"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.id").value("sr-1"))
                    .andExpect(jsonPath("$.result.ticketCode").value("SR-20260910-00001"));
        }

        @Test
        @DisplayName("Returns 404 NOT_FOUND when ticket does not exist")
        void findById_NotFound_Returns404() throws Exception {
            when(supportRequestAdminService.findAdminSupportRequestById("sr-non-existent"))
                    .thenThrow(new AppException(ErrorCode.SUPPORT_REQUEST_NOT_EXISTED));

            mockMvc.perform(get("/admin/support-requests/sr-non-existent")
                            .with(jwt().authorities(new SimpleGrantedAuthority("SUPPORT_REQUEST_VIEW"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.SUPPORT_REQUEST_NOT_EXISTED.getCode()));
        }
    }

    // ==========================================
    // 3. PATCH /admin/support-requests/{id}/receive
    // ==========================================
    @Nested
    @DisplayName("PATCH /admin/support-requests/{id}/receive")
    class ReceiveTicketTests {

        @Test
        @DisplayName("Returns 401 UNAUTHORIZED when no token is provided")
        void receiveTicket_WithoutToken_Returns401() throws Exception {
            mockMvc.perform(patch("/admin/support-requests/sr-1/receive"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));
        }

        @Test
        @DisplayName("Returns 403 FORBIDDEN when user has only SUPPORT_REQUEST_VIEW permission")
        void receiveTicket_OnlyViewPermission_Returns403() throws Exception {
            mockMvc.perform(patch("/admin/support-requests/sr-1/receive")
                            .with(jwt().authorities(new SimpleGrantedAuthority("SUPPORT_REQUEST_VIEW"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()));
        }

        @Test
        @DisplayName("Returns 403 FORBIDDEN when user has only SUPPORT_REQUEST_ASSIGN permission (cannot process)")
        void receiveTicket_OnlyAssignPermission_Returns403() throws Exception {
            mockMvc.perform(patch("/admin/support-requests/sr-1/receive")
                            .with(jwt().authorities(new SimpleGrantedAuthority("SUPPORT_REQUEST_ASSIGN"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()));
        }

        @Test
        @DisplayName("Returns 200 OK when user has SUPPORT_REQUEST_PROCESS permission")
        void receiveTicket_WithProcessPermission_Returns200() throws Exception {
            SupportRequestAdminDetailResponse detail = SupportRequestAdminDetailResponse.builder()
                    .id("sr-1")
                    .status(SupportRequestStatus.RECEIVED)
                    .build();

            when(supportRequestAdminService.receiveSupportRequest(eq("sr-1"), eq("staff-1"))).thenReturn(detail);

            mockMvc.perform(patch("/admin/support-requests/sr-1/receive")
                            .with(jwt().jwt(builder -> builder.subject("staff-1"))
                                    .authorities(new SimpleGrantedAuthority("SUPPORT_REQUEST_PROCESS"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.status").value("RECEIVED"))
                    .andExpect(jsonPath("$.message").value("Tiếp nhận yêu cầu hỗ trợ thành công"));
        }
    }

    // ==========================================
    // 4. PATCH /admin/support-requests/{id}/assign
    // ==========================================
    @Nested
    @DisplayName("PATCH /admin/support-requests/{id}/assign")
    class AssignTicketTests {

        @Test
        @DisplayName("Returns 401 UNAUTHORIZED when no token is provided")
        void assignTicket_WithoutToken_Returns401() throws Exception {
            AssignSupportRequestRequest request = AssignSupportRequestRequest.builder()
                    .assignedTo("staff-1")
                    .note("Phân công nhân viên 1")
                    .build();

            mockMvc.perform(patch("/admin/support-requests/sr-1/assign")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));
        }

        @Test
        @DisplayName("Returns 403 FORBIDDEN when user has only SUPPORT_REQUEST_PROCESS permission (cannot assign)")
        void assignTicket_MissingAssignPermission_Returns403() throws Exception {
            AssignSupportRequestRequest request = AssignSupportRequestRequest.builder()
                    .assignedTo("staff-1")
                    .build();

            mockMvc.perform(patch("/admin/support-requests/sr-1/assign")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(jwt().authorities(new SimpleGrantedAuthority("SUPPORT_REQUEST_PROCESS"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()));
        }

        @Test
        @DisplayName("Returns 400 BAD_REQUEST when assignedTo is blank")
        void assignTicket_BlankAssignedTo_Returns400() throws Exception {
            AssignSupportRequestRequest request = AssignSupportRequestRequest.builder()
                    .assignedTo("   ")
                    .build();

            mockMvc.perform(patch("/admin/support-requests/sr-1/assign")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(jwt().jwt(builder -> builder.subject("admin-1"))
                                    .authorities(new SimpleGrantedAuthority("ADMIN"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Returns 200 OK when user has SUPPORT_REQUEST_ASSIGN permission and valid payload")
        void assignTicket_WithAssignPermission_Returns200() throws Exception {
            AssignSupportRequestRequest request = AssignSupportRequestRequest.builder()
                    .assignedTo("staff-1")
                    .note("Phân công nhân viên 1")
                    .build();

            SupportRequestAdminDetailResponse detail = SupportRequestAdminDetailResponse.builder()
                    .id("sr-1")
                    .status(SupportRequestStatus.IN_PROGRESS)
                    .build();

            when(supportRequestAdminService.assignSupportRequest(eq("sr-1"), any(), eq("admin-1"))).thenReturn(detail);

            mockMvc.perform(patch("/admin/support-requests/sr-1/assign")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(jwt().jwt(builder -> builder.subject("admin-1"))
                                    .authorities(new SimpleGrantedAuthority("SUPPORT_REQUEST_ASSIGN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.status").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.message").value("Phân công nhân viên xử lý thành công"));
        }
    }

    // ==========================================
    // 5. PATCH /admin/support-requests/{id}/status
    // ==========================================
    @Nested
    @DisplayName("PATCH /admin/support-requests/{id}/status")
    class UpdateStatusTests {

        @Test
        @DisplayName("Returns 401 UNAUTHORIZED when no token is provided")
        void updateStatus_WithoutToken_Returns401() throws Exception {
            UpdateSupportRequestStatusRequest request = UpdateSupportRequestStatusRequest.builder()
                    .status(SupportRequestStatus.COMPLETED)
                    .resolution("Đã xử lý xong")
                    .build();

            mockMvc.perform(patch("/admin/support-requests/sr-1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));
        }

        @Test
        @DisplayName("Returns 403 FORBIDDEN when user has only SUPPORT_REQUEST_VIEW permission")
        void updateStatus_OnlyViewPermission_Returns403() throws Exception {
            UpdateSupportRequestStatusRequest request = UpdateSupportRequestStatusRequest.builder()
                    .status(SupportRequestStatus.COMPLETED)
                    .resolution("Đã xử lý xong")
                    .build();

            mockMvc.perform(patch("/admin/support-requests/sr-1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(jwt().authorities(new SimpleGrantedAuthority("SUPPORT_REQUEST_VIEW"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()));
        }

        @Test
        @DisplayName("Returns 400 BAD_REQUEST when status is null")
        void updateStatus_NullStatus_Returns400() throws Exception {
            UpdateSupportRequestStatusRequest request = UpdateSupportRequestStatusRequest.builder()
                    .status(null)
                    .build();

            mockMvc.perform(patch("/admin/support-requests/sr-1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(jwt().jwt(builder -> builder.subject("admin-1"))
                                    .authorities(new SimpleGrantedAuthority("ADMIN"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Returns 200 OK when user has SUPPORT_REQUEST_PROCESS permission")
        void updateStatus_WithProcessPermission_Returns200() throws Exception {
            UpdateSupportRequestStatusRequest request = UpdateSupportRequestStatusRequest.builder()
                    .status(SupportRequestStatus.COMPLETED)
                    .note("Đã xử lý xong sự cố")
                    .resolution("Thay dây mạng mới")
                    .build();

            SupportRequestAdminDetailResponse detail = SupportRequestAdminDetailResponse.builder()
                    .id("sr-1")
                    .status(SupportRequestStatus.COMPLETED)
                    .resolution("Thay dây mạng mới")
                    .build();

            when(supportRequestAdminService.updateSupportRequestStatus(eq("sr-1"), any(), eq("staff-1"))).thenReturn(detail);

            mockMvc.perform(patch("/admin/support-requests/sr-1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(jwt().jwt(builder -> builder.subject("staff-1"))
                                    .authorities(new SimpleGrantedAuthority("SUPPORT_REQUEST_PROCESS"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.message").value("Cập nhật trạng thái ticket thành công"));
        }
    }

    // ==========================================
    // 6. POST /admin/support-requests/{id}/histories
    // ==========================================
    @Nested
    @DisplayName("POST /admin/support-requests/{id}/histories")
    class AddHistoryTests {

        @Test
        @DisplayName("Returns 401 UNAUTHORIZED when no token is provided")
        void addHistory_WithoutToken_Returns401() throws Exception {
            AddSupportRequestHistoryRequest request = AddSupportRequestHistoryRequest.builder()
                    .note("Ghi chú")
                    .build();

            mockMvc.perform(post("/admin/support-requests/sr-1/histories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));
        }

        @Test
        @DisplayName("Returns 403 FORBIDDEN when user has only SUPPORT_REQUEST_VIEW permission")
        void addHistory_OnlyViewPermission_Returns403() throws Exception {
            AddSupportRequestHistoryRequest request = AddSupportRequestHistoryRequest.builder()
                    .note("Ghi chú")
                    .build();

            mockMvc.perform(post("/admin/support-requests/sr-1/histories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(jwt().authorities(new SimpleGrantedAuthority("SUPPORT_REQUEST_VIEW"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()));
        }

        @Test
        @DisplayName("Returns 400 BAD_REQUEST when note is blank")
        void addHistory_BlankNote_Returns400() throws Exception {
            AddSupportRequestHistoryRequest request = AddSupportRequestHistoryRequest.builder()
                    .note("   ")
                    .build();

            mockMvc.perform(post("/admin/support-requests/sr-1/histories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(jwt().jwt(builder -> builder.subject("admin-1"))
                                    .authorities(new SimpleGrantedAuthority("ADMIN"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Returns 201 CREATED when user has SUPPORT_REQUEST_PROCESS permission")
        void addHistory_WithProcessPermission_Returns201() throws Exception {
            AddSupportRequestHistoryRequest request = AddSupportRequestHistoryRequest.builder()
                    .note("Thêm ghi chú kiểm tra")
                    .build();

            SupportRequestHistoryResponse history = SupportRequestHistoryResponse.builder()
                    .id("hist-1")
                    .note("Thêm ghi chú kiểm tra")
                    .createdAt(Instant.now())
                    .build();

            when(supportRequestAdminService.addSupportRequestHistoryNote(eq("sr-1"), any(), eq("staff-1"))).thenReturn(history);

            mockMvc.perform(post("/admin/support-requests/sr-1/histories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(jwt().jwt(builder -> builder.subject("staff-1"))
                                    .authorities(new SimpleGrantedAuthority("SUPPORT_REQUEST_PROCESS"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.result.note").value("Thêm ghi chú kiểm tra"))
                    .andExpect(jsonPath("$.message").value("Thêm ghi chú xử lý thành công"));
        }
    }

    // ==========================================
    // 7. GET /admin/support-requests/{id}/histories
    // ==========================================
    @Nested
    @DisplayName("GET /admin/support-requests/{id}/histories")
    class GetHistoriesTests {

        @Test
        @DisplayName("Returns 401 UNAUTHORIZED when no token is provided")
        void getHistories_WithoutToken_Returns401() throws Exception {
            mockMvc.perform(get("/admin/support-requests/sr-1/histories"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));
        }

        @Test
        @DisplayName("Returns 403 FORBIDDEN when user has customer USER role")
        void getHistories_CustomerRole_Returns403() throws Exception {
            mockMvc.perform(get("/admin/support-requests/sr-1/histories")
                            .with(jwt().authorities(new SimpleGrantedAuthority("USER"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()));
        }

        @Test
        @DisplayName("Returns 200 OK when user has SUPPORT_REQUEST_VIEW permission")
        void getHistories_WithViewPermission_Returns200() throws Exception {
            SupportRequestHistoryResponse history = SupportRequestHistoryResponse.builder()
                    .id("hist-1")
                    .note("Ghi chú 1")
                    .createdAt(Instant.now())
                    .build();

            when(supportRequestAdminService.findSupportRequestHistories("sr-1")).thenReturn(List.of(history));

            mockMvc.perform(get("/admin/support-requests/sr-1/histories")
                            .with(jwt().jwt(builder -> builder.subject("staff-1"))
                                    .authorities(new SimpleGrantedAuthority("SUPPORT_REQUEST_VIEW"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result[0].id").value("hist-1"));
        }
    }

    // ==========================================
    // 8. GET /admin/support-requests/assignees (F4)
    // ==========================================
    @Nested
    @DisplayName("GET /admin/support-requests/assignees")
    class AssigneesTests {

        @Test
        @DisplayName("F4: Returns 401 UNAUTHORIZED when no token is provided")
        void assignees_WithoutToken_Returns401() throws Exception {
            mockMvc.perform(get("/admin/support-requests/assignees"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));
        }

        @Test
        @DisplayName("F4: Returns 403 FORBIDDEN when customer (USER role) requests assignees")
        void assignees_CustomerRole_Returns403() throws Exception {
            mockMvc.perform(get("/admin/support-requests/assignees")
                            .with(jwt().authorities(new SimpleGrantedAuthority("USER"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()));
        }

        @Test
        @DisplayName("F4: Returns 403 FORBIDDEN when user has only SUPPORT_REQUEST_VIEW permission")
        void assignees_ViewPermissionOnly_Returns403() throws Exception {
            mockMvc.perform(get("/admin/support-requests/assignees")
                            .with(jwt().authorities(new SimpleGrantedAuthority("SUPPORT_REQUEST_VIEW"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()));
        }

        @Test
        @DisplayName("F4: Returns 200 OK when user has SUPPORT_REQUEST_ASSIGN permission")
        void assignees_WithAssignPermission_Returns200() throws Exception {
            SupportRequestAdminAssigneeResponse assignee = SupportRequestAdminAssigneeResponse.builder()
                    .id("staff-1")
                    .username("staff1")
                    .fullName("Staff One")
                    .email("staff1@telecare.com")
                    .roleName("STAFF")
                    .build();

            when(supportRequestAdminService.findEligibleAssignees(any())).thenReturn(List.of(assignee));

            mockMvc.perform(get("/admin/support-requests/assignees")
                            .with(jwt().authorities(new SimpleGrantedAuthority("SUPPORT_REQUEST_ASSIGN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result[0].id").value("staff-1"))
                    .andExpect(jsonPath("$.result[0].fullName").value("Staff One"));
        }

        @Test
        @DisplayName("F4: Returns 200 OK when user has ADMIN role")
        void assignees_WithAdminRole_Returns200() throws Exception {
            when(supportRequestAdminService.findEligibleAssignees(any())).thenReturn(List.of());

            mockMvc.perform(get("/admin/support-requests/assignees")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").isArray());
        }
    }
}
