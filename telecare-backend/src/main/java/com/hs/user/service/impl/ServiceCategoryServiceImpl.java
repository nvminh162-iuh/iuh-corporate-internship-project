package com.hs.user.service.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hs.user.advice.base.AppException;
import com.hs.user.constant.base.ErrorCode;
import com.hs.user.dto.request.ServiceCategoryRequest;
import com.hs.user.dto.response.ServiceCategoryResponse;
import com.hs.user.mapper.ServiceCategoryMapper;
import com.hs.user.model.ServiceCategory;
import com.hs.user.model.User;
import com.hs.user.repository.ServiceCategoryRepository;
import com.hs.user.repository.ServicePlanRepository;
import com.hs.user.repository.UserRepository;
import com.hs.user.service.ServiceCategoryService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
public class ServiceCategoryServiceImpl implements ServiceCategoryService {

    ServiceCategoryRepository serviceCategoryRepository;
    ServicePlanRepository servicePlanRepository;
    UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<@NonNull ServiceCategoryResponse> findAllCategories(Pageable pageable) {
        Page<ServiceCategory> categoriesPage = serviceCategoryRepository.findAll(pageable);

        Set<String> actorIds = new HashSet<>();
        categoriesPage.getContent().forEach(cat -> {
            if (cat.getCreatedBy() != null) actorIds.add(cat.getCreatedBy());
            if (cat.getUpdatedBy() != null) actorIds.add(cat.getUpdatedBy());
        });
        Map<String, User> actors = getActorMap(actorIds);

        return categoriesPage.map(cat -> ServiceCategoryMapper.mapToServiceCategoryResponse(cat, actors));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceCategoryResponse> findAllActiveCategoriesForDropdown() {
        List<ServiceCategory> categories = serviceCategoryRepository.findAllByActiveTrueOrderByDisplayOrderAscCreatedAtDesc();

        Set<String> actorIds = new HashSet<>();
        categories.forEach(cat -> {
            if (cat.getCreatedBy() != null) actorIds.add(cat.getCreatedBy());
            if (cat.getUpdatedBy() != null) actorIds.add(cat.getUpdatedBy());
        });
        Map<String, User> actors = getActorMap(actorIds);

        return categories.stream()
                .map(cat -> ServiceCategoryMapper.mapToServiceCategoryResponse(cat, actors))
                .toList();
    }

    @Override
    public ServiceCategoryResponse createCategory(ServiceCategoryRequest request) {
        String code = request.code().trim().toUpperCase();

        if (serviceCategoryRepository.existsByCode(code)) {
            throw new AppException(ErrorCode.CATEGORY_CODE_EXISTED);
        }

        ServiceCategory category = ServiceCategory.builder()
                .code(code)
                .name(request.name().trim())
                .description(request.description() != null ? request.description().trim() : null)
                .displayOrder(request.displayOrder() != null ? request.displayOrder() : 0)
                .build();

        ServiceCategory savedCategory = serviceCategoryRepository.save(category);
        log.info("Created service category {} with code {}", savedCategory.getId(), savedCategory.getCode());

        Map<String, User> actors = getActorMap(getAuditActorIds(savedCategory));
        return ServiceCategoryMapper.mapToServiceCategoryResponse(savedCategory, actors);
    }

    @Override
    public ServiceCategoryResponse updateCategory(String id, ServiceCategoryRequest request) {
        ServiceCategory category = serviceCategoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXISTED));

        String code = request.code().trim().toUpperCase();
        if (serviceCategoryRepository.existsByCodeAndIdNot(code, id)) {
            throw new AppException(ErrorCode.CATEGORY_CODE_EXISTED);
        }

        category.setCode(code);
        category.setName(request.name().trim());
        category.setDescription(request.description() != null ? request.description().trim() : null);
        if (request.displayOrder() != null) {
            category.setDisplayOrder(request.displayOrder());
        }

        ServiceCategory updatedCategory = serviceCategoryRepository.save(category);
        log.info("Updated service category {}", updatedCategory.getId());

        Map<String, User> actors = getActorMap(getAuditActorIds(updatedCategory));
        return ServiceCategoryMapper.mapToServiceCategoryResponse(updatedCategory, actors);
    }

    @Override
    public void enableCategory(String id) {
        ServiceCategory category = serviceCategoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXISTED));

        category.setActive(true);
        serviceCategoryRepository.save(category);
        log.info("Enabled service category {}", id);
    }

    @Override
    public void disableCategory(String id) {
        ServiceCategory category = serviceCategoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXISTED));

        if (servicePlanRepository.existsByCategoryIdAndActiveTrue(id)) {
            throw new AppException(ErrorCode.CATEGORY_HAS_ACTIVE_PLANS);
        }

        category.setActive(false);
        serviceCategoryRepository.save(category);
        log.info("Disabled service category {}", id);
    }

    private Set<String> getAuditActorIds(ServiceCategory category) {
        Set<String> ids = new HashSet<>();
        if (category.getCreatedBy() != null) ids.add(category.getCreatedBy());
        if (category.getUpdatedBy() != null) ids.add(category.getUpdatedBy());
        return ids;
    }

    private Map<String, User> getActorMap(Set<String> actorIds) {
        if (actorIds == null || actorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<String> cleanIds = actorIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());
        if (cleanIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRepository.findAllById(cleanIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }
}
