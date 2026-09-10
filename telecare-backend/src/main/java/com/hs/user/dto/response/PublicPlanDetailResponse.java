package com.hs.user.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.hs.user.model.constant.BillingCycle;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PublicPlanDetailResponse {
    String id;
    String code;
    String slug;
    String name;
    String summary;
    String description;
    BigDecimal price;
    String currency;
    BillingCycle billingCycle;
    Boolean highlighted;
    Integer displayOrder;
    PublicCategoryResponse category;
    List<PublicPlanFeatureResponse> features;
}
