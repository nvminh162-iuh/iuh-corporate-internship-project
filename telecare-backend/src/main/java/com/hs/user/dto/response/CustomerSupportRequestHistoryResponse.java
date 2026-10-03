package com.hs.user.dto.response;

import java.time.Instant;

import com.hs.user.model.constant.SupportRequestHistoryAction;
import com.hs.user.model.constant.SupportRequestStatus;

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
public class CustomerSupportRequestHistoryResponse {

    String id;

    SupportRequestHistoryAction action;

    SupportRequestStatus fromStatus;

    SupportRequestStatus toStatus;

    String message;

    Instant createdAt;
}
