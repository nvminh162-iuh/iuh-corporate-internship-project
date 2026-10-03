package com.hs.user.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hs.user.advice.base.AppException;
import com.hs.user.constant.base.ErrorCode;
import com.hs.user.dto.request.CreateSupportRequestRequest;
import com.hs.user.dto.response.SupportCategoryResponse;
import com.hs.user.dto.response.SupportRequestResponse;
import com.hs.user.mapper.SupportRequestMapper;
import com.hs.user.model.ServicePlan;
import com.hs.user.model.SupportCategory;
import com.hs.user.model.SupportRequest;
import com.hs.user.model.SupportRequestHistory;
import com.hs.user.model.constant.SupportRequestHistoryAction;
import com.hs.user.model.constant.SupportRequestStatus;
import com.hs.user.repository.ServicePlanRepository;
import com.hs.user.repository.SupportCategoryRepository;
import com.hs.user.repository.SupportRequestHistoryRepository;
import com.hs.user.repository.SupportRequestRepository;
import com.hs.user.repository.specification.SupportRequestSpecification;
import com.hs.user.service.SupportRequestService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SupportRequestServiceImpl implements SupportRequestService {

    SupportCategoryRepository supportCategoryRepository;
    SupportRequestRepository supportRequestRepository;
    SupportRequestHistoryRepository supportRequestHistoryRepository;
    ServicePlanRepository servicePlanRepository;

    private static final AtomicLong fallbackSequence = new AtomicLong(1);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt",
            "updatedAt",
            "status",
            "ticketCode",
            "subject",
            "id"
    );

    @Override
    @Transactional(readOnly = true)
    public List<SupportCategoryResponse> findAllActiveSupportCategories() {
        return supportCategoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(SupportRequestMapper::mapToSupportCategoryResponse)
                .toList();
    }

    @Override
    @Transactional
    public SupportRequestResponse createSupportRequest(CreateSupportRequestRequest request, String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // Validate Category
        SupportCategory category = supportCategoryRepository.findByCodeAndActiveTrue(request.getCategoryCode())
                .orElseThrow(() -> new AppException(ErrorCode.SUPPORT_CATEGORY_NOT_EXISTED));

        // Validate Optional Service Plan
        ServicePlan servicePlan = null;
        if (request.getServicePlanId() != null && !request.getServicePlanId().isBlank()) {
            servicePlan = servicePlanRepository.findById(request.getServicePlanId())
                    .filter(p -> p.getActive() != null && p.getActive())
                    .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_EXISTED));
        }

        // Generate Ticket Code atomically
        String ticketCode = generateTicketCode();

        // Build Ticket Entity
        SupportRequest supportRequest = SupportRequest.builder()
                .ticketCode(ticketCode)
                .customerId(customerId)
                .category(category)
                .servicePlan(servicePlan)
                .subject(request.getSubject().trim())
                .content(request.getContent().trim())
                .contactPhone(request.getContactPhone() != null ? request.getContactPhone().trim() : null)
                .contactEmail(request.getContactEmail() != null ? request.getContactEmail().trim() : null)
                .status(SupportRequestStatus.NEW)
                .build();
        supportRequest.setActive(true);

        SupportRequest savedRequest = supportRequestRepository.save(supportRequest);

        // Build & Save Initial History Entry (Action: CREATED, fromStatus: null, toStatus: NEW)
        SupportRequestHistory history = SupportRequestHistory.builder()
                .supportRequest(savedRequest)
                .fromStatus(null)
                .toStatus(SupportRequestStatus.NEW)
                .action(SupportRequestHistoryAction.CREATED)
                .note("Yêu cầu hỗ trợ được tạo mới bởi khách hàng")
                .actorId(customerId)
                .build();
        history.setActive(true);

        supportRequestHistoryRepository.save(history);

        log.info("Successfully created support request ticketCode={} for customerId={}", ticketCode, customerId);

        return SupportRequestMapper.mapToSupportRequestResponse(savedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<com.hs.user.dto.response.CustomerSupportRequestSummaryResponse> findMySupportRequests(
            String customerId,
            com.hs.user.dto.request.CustomerSupportRequestQuery query,
            org.springframework.data.domain.Pageable pageable
    ) {
        if (customerId == null || customerId.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (query != null && query.getFromDate() != null && query.getToDate() != null
                && query.getFromDate().isAfter(query.getToDate())) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        validateSortFields(pageable.getSort());

        int pageSize = Math.min(Math.max(pageable.getPageSize(), 1), 100);
        int pageNumber = Math.max(pageable.getPageNumber(), 0);
        org.springframework.data.domain.Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort().and(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"))
                : org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
                        .and(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"));
        org.springframework.data.domain.Pageable safePageable = org.springframework.data.domain.PageRequest.of(pageNumber, pageSize, sort);

        org.springframework.data.domain.Page<SupportRequest> page = supportRequestRepository.findAll(
                SupportRequestSpecification.filterCustomerRequests(customerId, query),
                safePageable
        );

        return page.map(SupportRequestMapper::mapToCustomerSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public com.hs.user.dto.response.CustomerSupportRequestDetailResponse findMySupportRequestDetail(String customerId, String id) {
        if (customerId == null || customerId.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        SupportRequest request = supportRequestRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new AppException(ErrorCode.SUPPORT_REQUEST_NOT_EXISTED));

        return SupportRequestMapper.mapToCustomerDetailResponse(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.hs.user.dto.response.CustomerSupportRequestHistoryResponse> findMySupportRequestHistories(String customerId, String id) {
        if (customerId == null || customerId.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // Verify ticket ownership first - throws 404 if not found or not owned
        supportRequestRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new AppException(ErrorCode.SUPPORT_REQUEST_NOT_EXISTED));

        List<SupportRequestHistory> histories = supportRequestHistoryRepository.findBySupportRequestIdOrderByCreatedAtAsc(id);

        return histories.stream()
                .filter(h -> h.getAction() == SupportRequestHistoryAction.CREATED || h.getAction() == SupportRequestHistoryAction.STATUS_CHANGED)
                .map(SupportRequestMapper::mapToCustomerHistoryResponse)
                .toList();
    }

    private synchronized String generateTicketCode() {
        String datePrefix = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).format(DATE_FORMATTER);
        long seqValue;

        try {
            Long nextSeq = supportRequestRepository.getNextTicketSequence();
            if (nextSeq != null && nextSeq > 0) {
                seqValue = nextSeq;
            } else {
                log.warn("Database sequence support_request_ticket_seq returned non-positive value: {}, using synchronized fallback", nextSeq);
                seqValue = fallbackSequence.getAndIncrement();
            }
        } catch (Exception e) {
            log.warn("Could not query database sequence support_request_ticket_seq (error: {}), using synchronized fallback sequence", e.getMessage());
            seqValue = fallbackSequence.getAndIncrement();
        }

        String formattedSeq = String.format("%05d", seqValue);
        String code = "SR-" + datePrefix + "-" + formattedSeq;

        while (supportRequestRepository.existsByTicketCode(code)) {
            seqValue = fallbackSequence.getAndIncrement();
            code = "SR-" + datePrefix + "-" + String.format("%05d", seqValue);
        }

        return code;
    }

    private void validateSortFields(org.springframework.data.domain.Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return;
        }
        for (org.springframework.data.domain.Sort.Order order : sort) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new AppException(ErrorCode.INVALID_SORT_FIELD);
            }
        }
    }
}
