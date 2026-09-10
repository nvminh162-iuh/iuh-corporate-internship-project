package com.hs.user.controller.publicapi;

import java.math.BigDecimal;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hs.user.dto.base.ApiResponse;
import com.hs.user.dto.base.PageResponse;
import com.hs.user.dto.request.PublicPlanQuery;
import com.hs.user.dto.response.PublicPlanDetailResponse;
import com.hs.user.dto.response.PublicPlanSummaryResponse;
import com.hs.user.model.constant.BillingCycle;
import com.hs.user.service.PublicPlanService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/plans")
public class ServicePlanPublicController {

    PublicPlanService publicPlanService;

    @GetMapping
    public ApiResponse<PageResponse<PublicPlanSummaryResponse>> findAllPublicPlans(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BillingCycle billingCycle,
            @RequestParam(required = false) Boolean highlighted,
            @PageableDefault(size = 10, sort = "displayOrder", direction = Sort.Direction.ASC) Pageable pageable) {

        PublicPlanQuery query = PublicPlanQuery.builder()
                .keyword(keyword)
                .categoryCode(categoryCode)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .billingCycle(billingCycle)
                .highlighted(highlighted)
                .build();

        PageResponse<PublicPlanSummaryResponse> page = new PageResponse<>(
                publicPlanService.findAllPublicPlans(query, pageable));

        return ApiResponse.<PageResponse<PublicPlanSummaryResponse>>builder()
                .result(page)
                .build();
    }

    @GetMapping("/{slug}")
    public ApiResponse<PublicPlanDetailResponse> findPublicPlanBySlug(@PathVariable String slug) {
        return ApiResponse.<PublicPlanDetailResponse>builder()
                .result(publicPlanService.findPublicPlanBySlug(slug))
                .build();
    }
}
