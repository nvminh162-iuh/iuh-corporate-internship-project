package com.hs.user.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record UpsertRoleRequest(

        @NotBlank(message = "Role name không được để trống")
        String name,

        String description,

        Set<String> permissionIdList
) {
}
