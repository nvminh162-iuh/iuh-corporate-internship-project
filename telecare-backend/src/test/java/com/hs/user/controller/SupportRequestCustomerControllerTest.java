package com.hs.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hs.user.advice.base.AppException;
import com.hs.user.advice.exception.GlobalException;
import com.hs.user.constant.base.ErrorCode;
import com.hs.user.controller.customer.SupportRequestCustomerController;
import com.hs.user.controller.publicapi.SupportCategoryPublicController;
import com.hs.user.dto.request.CreateSupportRequestRequest;
import com.hs.user.dto.response.SupportCategoryResponse;
import com.hs.user.dto.response.SupportRequestResponse;
import com.hs.user.model.constant.SupportRequestStatus;
import com.hs.user.service.SupportRequestService;

import com.hs.user.filter.UserContextFilter;

@ExtendWith(MockitoExtension.class)
class SupportRequestCustomerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SupportRequestService supportRequestService;

    @InjectMocks
    private SupportRequestCustomerController supportRequestCustomerController;

    @InjectMocks
    private SupportCategoryPublicController supportCategoryPublicController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(supportRequestCustomerController, supportCategoryPublicController)
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
                .addFilters(new UserContextFilter())
                .setControllerAdvice(new GlobalException())
                .build();
    }

    @Test
    @DisplayName("GET /support-categories returns 200 OK with active categories list")
    void findAllActiveSupportCategories_ReturnsOk() throws Exception {
        SupportCategoryResponse category = SupportCategoryResponse.builder()
                .id("cat-1")
                .code("PACKAGE")
                .name("Gói cước")
                .description("Vấn đề gói cước")
                .displayOrder(1)
                .build();

        when(supportRequestService.findAllActiveSupportCategories())
                .thenReturn(List.of(category));

        mockMvc.perform(get("/support-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].code").value("PACKAGE"))
                .andExpect(jsonPath("$.result[0].name").value("Gói cước"));
    }

    @Test
    @DisplayName("POST /support-requests returns 201 CREATED when payload is valid")
    void createSupportRequest_ValidPayload_ReturnsCreated() throws Exception {
        CreateSupportRequestRequest request = CreateSupportRequestRequest.builder()
                .categoryCode("CONNECTION")
                .subject("Kiểm tra đường truyền Internet")
                .content("Nội dung mô tả chi tiết vấn đề mạng chập chờn từ sáng...")
                .contactPhone("0987654321")
                .contactEmail("customer@domain.com")
                .build();

        SupportRequestResponse response = SupportRequestResponse.builder()
                .id("sr-1")
                .ticketCode("SR-20260910-00001")
                .customerId("cust-token-sub")
                .subject(request.getSubject())
                .content(request.getContent())
                .status(SupportRequestStatus.NEW)
                .build();

        when(supportRequestService.createSupportRequest(any(), any()))
                .thenReturn(response);

        mockMvc.perform(post("/support-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", "cust-token-sub"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.ticketCode").value("SR-20260910-00001"))
                .andExpect(jsonPath("$.result.status").value("NEW"));
    }

    @Test
    @DisplayName("POST /support-requests returns 400 BAD_REQUEST when subject < 10 characters")
    void createSupportRequest_InvalidSubjectLength_ReturnsBadRequest() throws Exception {
        CreateSupportRequestRequest request = CreateSupportRequestRequest.builder()
                .categoryCode("CONNECTION")
                .subject("Ngắn") // Only 4 chars
                .content("Nội dung mô tả chi tiết vấn đề mạng chập chờn từ sáng...")
                .build();

        mockMvc.perform(post("/support-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /support-requests returns 400 BAD_REQUEST when content < 20 characters")
    void createSupportRequest_InvalidContentLength_ReturnsBadRequest() throws Exception {
        CreateSupportRequestRequest request = CreateSupportRequestRequest.builder()
                .categoryCode("CONNECTION")
                .subject("Tiêu đề kiểm tra mạng viễn thông")
                .content("Nội dung quá ngắn") // Only 17 chars
                .build();

        mockMvc.perform(post("/support-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /support-requests returns 404 NOT_FOUND when category does not exist")
    void createSupportRequest_CategoryNotFound_ReturnsNotFound() throws Exception {
        CreateSupportRequestRequest request = CreateSupportRequestRequest.builder()
                .categoryCode("INVALID_CAT")
                .subject("Tiêu đề kiểm tra mạng viễn thông")
                .content("Nội dung mô tả chi tiết vấn đề mạng chập chờn từ sáng...")
                .build();

        when(supportRequestService.createSupportRequest(any(), any()))
                .thenThrow(new AppException(ErrorCode.SUPPORT_CATEGORY_NOT_EXISTED));

        mockMvc.perform(post("/support-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", "cust-token-sub"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1601));
    }

    @Test
    @DisplayName("GET /support-requests/me returns 200 OK with customer's tickets")
    void findMySupportRequests_ReturnsOk() throws Exception {
        com.hs.user.dto.response.CustomerSupportRequestSummaryResponse summary = com.hs.user.dto.response.CustomerSupportRequestSummaryResponse.builder()
                .id("sr-1")
                .ticketCode("SR-20261003-00001")
                .subject("Cần hỗ trợ về đường truyền cáp quang")
                .categoryCode("TECH_SUPPORT")
                .categoryName("Hỗ trợ kỹ thuật")
                .status(SupportRequestStatus.NEW)
                .build();

        org.springframework.data.domain.Page<com.hs.user.dto.response.CustomerSupportRequestSummaryResponse> page =
                new org.springframework.data.domain.PageImpl<>(List.of(summary), org.springframework.data.domain.PageRequest.of(0, 10), 1);

        when(supportRequestService.findMySupportRequests(eq("cust-1"), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/support-requests/me")
                        .header("X-User-Id", "cust-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.result[0].ticketCode").value("SR-20261003-00001"))
                .andExpect(jsonPath("$.result.result[0].categoryCode").value("TECH_SUPPORT"));
    }

    @Test
    @DisplayName("GET /support-requests/me/{id} returns 200 OK when customer owns ticket")
    void findMySupportRequestDetail_Owned_ReturnsOk() throws Exception {
        com.hs.user.dto.response.CustomerSupportRequestDetailResponse detail = com.hs.user.dto.response.CustomerSupportRequestDetailResponse.builder()
                .id("sr-1")
                .ticketCode("SR-20261003-00001")
                .subject("Cần hỗ trợ về đường truyền cáp quang")
                .content("Mô tả chi tiết sự cố mạng tại nhà...")
                .status(SupportRequestStatus.IN_PROGRESS)
                .categoryCode("TECH_SUPPORT")
                .build();

        when(supportRequestService.findMySupportRequestDetail("cust-1", "sr-1"))
                .thenReturn(detail);

        mockMvc.perform(get("/support-requests/me/sr-1")
                        .header("X-User-Id", "cust-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.ticketCode").value("SR-20261003-00001"))
                .andExpect(jsonPath("$.result.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("GET /support-requests/me/{id} returns 404 NOT_FOUND for anti-enumeration when ticket belongs to another customer")
    void findMySupportRequestDetail_NonOwned_ReturnsNotFound() throws Exception {
        when(supportRequestService.findMySupportRequestDetail("cust-1", "sr-other"))
                .thenThrow(new AppException(ErrorCode.SUPPORT_REQUEST_NOT_EXISTED));

        mockMvc.perform(get("/support-requests/me/sr-other")
                        .header("X-User-Id", "cust-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1602));
    }

    @Test
    @DisplayName("GET /support-requests/me/{id}/histories returns 200 OK with customer-safe timeline")
    void findMySupportRequestHistories_ReturnsOk() throws Exception {
        com.hs.user.dto.response.CustomerSupportRequestHistoryResponse history = com.hs.user.dto.response.CustomerSupportRequestHistoryResponse.builder()
                .id("hist-1")
                .action(com.hs.user.model.constant.SupportRequestHistoryAction.STATUS_CHANGED)
                .fromStatus(null)
                .toStatus(SupportRequestStatus.NEW)
                .build();

        when(supportRequestService.findMySupportRequestHistories("cust-1", "sr-1"))
                .thenReturn(List.of(history));

        mockMvc.perform(get("/support-requests/me/sr-1/histories")
                        .header("X-User-Id", "cust-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].action").value("STATUS_CHANGED"))
                .andExpect(jsonPath("$.result[0].toStatus").value("NEW"));
    }

    @Test
    @DisplayName("GET /support-requests/me returns 401 UNAUTHENTICATED when unauthenticated")
    void findMySupportRequests_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/support-requests/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1002));
    }
}
