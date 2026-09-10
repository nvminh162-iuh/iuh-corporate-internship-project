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
}
