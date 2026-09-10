package com.hs.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PlanFeatureRequest(
        @NotBlank(message = "Mã đặc điểm không được để trống")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Mã đặc điểm chỉ chứa chữ, số, _ và -")
        String code,

        @NotBlank(message = "Tên đặc điểm không được để trống")
        @Size(max = 120, message = "Tên đặc điểm tối đa 120 ký tự")
        String name,

        @NotBlank(message = "Giá trị đặc điểm không được để trống")
        @Size(max = 255, message = "Giá trị đặc điểm tối đa 255 ký tự")
        String value,

        @Size(max = 50, message = "Đơn vị tối đa 50 ký tự")
        String unit,

        Integer displayOrder
) {
}
