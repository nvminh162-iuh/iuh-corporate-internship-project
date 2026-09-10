package com.hs.user.controller.publicapi;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hs.user.dto.base.ApiResponse;
import com.hs.user.dto.response.PublicCategoryResponse;
import com.hs.user.service.PublicPlanService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/service-categories")
public class ServiceCategoryPublicController {

    PublicPlanService publicPlanService;

    @GetMapping
    public ApiResponse<List<PublicCategoryResponse>> findAllPublicCategories() {
        return ApiResponse.<List<PublicCategoryResponse>>builder()
                .result(publicPlanService.findAllPublicCategories())
                .build();
    }
}
