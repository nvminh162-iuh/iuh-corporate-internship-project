package com.hs.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateSupportRequestRequest {

    @NotBlank(message = "Category code is required")
    @Size(max = 50, message = "Category code must not exceed 50 characters")
    String categoryCode;

    String servicePlanId;

    @NotBlank(message = "Subject is required")
    @Size(min = 10, max = 200, message = "Subject must be between 10 and 200 characters")
    String subject;

    @NotBlank(message = "Content is required")
    @Size(min = 20, max = 2000, message = "Content must be between 20 and 2000 characters")
    String content;

    @Pattern(regexp = "^$|^[0-9+() -]{8,20}$", message = "Contact phone must be a valid phone number")
    String contactPhone;

    @Email(message = "Contact email must be a valid email address")
    String contactEmail;
}
