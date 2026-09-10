package com.hs.user.dto.response;

import java.math.BigDecimal;

import com.hs.user.model.constant.BillingCycle;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PublicPlanSummaryResponse {
    String id;
    String code;
    String slug;
    String name;
    String summary;
    BigDecimal price;
    String currency;
    BillingCycle billingCycle;
    Boolean highlighted;
    Integer displayOrder;
    PublicCategoryResponse category;
    Integer featureCount;
}
