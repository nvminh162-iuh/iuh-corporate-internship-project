package com.hs.user.dto.request;

import java.math.BigDecimal;

import com.hs.user.model.constant.BillingCycle;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PublicPlanQuery {
    String keyword;
    String categoryCode;
    BigDecimal minPrice;
    BigDecimal maxPrice;
    BillingCycle billingCycle;
    Boolean highlighted;
}
