package com.hs.user.dto.request;

import java.time.Instant;

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
public class SupportRequestAdminQuery {

    String keyword;
    SupportRequestStatus status;
    String categoryCode;
    String assignedTo;
    Instant fromDate;
    Instant toDate;
}
