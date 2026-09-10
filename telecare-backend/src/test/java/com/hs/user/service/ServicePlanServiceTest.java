package com.hs.user.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hs.user.advice.base.AppException;
import com.hs.user.constant.base.ErrorCode;
import com.hs.user.dto.request.PlanFeatureRequest;
import com.hs.user.dto.request.ServicePlanCreateRequest;
import com.hs.user.dto.request.ServicePlanStatusUpdateRequest;
import com.hs.user.dto.request.ServicePlanUpdateRequest;
import com.hs.user.dto.response.ServicePlanDetailResponse;
import com.hs.user.model.PlanFeature;
import com.hs.user.model.ServiceCategory;
import com.hs.user.model.ServicePlan;
import com.hs.user.model.constant.BillingCycle;
import com.hs.user.model.constant.PlanStatus;
import com.hs.user.repository.ServiceCategoryRepository;
import com.hs.user.repository.ServicePlanRepository;
import com.hs.user.repository.UserRepository;
import com.hs.user.service.impl.ServicePlanServiceImpl;

@ExtendWith(MockitoExtension.class)
class ServicePlanServiceTest {

    @Mock
    private ServicePlanRepository servicePlanRepository;

    @Mock
    private ServiceCategoryRepository serviceCategoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ServicePlanServiceImpl servicePlanService;

    private ServiceCategory category;
    private ServicePlan plan;

    @BeforeEach
    void setUp() {
        category = ServiceCategory.builder()
                .code("INTERNET")
                .name("Internet Broadband")
                .build();
        category.setId("cat-123");
        category.setActive(true);

        plan = ServicePlan.builder()
                .code("HOME_NET_1")
                .slug("home-net-1")
                .name("Home Net 1")
                .summary("Basic home internet package")
                .description("Detailed description for Home Net 1")
                .price(new BigDecimal("220000.00"))
                .currency("VND")
                .billingCycle(BillingCycle.MONTH)
                .status(PlanStatus.DRAFT)
                .category(category)
                .highlighted(false)
                .displayOrder(1)
                .features(new ArrayList<>())
                .build();
        plan.setId("plan-123");
        plan.setActive(true);
    }

    @Test
    void createPlan_Success() {
        PlanFeatureRequest featureReq = new PlanFeatureRequest("DATA_SPEED", "Download Speed", "100 Mbps", "Mbps", 1);
        ServicePlanCreateRequest request = new ServicePlanCreateRequest(
                "home_net_1",
                "home-net-1",
                "Home Net 1",
                "Summary",
                "Description",
                new BigDecimal("220000.00"),
                "VND",
                BillingCycle.MONTH,
                PlanStatus.DRAFT,
                "cat-123",
                false,
                1,
                List.of(featureReq)
        );

        when(servicePlanRepository.existsByCode("HOME_NET_1")).thenReturn(false);
        when(servicePlanRepository.existsBySlug("home-net-1")).thenReturn(false);
        when(serviceCategoryRepository.findByIdAndActiveTrue("cat-123")).thenReturn(Optional.of(category));
        when(servicePlanRepository.save(any(ServicePlan.class))).thenReturn(plan);

        ServicePlanDetailResponse response = servicePlanService.createPlan(request);

        assertNotNull(response);
        assertEquals("HOME_NET_1", response.code());
        assertEquals("home-net-1", response.slug());
        verify(servicePlanRepository).save(any(ServicePlan.class));
    }

