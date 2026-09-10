package com.hs.user.controller.admin;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hs.user.dto.base.ApiResponse;
import com.hs.user.dto.base.PageResponse;
import com.hs.user.dto.request.ServiceCategoryRequest;
import com.hs.user.dto.response.ServiceCategoryResponse;
import com.hs.user.service.ServiceCategoryService;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/service-categories")
// @PreAuthorize("hasAuthority('ADMIN')")
public class ServiceCategoryAdminController {

    ServiceCategoryService serviceCategoryService;

    @GetMapping
    // @PreAuthorize("hasAuthority('CATEGORY_VIEW')")
    public ApiResponse<PageResponse<ServiceCategoryResponse>> findAllCategories(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<ServiceCategoryResponse> page = new PageResponse<>(serviceCategoryService.findAllCategories(pageable));
        return ApiResponse.<PageResponse<ServiceCategoryResponse>>builder()
                .result(page)
                .build();
    }

    @GetMapping("/all")
    // @PreAuthorize("hasAuthority('CATEGORY_VIEW')")
    public ApiResponse<List<ServiceCategoryResponse>> findAllActiveCategoriesForDropdown() {
        return ApiResponse.<List<ServiceCategoryResponse>>builder()
                .result(serviceCategoryService.findAllActiveCategoriesForDropdown())
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    // @PreAuthorize("hasAuthority('CATEGORY_CREATE')")
    public ApiResponse<ServiceCategoryResponse> createCategory(
            @RequestBody @Valid ServiceCategoryRequest request) {
        return ApiResponse.<ServiceCategoryResponse>builder()
                .message("Service category created successfully")
                .result(serviceCategoryService.createCategory(request))
                .build();
    }

    @PutMapping("/{id}")
    // @PreAuthorize("hasAuthority('CATEGORY_UPDATE')")
    public ApiResponse<ServiceCategoryResponse> updateCategory(
            @PathVariable String id,
            @RequestBody @Valid ServiceCategoryRequest request) {
        return ApiResponse.<ServiceCategoryResponse>builder()
                .message("Service category updated successfully")
                .result(serviceCategoryService.updateCategory(id, request))
                .build();
    }

    @PatchMapping("/{id}/enable")
    // @PreAuthorize("hasAuthority('CATEGORY_UPDATE')")
    public ApiResponse<@NonNull Void> enableCategory(@PathVariable String id) {
        serviceCategoryService.enableCategory(id);
        return ApiResponse.<Void>builder()
                .message("Service category enabled successfully")
                .build();
    }

    @PatchMapping("/{id}/disable")
    // @PreAuthorize("hasAuthority('CATEGORY_DELETE')")
    public ApiResponse<@NonNull Void> disableCategory(@PathVariable String id) {
        serviceCategoryService.disableCategory(id);
        return ApiResponse.<Void>builder()
                .message("Service category disabled successfully")
                .build();
    }
}
