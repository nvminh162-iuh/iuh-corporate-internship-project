package com.hs.user.mapper;

import java.util.Map;

import com.hs.user.dto.response.ServiceCategoryResponse;
import com.hs.user.model.ServiceCategory;
import com.hs.user.model.User;

public class ServiceCategoryMapper {

    private ServiceCategoryMapper() {
    }

    public static ServiceCategoryResponse mapToServiceCategoryResponse(ServiceCategory category, Map<String, User> actors) {
        if (category == null) {
            return null;
        }

        return ServiceCategoryResponse.builder()
                .id(category.getId())
                .code(category.getCode())
                .name(category.getName())
                .description(category.getDescription())
                .displayOrder(category.getDisplayOrder())
                .active(category.getActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .createdBy(UserMapper.resolveActor(category.getCreatedBy(), actors))
                .updatedBy(UserMapper.resolveActor(category.getUpdatedBy(), actors))
                .build();
    }
}