    @Test
    void createPlan_DuplicateCode_ThrowsException() {
        ServicePlanCreateRequest request = new ServicePlanCreateRequest(
                "HOME_NET_1", "home-net-1", "Home Net 1", null, null,
                new BigDecimal("220000.00"), "VND", BillingCycle.MONTH, PlanStatus.DRAFT,
                "cat-123", false, 1, List.of()
        );

        when(servicePlanRepository.existsByCode("HOME_NET_1")).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> servicePlanService.createPlan(request));
        assertEquals(ErrorCode.PLAN_CODE_EXISTED, exception.getErrorCode());
        verify(servicePlanRepository, never()).save(any());
    }

    @Test
    void createPlan_DuplicateSlug_ThrowsException() {
        ServicePlanCreateRequest request = new ServicePlanCreateRequest(
                "HOME_NET_1", "home-net-1", "Home Net 1", null, null,
                new BigDecimal("220000.00"), "VND", BillingCycle.MONTH, PlanStatus.DRAFT,
                "cat-123", false, 1, List.of()
        );

        when(servicePlanRepository.existsByCode("HOME_NET_1")).thenReturn(false);
        when(servicePlanRepository.existsBySlug("home-net-1")).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> servicePlanService.createPlan(request));
        assertEquals(ErrorCode.PLAN_SLUG_EXISTED, exception.getErrorCode());
    }

    @Test
    void createPlan_DuplicateFeatureCode_ThrowsException() {
        PlanFeatureRequest f1 = new PlanFeatureRequest("SPEED", "Speed 1", "100Mbps", "Mbps", 1);
        PlanFeatureRequest f2 = new PlanFeatureRequest("speed", "Speed 2", "200Mbps", "Mbps", 2);

        ServicePlanCreateRequest request = new ServicePlanCreateRequest(
                "HOME_NET_1", "home-net-1", "Home Net 1", null, null,
                new BigDecimal("220000.00"), "VND", BillingCycle.MONTH, PlanStatus.DRAFT,
                "cat-123", false, 1, List.of(f1, f2)
        );

        when(servicePlanRepository.existsByCode("HOME_NET_1")).thenReturn(false);
        when(servicePlanRepository.existsBySlug("home-net-1")).thenReturn(false);
        when(serviceCategoryRepository.findByIdAndActiveTrue("cat-123")).thenReturn(Optional.of(category));

        AppException exception = assertThrows(AppException.class, () -> servicePlanService.createPlan(request));
        assertEquals(ErrorCode.DUPLICATE_PLAN_FEATURE, exception.getErrorCode());
    }

    @Test
    void updatePlan_SoftDeletedPlan_ThrowsException() {
        plan.setActive(false);
        when(servicePlanRepository.findById("plan-123")).thenReturn(Optional.of(plan));

        ServicePlanUpdateRequest request = new ServicePlanUpdateRequest(
                "HOME_NET_1", "home-net-1", "Home Net 1 Updated", null, null,
                new BigDecimal("250000.00"), "VND", BillingCycle.MONTH, PlanStatus.DRAFT,
                "cat-123", false, 1, List.of()
        );

        AppException exception = assertThrows(AppException.class, () -> servicePlanService.updatePlan("plan-123", request));
        assertEquals(ErrorCode.DELETED_PLAN_CANNOT_BE_UPDATED, exception.getErrorCode());
    }

    @Test
    void updatePlanStatus_Success() {
        when(servicePlanRepository.findById("plan-123")).thenReturn(Optional.of(plan));
        when(servicePlanRepository.save(any(ServicePlan.class))).thenReturn(plan);

        ServicePlanStatusUpdateRequest statusReq = new ServicePlanStatusUpdateRequest(PlanStatus.PUBLISHED);
        ServicePlanDetailResponse response = servicePlanService.updatePlanStatus("plan-123", statusReq);

        assertNotNull(response);
        assertEquals(PlanStatus.PUBLISHED, plan.getStatus());
        verify(servicePlanRepository).save(plan);
    }

    @Test
    void softDeletePlan_Success() {
        when(servicePlanRepository.findById("plan-123")).thenReturn(Optional.of(plan));

        servicePlanService.softDeletePlan("plan-123");

        assertFalse(plan.getActive());
        verify(servicePlanRepository).save(plan);
    }

    @Test
    void findPlanDetailById_NotFound_ThrowsException() {
        when(servicePlanRepository.findDetailById("non-existent")).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> servicePlanService.findPlanDetailById("non-existent"));
        assertEquals(ErrorCode.PLAN_NOT_EXISTED, exception.getErrorCode());
    }
}
