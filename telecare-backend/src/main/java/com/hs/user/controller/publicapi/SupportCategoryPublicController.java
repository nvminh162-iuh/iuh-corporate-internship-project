package com.hs.user.controller.publicapi;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hs.user.dto.base.ApiResponse;
import com.hs.user.dto.response.SupportCategoryResponse;
import com.hs.user.service.SupportRequestService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/support-categories")
public class SupportCategoryPublicController {

    SupportRequestService supportRequestService;

    @GetMapping
    public ApiResponse<List<SupportCategoryResponse>> findAllActiveSupportCategories() {
        return ApiResponse.<List<SupportCategoryResponse>>builder()
                .result(supportRequestService.findAllActiveSupportCategories())
                .build();
    }
}
