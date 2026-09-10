package com.hs.user.service;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.hs.user.dto.request.ServiceCategoryRequest;
import com.hs.user.dto.response.ServiceCategoryResponse;

public interface ServiceCategoryService {

    Page<@NonNull ServiceCategoryResponse> findAllCategories(Pageable pageable);

    List<ServiceCategoryResponse> findAllActiveCategoriesForDropdown();

    ServiceCategoryResponse createCategory(ServiceCategoryRequest request);

    ServiceCategoryResponse updateCategory(String id, ServiceCategoryRequest request);

    void enableCategory(String id);

    void disableCategory(String id);
}
