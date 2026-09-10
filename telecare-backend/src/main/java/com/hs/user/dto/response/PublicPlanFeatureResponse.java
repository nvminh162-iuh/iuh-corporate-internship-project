package com.hs.user.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PublicPlanFeatureResponse {
    String code;
    String name;
    String value;
    String unit;
    Integer displayOrder;
}
