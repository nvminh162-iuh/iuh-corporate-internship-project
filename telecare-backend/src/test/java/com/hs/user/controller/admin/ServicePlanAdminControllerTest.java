package com.hs.user.controller.admin;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hs.user.advice.exception.GlobalException;
import com.hs.user.dto.request.ServicePlanCreateRequest;
import com.hs.user.dto.request.ServicePlanStatusUpdateRequest;
import com.hs.user.dto.request.ServicePlanUpdateRequest;
import com.hs.user.dto.response.ServicePlanDetailResponse;
import com.hs.user.dto.response.ServicePlanSummaryResponse;
import com.hs.user.model.constant.BillingCycle;
import com.hs.user.model.constant.PlanStatus;
import com.hs.user.service.ServicePlanService;

@ExtendWith(MockitoExtension.class)
class ServicePlanAdminControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ServicePlanService servicePlanService;

    @InjectMocks
    private ServicePlanAdminController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalException())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void findAllPlans_ShouldReturnPagedResult() throws Exception {
        ServicePlanSummaryResponse summary = ServicePlanSummaryResponse.builder()
                .id("plan-1")
                .code("HOME_NET_1")
                .slug("home-net-1")
                .name("Home Net 1")
                .price(new BigDecimal("220000.00"))
                .currency("VND")
                .billingCycle(BillingCycle.MONTH)
                .status(PlanStatus.DRAFT)
                .active(true)
                .build();

        given(servicePlanService.findAllPlans(any(), any(), any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(summary)));

        mockMvc.perform(get("/admin/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.result[0].id").value("plan-1"))
                .andExpect(jsonPath("$.result.result[0].code").value("HOME_NET_1"));
    }

    @Test
    void findPlanDetailById_ShouldReturnDetail() throws Exception {
        ServicePlanDetailResponse detail = ServicePlanDetailResponse.builder()
                .id("plan-1")
                .code("HOME_NET_1")
                .name("Home Net 1")
                .price(new BigDecimal("220000.00"))
                .status(PlanStatus.DRAFT)
                .active(true)
                .features(List.of())
                .build();

        given(servicePlanService.findPlanDetailById("plan-1")).willReturn(detail);

        mockMvc.perform(get("/admin/plans/plan-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value("plan-1"))
                .andExpect(jsonPath("$.result.code").value("HOME_NET_1"));
    }

    @Test
    void createPlan_ShouldReturn201Created() throws Exception {
        ServicePlanCreateRequest request = new ServicePlanCreateRequest(
                "HOME_NET_1", "home-net-1", "Home Net 1", "Summary", "Description",
                new BigDecimal("220000.00"), "VND", BillingCycle.MONTH, PlanStatus.DRAFT,
                "cat-1", false, 1, List.of()
        );

        ServicePlanDetailResponse response = ServicePlanDetailResponse.builder()
                .id("plan-1")
                .code("HOME_NET_1")
                .slug("home-net-1")
                .name("Home Net 1")
                .status(PlanStatus.DRAFT)
                .active(true)
                .build();

        given(servicePlanService.createPlan(any(ServicePlanCreateRequest.class))).willReturn(response);

        mockMvc.perform(post("/admin/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.id").value("plan-1"))
                .andExpect(jsonPath("$.message").value("Service plan created successfully"));
    }

    @Test
    void updatePlan_ShouldReturn200Ok() throws Exception {
        ServicePlanUpdateRequest request = new ServicePlanUpdateRequest(
                "HOME_NET_1", "home-net-1", "Home Net 1 Updated", "Summary", "Description",
                new BigDecimal("250000.00"), "VND", BillingCycle.MONTH, PlanStatus.DRAFT,
                "cat-1", false, 1, List.of()
        );

        ServicePlanDetailResponse response = ServicePlanDetailResponse.builder()
                .id("plan-1")
                .code("HOME_NET_1")
                .name("Home Net 1 Updated")
                .status(PlanStatus.DRAFT)
                .active(true)
                .build();

        given(servicePlanService.updatePlan(eq("plan-1"), any(ServicePlanUpdateRequest.class))).willReturn(response);

        mockMvc.perform(put("/admin/plans/plan-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("Home Net 1 Updated"));
    }

    @Test
    void updatePlanStatus_ShouldReturn200Ok() throws Exception {
        ServicePlanStatusUpdateRequest statusReq = new ServicePlanStatusUpdateRequest(PlanStatus.PUBLISHED);
        ServicePlanDetailResponse response = ServicePlanDetailResponse.builder()
                .id("plan-1")
                .status(PlanStatus.PUBLISHED)
                .build();

        given(servicePlanService.updatePlanStatus(eq("plan-1"), any(ServicePlanStatusUpdateRequest.class))).willReturn(response);

        mockMvc.perform(patch("/admin/plans/plan-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("PUBLISHED"));
    }

    @Test
    void softDeletePlan_ShouldReturn200Ok() throws Exception {
        doNothing().when(servicePlanService).softDeletePlan("plan-1");

        mockMvc.perform(delete("/admin/plans/plan-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Service plan deleted successfully"));
    }
}
