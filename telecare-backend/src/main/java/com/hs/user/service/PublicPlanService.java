package com.hs.user.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.hs.user.dto.request.PublicPlanQuery;
import com.hs.user.dto.response.PublicCategoryResponse;
import com.hs.user.dto.response.PublicPlanDetailResponse;
import com.hs.user.dto.response.PublicPlanSummaryResponse;

public interface PublicPlanService {

    Page<PublicPlanSummaryResponse> findAllPublicPlans(PublicPlanQuery query, Pageable pageable);

    PublicPlanDetailResponse findPublicPlanBySlug(String slug);

    List<PublicCategoryResponse> findAllPublicCategories();
}
