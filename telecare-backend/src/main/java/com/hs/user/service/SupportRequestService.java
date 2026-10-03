package com.hs.user.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.hs.user.dto.request.CreateSupportRequestRequest;
import com.hs.user.dto.request.CustomerSupportRequestQuery;
import com.hs.user.dto.response.CustomerSupportRequestDetailResponse;
import com.hs.user.dto.response.CustomerSupportRequestHistoryResponse;
import com.hs.user.dto.response.CustomerSupportRequestSummaryResponse;
import com.hs.user.dto.response.SupportCategoryResponse;
import com.hs.user.dto.response.SupportRequestResponse;

public interface SupportRequestService {

    List<SupportCategoryResponse> findAllActiveSupportCategories();

    SupportRequestResponse createSupportRequest(CreateSupportRequestRequest request, String customerId);

    Page<CustomerSupportRequestSummaryResponse> findMySupportRequests(String customerId, CustomerSupportRequestQuery query, Pageable pageable);

    CustomerSupportRequestDetailResponse findMySupportRequestDetail(String customerId, String id);

    List<CustomerSupportRequestHistoryResponse> findMySupportRequestHistories(String customerId, String id);
}
