package com.hs.user.dto.request;

import java.time.LocalDate;

import com.hs.user.model.constant.Gender;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 3, max = 50, message = "Tên đăng nhập phải từ 3-50 ký tự")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Tên đăng nhập chỉ được chứa chữ, số, dấu chấm, gạch dưới hoặc gạch ngang")
        String username,

        @Email(message = "Email không hợp lệ")
        @Size(max = 100, message = "Email tối đa 100 ký tự")
        String email,

        @Size(min = 2, max = 50, message = "Tên phải từ 2-50 ký tự")
        String firstName,

        @Size(min = 2, max = 50, message = "Họ phải từ 2-50 ký tự")
        String lastName,

        @Pattern(regexp = "^[0-9]{10,15}$", message = "Số điện thoại phải từ 10-15 số")
        String phone,

        @Past(message = "Ngày sinh phải là một ngày trong quá khứ")
        LocalDate dob,

        Gender gender
) {
}
