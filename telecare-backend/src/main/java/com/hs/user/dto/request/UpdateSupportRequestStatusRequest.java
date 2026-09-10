package com.hs.user.dto.request;

import com.hs.user.model.constant.SupportRequestStatus;

import jakarta.validation.constraints.NotNull;
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
public class UpdateSupportRequestStatusRequest {

    @NotNull(message = "Status is required")
    SupportRequestStatus status;

    String note;

    String resolution;
}
