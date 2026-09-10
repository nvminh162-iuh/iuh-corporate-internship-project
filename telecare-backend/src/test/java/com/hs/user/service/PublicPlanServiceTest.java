package com.hs.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.hs.user.advice.base.AppException;
import com.hs.user.constant.base.ErrorCode;
import com.hs.user.dto.request.PublicPlanQuery;
import com.hs.user.dto.response.PublicCategoryResponse;
import com.hs.user.dto.response.PublicPlanDetailResponse;
import com.hs.user.dto.response.PublicPlanSummaryResponse;
import com.hs.user.model.PlanFeature;
import com.hs.user.model.ServiceCategory;
import com.hs.user.model.ServicePlan;
import com.hs.user.model.constant.BillingCycle;
import com.hs.user.model.constant.PlanStatus;
import com.hs.user.repository.ServiceCategoryRepository;
import com.hs.user.repository.ServicePlanRepository;
import com.hs.user.service.impl.PublicPlanServiceImpl;

@ExtendWith(MockitoExtension.class)
class PublicPlanServiceTest {

    @Mock
    private ServicePlanRepository servicePlanRepository;

    @Mock
    private ServiceCategoryRepository serviceCategoryRepository;

    @InjectMocks
    private PublicPlanServiceImpl publicPlanService;

    private ServiceCategory sampleCategory;
    private ServicePlan samplePlan;

    @BeforeEach
    void setUp() {
        sampleCategory = ServiceCategory.builder()
                .id("cat-1")
                .code("DATA")
                .name("Data Internet")
                .description("High speed data")
                .displayOrder(1)
                .build();
        sampleCategory.setActive(true);

        PlanFeature feature = PlanFeature.builder()
                .id("feat-1")
                .code("DATA_CAP")
                .name("Data Capacity")
                .value("100")
                .unit("GB")
                .displayOrder(1)
                .build();

        samplePlan = ServicePlan.builder()
                .id("plan-1")
                .code("MAX100")
                .slug("max100")
                .name("MAX100 Data Package")
                .summary("100GB Data")
                .description("Detailed description")
                .price(new BigDecimal("100000.00"))
                .currency("VND")
                .billingCycle(BillingCycle.MONTH)
                .status(PlanStatus.PUBLISHED)
                .category(sampleCategory)
                .highlighted(true)
                .displayOrder(1)
                .build();
        samplePlan.setActive(true);
        samplePlan.addFeature(feature);
    }

    @Test
    @DisplayName("findAllPublicPlans should return paginated public plan summaries")
    void findAllPublicPlans_Success() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("displayOrder").ascending());
        Page<ServicePlan> planPage = new PageImpl<>(List.of(samplePlan), pageable, 1);

        when(servicePlanRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(planPage);

        PublicPlanQuery query = PublicPlanQuery.builder()
                .categoryCode("DATA")
                .minPrice(new BigDecimal("50000"))
                .maxPrice(new BigDecimal("150000"))
                .build();

        Page<PublicPlanSummaryResponse> result = publicPlanService.findAllPublicPlans(query, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getCode()).isEqualTo("MAX100");
    }

    @Test
    @DisplayName("findAllPublicPlans should throw INVALID_PRICE_RANGE when minPrice > maxPrice")
    void findAllPublicPlans_InvalidPriceRange() {
        Pageable pageable = PageRequest.of(0, 10);
        PublicPlanQuery query = PublicPlanQuery.builder()
                .minPrice(new BigDecimal("200000"))
                .maxPrice(new BigDecimal("100000"))
                .build();

        assertThatThrownBy(() -> publicPlanService.findAllPublicPlans(query, pageable))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PRICE_RANGE);
    }

    @Test
    @DisplayName("findAllPublicPlans should throw INVALID_SORT_FIELD when sorting by disallowed property")
    void findAllPublicPlans_InvalidSortField() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("unallowedProperty").ascending());
        PublicPlanQuery query = PublicPlanQuery.builder().build();

        assertThatThrownBy(() -> publicPlanService.findAllPublicPlans(query, pageable))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_SORT_FIELD);
    }

    @Test
    @DisplayName("findPublicPlanBySlug should return detailed public plan")
    void findPublicPlanBySlug_Success() {
        when(servicePlanRepository.findPublicDetailBySlug("max100")).thenReturn(Optional.of(samplePlan));

        PublicPlanDetailResponse response = publicPlanService.findPublicPlanBySlug("max100");

        assertThat(response).isNotNull();
        assertThat(response.getSlug()).isEqualTo("max100");
        assertThat(response.getFeatures()).hasSize(1);
        assertThat(response.getFeatures().getFirst().getCode()).isEqualTo("DATA_CAP");
    }

    @Test
    @DisplayName("findPublicPlanBySlug should throw PLAN_NOT_EXISTED when slug is not found")
    void findPublicPlanBySlug_NotFound() {
        when(servicePlanRepository.findPublicDetailBySlug("non-existent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicPlanService.findPublicPlanBySlug("non-existent"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PLAN_NOT_EXISTED);
    }

    @Test
    @DisplayName("findAllPublicCategories should return active categories")
    void findAllPublicCategories_Success() {
        when(serviceCategoryRepository.findAllByActiveTrueOrderByDisplayOrderAscCreatedAtDesc())
                .thenReturn(List.of(sampleCategory));

        List<PublicCategoryResponse> result = publicPlanService.findAllPublicCategories();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getCode()).isEqualTo("DATA");
    }
}
