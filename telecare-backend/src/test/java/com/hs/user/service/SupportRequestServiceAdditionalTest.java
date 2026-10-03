package com.hs.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.hs.user.advice.base.AppException;
import com.hs.user.constant.base.ErrorCode;
import com.hs.user.dto.request.CreateSupportRequestRequest;
import com.hs.user.dto.request.CustomerSupportRequestQuery;
import com.hs.user.dto.response.CustomerSupportRequestDetailResponse;
import com.hs.user.dto.response.CustomerSupportRequestHistoryResponse;
import com.hs.user.dto.response.CustomerSupportRequestSummaryResponse;
import com.hs.user.model.SupportCategory;
import com.hs.user.model.SupportRequest;
import com.hs.user.model.SupportRequestHistory;
import com.hs.user.model.constant.SupportRequestHistoryAction;
import com.hs.user.model.constant.SupportRequestStatus;
import com.hs.user.repository.ServicePlanRepository;
import com.hs.user.repository.SupportCategoryRepository;
import com.hs.user.repository.SupportRequestHistoryRepository;
import com.hs.user.repository.SupportRequestRepository;
import com.hs.user.service.impl.SupportRequestServiceImpl;

import java.time.LocalDate;

/**
 * SR-C-010..016: Additional Support Request Service test cases not covered
 * in SupportRequestServiceTest.java
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SupportRequestService - Additional Tests (SR-C-010 to SR-C-016)")
class SupportRequestServiceAdditionalTest {

    @Mock
    private SupportCategoryRepository supportCategoryRepository;

    @Mock
    private SupportRequestRepository supportRequestRepository;

    @Mock
    private SupportRequestHistoryRepository supportRequestHistoryRepository;

    @Mock
    private ServicePlanRepository servicePlanRepository;

    @InjectMocks
    private SupportRequestServiceImpl supportRequestService;

    private SupportCategory activeCategory;
    private SupportRequest ticket1;
    private SupportRequest ticket2OtherUser;

    @BeforeEach
    void setUp() {
        activeCategory = SupportCategory.builder()
                .id("cat-1")
                .code("CONNECTION")
                .name("Ket noi Internet")
                .build();
        activeCategory.setActive(true);

        ticket1 = SupportRequest.builder()
                .id("sr-1")
                .ticketCode("SR-20261003-00001")
                .customerId("cust-1")
                .category(activeCategory)
                .subject("Ho tro ky thuat mang vien thong")
                .content("Mo ta chi tiet su co mang tai nha...")
                .status(SupportRequestStatus.NEW)
                .build();

        ticket2OtherUser = SupportRequest.builder()
                .id("sr-other")
                .ticketCode("SR-20261003-00002")
                .customerId("cust-2")
                .category(activeCategory)
                .subject("Ticket cua nguoi khac")
                .content("Noi dung khong lien quan...")
                .status(SupportRequestStatus.IN_PROGRESS)
                .build();
    }

    // ==========================================
    // SR-C-010: findMySupportRequests - pagination
    // ==========================================
    @Test
    @DisplayName("SR-C-010: findMySupportRequests returns only tickets belonging to given customer")
    void findMySupportRequests_ReturnsCustomerTickets() {
        Page<SupportRequest> page = new PageImpl<>(List.of(ticket1), PageRequest.of(0, 10), 1);
        when(supportRequestRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        Page<CustomerSupportRequestSummaryResponse> result = supportRequestService
                .findMySupportRequests("cust-1", null, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTicketCode()).isEqualTo("SR-20261003-00001");
    }

    @Test
    @DisplayName("SR-C-010: findMySupportRequests with blank customerId throws UNAUTHENTICATED")
    void findMySupportRequests_BlankCustomerId_ThrowsUnauthenticated() {
        assertThatThrownBy(() -> supportRequestService.findMySupportRequests("  ", null, PageRequest.of(0, 10)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHENTICATED);
    }

    // ==========================================
    // SR-C-011: findMySupportRequestDetail - owned ticket
    // ==========================================
    @Test
    @DisplayName("SR-C-011: findMySupportRequestDetail returns detail when customer owns the ticket")
    void findMySupportRequestDetail_OwnedTicket_ReturnsDetail() {
        when(supportRequestRepository.findByIdAndCustomerId("sr-1", "cust-1"))
                .thenReturn(Optional.of(ticket1));

        CustomerSupportRequestDetailResponse response =
                supportRequestService.findMySupportRequestDetail("cust-1", "sr-1");

        assertThat(response).isNotNull();
        assertThat(response.getTicketCode()).isEqualTo("SR-20261003-00001");
        assertThat(response.getStatus()).isEqualTo(SupportRequestStatus.NEW);
    }

    // ==========================================
    // SR-C-012: findMySupportRequestDetail - not owned -> 404 (anti-IDOR)
    // ==========================================
    @Test
    @DisplayName("SR-C-012: findMySupportRequestDetail returns SUPPORT_REQUEST_NOT_EXISTED for another user's ticket (anti-IDOR)")
    void findMySupportRequestDetail_NotOwnedTicket_ReturnsNotFound() {
        when(supportRequestRepository.findByIdAndCustomerId("sr-other", "cust-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> supportRequestService.findMySupportRequestDetail("cust-1", "sr-other"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SUPPORT_REQUEST_NOT_EXISTED);
    }

    // ==========================================
    // SR-C-013: findMySupportRequestHistories
    // ==========================================
    @Test
    @DisplayName("SR-C-013: findMySupportRequestHistories returns only CREATED and STATUS_CHANGED events")
    void findMySupportRequestHistories_FiltersToPublicActions() {
        SupportRequestHistory historyCreated = SupportRequestHistory.builder()
                .id("hist-1")
                .supportRequest(ticket1)
                .action(SupportRequestHistoryAction.CREATED)
                .fromStatus(null)
                .toStatus(SupportRequestStatus.NEW)
                .build();

        // ASSIGNED action - should be filtered out from customer view
        SupportRequestHistory historyAssigned = SupportRequestHistory.builder()
                .id("hist-2")
                .supportRequest(ticket1)
                .action(SupportRequestHistoryAction.ASSIGNED)
                .fromStatus(SupportRequestStatus.NEW)
                .toStatus(SupportRequestStatus.NEW)
                .build();

        SupportRequestHistory historyStatusChanged = SupportRequestHistory.builder()
                .id("hist-3")
                .supportRequest(ticket1)
                .action(SupportRequestHistoryAction.STATUS_CHANGED)
                .fromStatus(SupportRequestStatus.NEW)
                .toStatus(SupportRequestStatus.IN_PROGRESS)
                .build();

        when(supportRequestRepository.findByIdAndCustomerId("sr-1", "cust-1"))
                .thenReturn(Optional.of(ticket1));
        when(supportRequestHistoryRepository.findBySupportRequestIdOrderByCreatedAtAsc("sr-1"))
                .thenReturn(List.of(historyCreated, historyAssigned, historyStatusChanged));

        List<CustomerSupportRequestHistoryResponse> result =
                supportRequestService.findMySupportRequestHistories("cust-1", "sr-1");

        // Only CREATED and STATUS_CHANGED should be visible to customer
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAction()).isEqualTo(SupportRequestHistoryAction.CREATED);
        assertThat(result.get(1).getAction()).isEqualTo(SupportRequestHistoryAction.STATUS_CHANGED);
    }

    @Test
    @DisplayName("SR-C-013: findMySupportRequestHistories throws 404 for non-owned ticket")
    void findMySupportRequestHistories_NotOwned_ThrowsNotFound() {
        when(supportRequestRepository.findByIdAndCustomerId("sr-other", "cust-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> supportRequestService.findMySupportRequestHistories("cust-1", "sr-other"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SUPPORT_REQUEST_NOT_EXISTED);
    }

    // ==========================================
    // SR-C-015: Date range valid
    // ==========================================
    @Test
    @DisplayName("SR-C-015: findMySupportRequests with valid date range (fromDate <= toDate) succeeds")
    void findMySupportRequests_ValidDateRange_Succeeds() {
        CustomerSupportRequestQuery query = CustomerSupportRequestQuery.builder()
                .fromDate(LocalDate.of(2026, 1, 1))
                .toDate(LocalDate.of(2026, 12, 31))
                .build();

        Page<SupportRequest> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(supportRequestRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        Page<CustomerSupportRequestSummaryResponse> result =
                supportRequestService.findMySupportRequests("cust-1", query, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isZero();
    }

    // ==========================================
    // SR-C-016: Date range invalid (fromDate > toDate)
    // ==========================================
    @Test
    @DisplayName("SR-C-016: findMySupportRequests with fromDate > toDate throws INVALID_DATE_RANGE")
    void findMySupportRequests_InvalidDateRange_ThrowsException() {
        CustomerSupportRequestQuery query = CustomerSupportRequestQuery.builder()
                .fromDate(LocalDate.of(2026, 12, 31))
                .toDate(LocalDate.of(2026, 1, 1))
                .build();

        assertThatThrownBy(() ->
                supportRequestService.findMySupportRequests("cust-1", query, PageRequest.of(0, 10)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_DATE_RANGE);

        verify(supportRequestRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    // ==========================================
    // Additional: findAllActiveSupportCategories
    // ==========================================
    @Test
    @DisplayName("findAllActiveSupportCategories returns empty list when no active categories")
    void findAllActiveSupportCategories_EmptyResult_ReturnsEmptyList() {
        when(supportCategoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(List.of());

        var result = supportRequestService.findAllActiveSupportCategories();

        assertThat(result).isEmpty();
    }
}
