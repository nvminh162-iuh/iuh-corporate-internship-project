package com.hs.user.service.impl;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hs.user.advice.base.AppException;
import com.hs.user.constant.base.ErrorCode;
import com.hs.user.dto.request.AddSupportRequestHistoryRequest;
import com.hs.user.dto.request.AssignSupportRequestRequest;
import com.hs.user.dto.request.SupportRequestAdminQuery;
import com.hs.user.dto.request.UpdateSupportRequestStatusRequest;
import com.hs.user.dto.response.SupportRequestAdminDetailResponse;
import com.hs.user.dto.response.SupportRequestAdminSummaryResponse;
import com.hs.user.dto.response.SupportRequestHistoryResponse;
import com.hs.user.dto.response.UserResponse;
import com.hs.user.mapper.SupportRequestMapper;
import com.hs.user.mapper.UserMapper;
import com.hs.user.model.SupportRequest;
import com.hs.user.model.SupportRequestHistory;
import com.hs.user.model.User;
import com.hs.user.model.constant.SupportRequestHistoryAction;
import com.hs.user.model.constant.SupportRequestStatus;
import com.hs.user.repository.SupportRequestHistoryRepository;
import com.hs.user.repository.SupportRequestRepository;
import com.hs.user.repository.UserRepository;
import com.hs.user.repository.specification.SupportRequestSpecification;
import com.hs.user.service.SupportRequestAdminService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SupportRequestAdminServiceImpl implements SupportRequestAdminService {

    SupportRequestRepository supportRequestRepository;
    SupportRequestHistoryRepository supportRequestHistoryRepository;
    UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<SupportRequestAdminSummaryResponse> findAllAdminSupportRequests(SupportRequestAdminQuery query, Pageable pageable) {
        Page<SupportRequest> requestsPage = supportRequestRepository.findAll(
                SupportRequestSpecification.filterAdminRequests(query),
                pageable
        );

        return requestsPage.map(req -> {
            UserResponse customer = resolveUser(req.getCustomerId());
            UserResponse assignedTo = req.getAssignedTo() != null ? UserMapper.mapToUserResponse(req.getAssignedTo()) : null;
            return SupportRequestMapper.mapToAdminSummaryResponse(req, customer, assignedTo);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public SupportRequestAdminDetailResponse findAdminSupportRequestById(String id) {
        SupportRequest req = findEntityById(id);
        UserResponse customer = resolveUser(req.getCustomerId());
        UserResponse assignedTo = req.getAssignedTo() != null ? UserMapper.mapToUserResponse(req.getAssignedTo()) : null;

        return SupportRequestMapper.mapToAdminDetailResponse(req, customer, assignedTo);
    }

    @Override
    @Transactional
    public SupportRequestAdminDetailResponse receiveSupportRequest(String id, String actorId) {
        SupportRequest req = findEntityById(id);

        if (req.getStatus() == SupportRequestStatus.CLOSED) {
            throw new AppException(ErrorCode.CLOSED_TICKET_CANNOT_BE_UPDATED);
        }

        if (req.getStatus() != SupportRequestStatus.NEW) {
            throw new AppException(ErrorCode.INVALID_SUPPORT_REQUEST_STATUS);
        }

        SupportRequestStatus oldStatus = req.getStatus();
        req.setStatus(SupportRequestStatus.RECEIVED);
        req.setReceivedAt(Instant.now());

        SupportRequest saved = supportRequestRepository.save(req);

        createHistoryEntry(saved, oldStatus, SupportRequestStatus.RECEIVED, SupportRequestHistoryAction.STATUS_CHANGED, "Đã tiếp nhận ticket", actorId);

        log.info("Ticket id={} ticketCode={} received by actorId={}", id, req.getTicketCode(), actorId);
        return findAdminSupportRequestById(id);
    }

    @Override
    @Transactional
    public SupportRequestAdminDetailResponse assignSupportRequest(String id, AssignSupportRequestRequest request, String actorId) {
        SupportRequest req = findEntityById(id);

        if (req.getStatus() == SupportRequestStatus.CLOSED) {
            throw new AppException(ErrorCode.CLOSED_TICKET_CANNOT_BE_UPDATED);
        }

        if (req.getStatus() == SupportRequestStatus.NEW) {
            throw new AppException(ErrorCode.TICKET_MUST_BE_RECEIVED_FIRST);
        }

        User staff = userRepository.findById(request.getAssignedTo())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        SupportRequestStatus oldStatus = req.getStatus();
        SupportRequestStatus newStatus = oldStatus;

        req.setAssignedTo(staff);
        req.setAssignedAt(Instant.now());

        // If ticket was RECEIVED, transition automatically to IN_PROGRESS upon assignment
        if (oldStatus == SupportRequestStatus.RECEIVED) {
            newStatus = SupportRequestStatus.IN_PROGRESS;
            req.setStatus(newStatus);
        }

        SupportRequest saved = supportRequestRepository.save(req);

        String note = (request.getNote() != null && !request.getNote().isBlank())
                ? request.getNote().trim()
                : "Phân công xử lý cho nhân viên: " + staff.getUsername();

        createHistoryEntry(saved, oldStatus, newStatus, SupportRequestHistoryAction.ASSIGNED, note, actorId);

        log.info("Ticket id={} assigned to staffId={} by actorId={}", id, staff.getId(), actorId);
        return findAdminSupportRequestById(id);
    }

    @Override
    @Transactional
    public SupportRequestAdminDetailResponse updateSupportRequestStatus(String id, UpdateSupportRequestStatusRequest request, String actorId) {
        SupportRequest req = findEntityById(id);

        if (req.getStatus() == SupportRequestStatus.CLOSED) {
            throw new AppException(ErrorCode.CLOSED_TICKET_CANNOT_BE_UPDATED);
        }

        SupportRequestStatus oldStatus = req.getStatus();
        SupportRequestStatus targetStatus = request.getStatus();

        if (oldStatus == targetStatus) {
            return findAdminSupportRequestById(id);
        }

        // Must be received before transitioning to IN_PROGRESS, WAITING_CUSTOMER, COMPLETED
        if (oldStatus == SupportRequestStatus.NEW && targetStatus != SupportRequestStatus.RECEIVED) {
            throw new AppException(ErrorCode.TICKET_MUST_BE_RECEIVED_FIRST);
        }

        String note = request.getNote() != null ? request.getNote().trim() : "";

        // Require note for WAITING_CUSTOMER and COMPLETED
        if ((targetStatus == SupportRequestStatus.WAITING_CUSTOMER || targetStatus == SupportRequestStatus.COMPLETED) && note.isBlank()) {
            throw new AppException(ErrorCode.SUPPORT_REQUEST_NOTE_REQUIRED);
        }

        // Set milestone timestamps & resolution
        if (targetStatus == SupportRequestStatus.COMPLETED) {
            req.setCompletedAt(Instant.now());
            if (request.getResolution() != null && !request.getResolution().isBlank()) {
                req.setResolution(request.getResolution().trim());
            } else {
                req.setResolution(note);
            }
        } else if (targetStatus == SupportRequestStatus.CLOSED) {
            req.setClosedAt(Instant.now());
        }

        req.setStatus(targetStatus);
        SupportRequest saved = supportRequestRepository.save(req);

        createHistoryEntry(saved, oldStatus, targetStatus, SupportRequestHistoryAction.STATUS_CHANGED, note.isBlank() ? "Cập nhật trạng thái sang " + targetStatus : note, actorId);

        log.info("Ticket id={} status updated from {} to {} by actorId={}", id, oldStatus, targetStatus, actorId);
        return findAdminSupportRequestById(id);
    }

    @Override
    @Transactional
    public SupportRequestHistoryResponse addSupportRequestHistoryNote(String id, AddSupportRequestHistoryRequest request, String actorId) {
        SupportRequest req = findEntityById(id);

        if (req.getStatus() == SupportRequestStatus.CLOSED) {
            throw new AppException(ErrorCode.CLOSED_TICKET_CANNOT_BE_UPDATED);
        }

        SupportRequestHistory history = createHistoryEntry(
                req,
                req.getStatus(),
                req.getStatus(),
                SupportRequestHistoryAction.NOTE_ADDED,
                request.getNote().trim(),
                actorId
        );

        UserResponse actor = resolveUser(actorId);
        return SupportRequestMapper.mapToHistoryResponse(history, actor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportRequestHistoryResponse> findSupportRequestHistories(String id) {
        // Ensure ticket exists
        findEntityById(id);

        List<SupportRequestHistory> histories = supportRequestHistoryRepository.findBySupportRequestIdOrderByCreatedAtDesc(id);

        return histories.stream().map(h -> {
            UserResponse actor = resolveUser(h.getActorId());
            return SupportRequestMapper.mapToHistoryResponse(h, actor);
        }).toList();
    }

    private SupportRequest findEntityById(String id) {
        return supportRequestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUPPORT_REQUEST_NOT_EXISTED));
    }

    private SupportRequestHistory createHistoryEntry(
            SupportRequest request,
            SupportRequestStatus fromStatus,
            SupportRequestStatus toStatus,
            SupportRequestHistoryAction action,
            String note,
            String actorId
    ) {
        SupportRequestHistory history = SupportRequestHistory.builder()
                .supportRequest(request)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .action(action)
                .note(note)
                .actorId(actorId)
                .build();
        history.setActive(true);

        return supportRequestHistoryRepository.save(history);
    }

    private UserResponse resolveUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        return userRepository.findById(userId)
                .map(UserMapper::mapToUserResponse)
                .orElse(UserResponse.builder().id(userId).username(userId).build());
    }
}
