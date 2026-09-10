package com.hs.user.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.hs.user.advice.base.AppException;
import com.hs.user.constant.base.ErrorCode;
import com.hs.user.dto.request.ServiceCategoryRequest;
import com.hs.user.dto.response.ServiceCategoryResponse;
import com.hs.user.model.ServiceCategory;
import com.hs.user.repository.ServiceCategoryRepository;
import com.hs.user.repository.ServicePlanRepository;
import com.hs.user.repository.UserRepository;
import com.hs.user.service.impl.ServiceCategoryServiceImpl;

@ExtendWith(MockitoExtension.class)
class ServiceCategoryServiceTest {

    @Mock
    private ServiceCategoryRepository serviceCategoryRepository;

    @Mock
    private ServicePlanRepository servicePlanRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ServiceCategoryServiceImpl serviceCategoryService;

    private ServiceCategory category;

    @BeforeEach
    void setUp() {
        category = ServiceCategory.builder()
                .code("INTERNET")
                .name("Internet Broadband")
                .description("High speed fiber internet")
                .displayOrder(1)
                .build();
        category.setId("cat-123");
        category.setActive(true);
    }

    @Test
    void createCategory_Success() {
        ServiceCategoryRequest request = new ServiceCategoryRequest("internet", "Internet Broadband", "High speed", 1);
        when(serviceCategoryRepository.existsByCode("INTERNET")).thenReturn(false);
        when(serviceCategoryRepository.save(any(ServiceCategory.class))).thenReturn(category);

        ServiceCategoryResponse response = serviceCategoryService.createCategory(request);

        assertNotNull(response);
        assertEquals("INTERNET", response.code());
        assertEquals("Internet Broadband", response.name());
        verify(serviceCategoryRepository).save(any(ServiceCategory.class));
    }

    @Test
    void createCategory_DuplicateCode_ThrowsException() {
        ServiceCategoryRequest request = new ServiceCategoryRequest("INTERNET", "Internet", "Desc", 1);
        when(serviceCategoryRepository.existsByCode("INTERNET")).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> serviceCategoryService.createCategory(request));

        assertEquals(ErrorCode.CATEGORY_CODE_EXISTED, exception.getErrorCode());
        verify(serviceCategoryRepository, never()).save(any());
    }

    @Test
    void updateCategory_Success() {
        ServiceCategoryRequest request = new ServiceCategoryRequest("INTERNET_FTTH", "Internet FTTH", "Updated desc", 2);
        when(serviceCategoryRepository.findById("cat-123")).thenReturn(Optional.of(category));
        when(serviceCategoryRepository.existsByCodeAndIdNot("INTERNET_FTTH", "cat-123")).thenReturn(false);
        when(serviceCategoryRepository.save(any(ServiceCategory.class))).thenReturn(category);

        ServiceCategoryResponse response = serviceCategoryService.updateCategory("cat-123", request);

        assertNotNull(response);
        verify(serviceCategoryRepository).save(category);
    }

    @Test
    void updateCategory_NotFound_ThrowsException() {
        ServiceCategoryRequest request = new ServiceCategoryRequest("INTERNET", "Internet", "Desc", 1);
        when(serviceCategoryRepository.findById("non-existent")).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> serviceCategoryService.updateCategory("non-existent", request));

        assertEquals(ErrorCode.CATEGORY_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    void disableCategory_Success() {
        when(serviceCategoryRepository.findById("cat-123")).thenReturn(Optional.of(category));
        when(servicePlanRepository.existsByCategoryIdAndActiveTrue("cat-123")).thenReturn(false);

        serviceCategoryService.disableCategory("cat-123");

        assertFalse(category.getActive());
        verify(serviceCategoryRepository).save(category);
    }

    @Test
    void disableCategory_HasActivePlans_ThrowsException() {
        when(serviceCategoryRepository.findById("cat-123")).thenReturn(Optional.of(category));
        when(servicePlanRepository.existsByCategoryIdAndActiveTrue("cat-123")).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> serviceCategoryService.disableCategory("cat-123"));

        assertEquals(ErrorCode.CATEGORY_HAS_ACTIVE_PLANS, exception.getErrorCode());
        verify(serviceCategoryRepository, never()).save(any());
    }

    @Test
    void enableCategory_Success() {
        category.setActive(false);
        when(serviceCategoryRepository.findById("cat-123")).thenReturn(Optional.of(category));

        serviceCategoryService.enableCategory("cat-123");

        assertTrue(category.getActive());
        verify(serviceCategoryRepository).save(category);
    }

    @Test
    void findAllCategories_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ServiceCategory> categoryPage = new PageImpl<>(List.of(category), pageable, 1);
        when(serviceCategoryRepository.findAll(pageable)).thenReturn(categoryPage);

        Page<ServiceCategoryResponse> result = serviceCategoryService.findAllCategories(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}
