package com.hs.user.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.hs.user.dto.request.AddSupportRequestHistoryRequest;
import com.hs.user.dto.request.AssignSupportRequestRequest;
import com.hs.user.dto.request.SupportRequestAdminQuery;
import com.hs.user.dto.request.UpdateSupportRequestStatusRequest;
import com.hs.user.dto.response.SupportRequestAdminDetailResponse;
import com.hs.user.dto.response.SupportRequestAdminSummaryResponse;
import com.hs.user.dto.response.SupportRequestHistoryResponse;

public interface SupportRequestAdminService {

    Page<SupportRequestAdminSummaryResponse> findAllAdminSupportRequests(SupportRequestAdminQuery query, Pageable pageable);

    SupportRequestAdminDetailResponse findAdminSupportRequestById(String id);

    SupportRequestAdminDetailResponse receiveSupportRequest(String id, String actorId);

    SupportRequestAdminDetailResponse assignSupportRequest(String id, AssignSupportRequestRequest request, String actorId);

    SupportRequestAdminDetailResponse updateSupportRequestStatus(String id, UpdateSupportRequestStatusRequest request, String actorId);

    SupportRequestHistoryResponse addSupportRequestHistoryNote(String id, AddSupportRequestHistoryRequest request, String actorId);

    List<SupportRequestHistoryResponse> findSupportRequestHistories(String id);
}
