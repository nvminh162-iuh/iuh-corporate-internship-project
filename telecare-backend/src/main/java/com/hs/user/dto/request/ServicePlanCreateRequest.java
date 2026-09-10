package com.hs.user.dto.request;

import java.math.BigDecimal;
import java.util.List;

import com.hs.user.model.constant.BillingCycle;
import com.hs.user.model.constant.PlanStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ServicePlanCreateRequest(
        @NotBlank(message = "Mã gói cước không được để trống")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Mã gói cước chỉ chứa chữ, số, _ và -")
        String code,

        @NotBlank(message = "Slug không được để trống")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug chỉ chứa chữ thường, số và dấu gạch ngang")
        String slug,

        @NotBlank(message = "Tên gói cước không được để trống")
        @Size(min = 3, max = 120, message = "Tên gói cước phải từ 3 đến 120 ký tự")
        String name,

        @Size(max = 255, message = "Tóm tắt tối đa 255 ký tự")
        String summary,

        @Size(max = 5000, message = "Mô tả tối đa 5000 ký tự")
        String description,

        @NotNull(message = "Giá gói cước không được để trống")
        @DecimalMin(value = "0.00", message = "Giá gói cước không được âm")
        BigDecimal price,

        @Size(min = 3, max = 3, message = "Đơn vị tiền tệ phải đúng 3 ký tự")
        String currency,

        @NotNull(message = "Chu kỳ thanh toán không được để trống")
        BillingCycle billingCycle,

        PlanStatus status,

        @NotBlank(message = "Nhóm dịch vụ không được để trống")
        String categoryId,

        Boolean highlighted,

        Integer displayOrder,

        List<@Valid PlanFeatureRequest> features
) {
}
