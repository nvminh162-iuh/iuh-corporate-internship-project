package com.hs.user.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

import com.hs.user.model.constant.Gender;

public record OnboardingRequest(
        @NotBlank(message = "Tên không được để trống")
        @Size(min = 2, max = 50, message = "Tên phải từ 2-50 ký tự")
        String firstName,

        @NotBlank(message = "Họ không được để trống")
        @Size(min = 2, max = 50, message = "Họ phải từ 2-50 ký tự")
        String lastName,

        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(regexp = "^[0-9]{10,15}$", message = "Số điện thoại phải từ 10-11 số")
        String phone,

        @NotNull(message = "Ngày sinh không được để trống")
        @Past(message = "Ngày sinh phải là một ngày trong quá khứ")
        LocalDate dob,

        @NotNull(message = "Giới tính không được để trống")
        Gender gender
) {
}
