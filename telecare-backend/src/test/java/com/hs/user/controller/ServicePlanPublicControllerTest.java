package com.hs.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import com.hs.user.advice.exception.GlobalException;
import com.hs.user.controller.publicapi.ServiceCategoryPublicController;
import com.hs.user.controller.publicapi.ServicePlanPublicController;
import com.hs.user.dto.response.PublicCategoryResponse;
import com.hs.user.dto.response.PublicPlanDetailResponse;
import com.hs.user.dto.response.PublicPlanFeatureResponse;
import com.hs.user.dto.response.PublicPlanSummaryResponse;
import com.hs.user.model.constant.BillingCycle;
import com.hs.user.service.PublicPlanService;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

@ExtendWith(MockitoExtension.class)
class ServicePlanPublicControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PublicPlanService publicPlanService;

    @InjectMocks
    private ServicePlanPublicController servicePlanPublicController;

    @InjectMocks
    private ServiceCategoryPublicController serviceCategoryPublicController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(servicePlanPublicController, serviceCategoryPublicController)
                .setControllerAdvice(new GlobalException())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/plans returns public plans page")
    void findAllPublicPlans_ReturnsOk() throws Exception {
        PublicCategoryResponse category = PublicCategoryResponse.builder()
                .id("cat-1")
                .code("DATA")
                .name("Data Internet")
                .build();

        PublicPlanSummaryResponse plan = PublicPlanSummaryResponse.builder()
                .id("plan-1")
                .code("MAX100")
                .slug("max100")
                .name("MAX100 Package")
                .price(new BigDecimal("100000"))
                .currency("VND")
                .billingCycle(BillingCycle.MONTH)
                .category(category)
                .build();

        when(publicPlanService.findAllPublicPlans(any(), any()))
                .thenReturn(new PageImpl<>(List.of(plan)));

        mockMvc.perform(get("/plans")
                        .param("categoryCode", "DATA")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.result[0].code").value("MAX100"))
                .andExpect(jsonPath("$.result.result[0].slug").value("max100"));
    }

    @Test
    @DisplayName("GET /plans/{slug} returns public plan detail")
    void findPublicPlanBySlug_ReturnsOk() throws Exception {
        PublicPlanFeatureResponse feature = PublicPlanFeatureResponse.builder()
                .code("DATA_CAP")
                .name("Data Capacity")
                .value("100")
                .unit("GB")
                .build();

        PublicPlanDetailResponse plan = PublicPlanDetailResponse.builder()
                .id("plan-1")
                .code("MAX100")
                .slug("max100")
                .name("MAX100 Package")
                .price(new BigDecimal("100000"))
                .currency("VND")
                .billingCycle(BillingCycle.MONTH)
                .features(List.of(feature))
                .build();

        when(publicPlanService.findPublicPlanBySlug("max100")).thenReturn(plan);

        mockMvc.perform(get("/plans/max100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.slug").value("max100"))
                .andExpect(jsonPath("$.result.features[0].code").value("DATA_CAP"));
    }

    @Test
    @DisplayName("GET /service-categories returns active public categories")
    void findAllPublicCategories_ReturnsOk() throws Exception {
        PublicCategoryResponse category = PublicCategoryResponse.builder()
                .id("cat-1")
                .code("DATA")
                .name("Data Internet")
                .build();

        when(publicPlanService.findAllPublicCategories()).thenReturn(List.of(category));

        mockMvc.perform(get("/service-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].code").value("DATA"));
    }
}
