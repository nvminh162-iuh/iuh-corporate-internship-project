package com.hs.user.service;

import java.util.List;

import com.hs.user.dto.request.CreateSupportRequestRequest;
import com.hs.user.dto.response.SupportCategoryResponse;
import com.hs.user.dto.response.SupportRequestResponse;

public interface SupportRequestService {

    List<SupportCategoryResponse> findAllActiveSupportCategories();

    SupportRequestResponse createSupportRequest(CreateSupportRequestRequest request, String customerId);
}
