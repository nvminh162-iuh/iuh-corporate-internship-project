package com.hs.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ServiceCategoryRequest(
        @NotBlank(message = "Mã nhóm dịch vụ không được để trống")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Mã nhóm dịch vụ chỉ chứa chữ, số, _ và -")
        String code,

        @NotBlank(message = "Tên nhóm dịch vụ không được để trống")
        @Size(min = 3, max = 120, message = "Tên nhóm dịch vụ phải từ 3 đến 120 ký tự")
        String name,

        @Size(max = 5000, message = "Mô tả tối đa 5000 ký tự")
        String description,

        Integer displayOrder
) {
}
