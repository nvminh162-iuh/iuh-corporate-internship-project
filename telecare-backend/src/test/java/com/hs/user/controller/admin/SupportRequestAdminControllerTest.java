package com.hs.user.controller.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hs.user.advice.exception.GlobalException;
import com.hs.user.dto.request.AddSupportRequestHistoryRequest;
import com.hs.user.dto.request.AssignSupportRequestRequest;
import com.hs.user.dto.request.UpdateSupportRequestStatusRequest;
import com.hs.user.dto.response.SupportCategoryResponse;
import com.hs.user.dto.response.SupportRequestAdminDetailResponse;
import com.hs.user.dto.response.SupportRequestAdminSummaryResponse;
import com.hs.user.dto.response.SupportRequestHistoryResponse;
import com.hs.user.filter.UserContextFilter;
import com.hs.user.filter.UserContextHolder;
import com.hs.user.model.constant.SupportRequestStatus;
import com.hs.user.service.SupportRequestAdminService;

@ExtendWith(MockitoExtension.class)
class SupportRequestAdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SupportRequestAdminService supportRequestAdminService;

    @InjectMocks
    private SupportRequestAdminController supportRequestAdminController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(supportRequestAdminController)
                .addFilters(new UserContextFilter())
                .setControllerAdvice(new GlobalException())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        UserContextHolder.set(UserContextHolder.builder()
                .userId("admin-1")
                .email("admin@telecare.com")
                .build());
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("GET /admin/support-requests returns 200 OK with paginated list")
    void findAll_ReturnsOk() throws Exception {
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

        mockMvc.perform(get("/admin/support-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.result[0].ticketCode").value("SR-20260910-00001"))
                .andExpect(jsonPath("$.result.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /admin/support-requests/{id} returns 200 OK")
    void findById_ReturnsOk() throws Exception {
        SupportRequestAdminDetailResponse detail = SupportRequestAdminDetailResponse.builder()
                .id("sr-1")
                .ticketCode("SR-20260910-00001")
                .subject("Lỗi đường truyền Internet")
                .content("Mô tả chi tiết sự cố mạng bị chập chờn")
                .status(SupportRequestStatus.NEW)
                .build();

        when(supportRequestAdminService.findAdminSupportRequestById("sr-1")).thenReturn(detail);

        mockMvc.perform(get("/admin/support-requests/sr-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value("sr-1"))
                .andExpect(jsonPath("$.result.ticketCode").value("SR-20260910-00001"));
    }

    @Test
    @DisplayName("PATCH /admin/support-requests/{id}/receive returns 200 OK")
    void receiveTicket_ReturnsOk() throws Exception {
        SupportRequestAdminDetailResponse detail = SupportRequestAdminDetailResponse.builder()
                .id("sr-1")
                .status(SupportRequestStatus.RECEIVED)
                .build();

        when(supportRequestAdminService.receiveSupportRequest(eq("sr-1"), any())).thenReturn(detail);

        mockMvc.perform(patch("/admin/support-requests/sr-1/receive")
                        .header("X-User-Id", "admin-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("RECEIVED"));
    }

    @Test
    @DisplayName("PATCH /admin/support-requests/{id}/assign returns 200 OK")
    void assignTicket_ReturnsOk() throws Exception {
        AssignSupportRequestRequest request = AssignSupportRequestRequest.builder()
                .assignedTo("staff-1")
                .note("Phân công nhân viên 1")
                .build();

        SupportRequestAdminDetailResponse detail = SupportRequestAdminDetailResponse.builder()
                .id("sr-1")
                .status(SupportRequestStatus.IN_PROGRESS)
                .build();

        when(supportRequestAdminService.assignSupportRequest(eq("sr-1"), any(), any())).thenReturn(detail);

        mockMvc.perform(patch("/admin/support-requests/sr-1/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", "admin-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("PATCH /admin/support-requests/{id}/status returns 200 OK")
    void updateStatus_ReturnsOk() throws Exception {
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

        when(supportRequestAdminService.updateSupportRequestStatus(eq("sr-1"), any(), any())).thenReturn(detail);

        mockMvc.perform(patch("/admin/support-requests/sr-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", "admin-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /admin/support-requests/{id}/histories returns 201 CREATED")
    void addHistory_ReturnsCreated() throws Exception {
        AddSupportRequestHistoryRequest request = AddSupportRequestHistoryRequest.builder()
                .note("Thêm ghi chú kiểm tra")
                .build();

        SupportRequestHistoryResponse history = SupportRequestHistoryResponse.builder()
                .id("hist-1")
                .note("Thêm ghi chú kiểm tra")
                .createdAt(Instant.now())
                .build();

        when(supportRequestAdminService.addSupportRequestHistoryNote(eq("sr-1"), any(), any())).thenReturn(history);

        mockMvc.perform(post("/admin/support-requests/sr-1/histories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", "admin-1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.note").value("Thêm ghi chú kiểm tra"));
    }

    @Test
    @DisplayName("GET /admin/support-requests/{id}/histories returns 200 OK")
    void getHistories_ReturnsOk() throws Exception {
        SupportRequestHistoryResponse history = SupportRequestHistoryResponse.builder()
                .id("hist-1")
                .note("Ghi chú 1")
                .createdAt(Instant.now())
                .build();

        when(supportRequestAdminService.findSupportRequestHistories("sr-1")).thenReturn(List.of(history));

        mockMvc.perform(get("/admin/support-requests/sr-1/histories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].id").value("hist-1"));
    }
}
