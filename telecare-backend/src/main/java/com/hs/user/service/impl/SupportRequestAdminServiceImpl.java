package com.hs.user.service.impl;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hs.user.advice.base.AppException;
import com.hs.user.constant.PermissionConstants;
import com.hs.user.constant.RoleConstants;
import com.hs.user.constant.base.ErrorCode;
import com.hs.user.dto.request.AddSupportRequestHistoryRequest;
import com.hs.user.dto.request.AssignSupportRequestRequest;
import com.hs.user.dto.request.SupportRequestAdminQuery;
import com.hs.user.dto.request.UpdateSupportRequestStatusRequest;
import com.hs.user.dto.response.SupportRequestAdminAssigneeResponse;
import com.hs.user.dto.response.SupportRequestAdminDetailResponse;
import com.hs.user.dto.response.SupportRequestAdminSummaryResponse;
import com.hs.user.dto.response.SupportRequestHistoryResponse;
import com.hs.user.dto.response.UserResponse;
import com.hs.user.mapper.SupportRequestMapper;
import com.hs.user.mapper.UserMapper;
import com.hs.user.model.Role;
import com.hs.user.model.SupportRequest;
import com.hs.user.model.SupportRequestHistory;
import com.hs.user.model.User;
import com.hs.user.model.constant.NotificationType;
import com.hs.user.model.constant.SupportRequestHistoryAction;
import com.hs.user.model.constant.SupportRequestStatus;
import com.hs.user.repository.SupportRequestHistoryRepository;
import com.hs.user.repository.SupportRequestRepository;
import com.hs.user.repository.UserRepository;
import com.hs.user.repository.specification.SupportRequestSpecification;
import com.hs.user.service.NotificationService;
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

    private static final Map<SupportRequestStatus, Set<SupportRequestStatus>> ALLOWED_TRANSITIONS = Map.of(
            SupportRequestStatus.NEW, Set.of(SupportRequestStatus.RECEIVED),
            SupportRequestStatus.RECEIVED, Set.of(SupportRequestStatus.IN_PROGRESS),
            SupportRequestStatus.IN_PROGRESS, Set.of(SupportRequestStatus.WAITING_CUSTOMER, SupportRequestStatus.COMPLETED),
            SupportRequestStatus.WAITING_CUSTOMER, Set.of(SupportRequestStatus.IN_PROGRESS, SupportRequestStatus.COMPLETED),
            SupportRequestStatus.COMPLETED, Set.of(SupportRequestStatus.CLOSED),
            SupportRequestStatus.CLOSED, Collections.emptySet()
    );

    private static final Set<SupportRequestStatus> ALLOWED_ASSIGN_STATUSES = Set.of(
            SupportRequestStatus.RECEIVED,
            SupportRequestStatus.IN_PROGRESS,
            SupportRequestStatus.WAITING_CUSTOMER
    );

    private static final Set<SupportRequestStatus> ALLOWED_NOTE_STATUSES = Set.of(
            SupportRequestStatus.RECEIVED,
            SupportRequestStatus.IN_PROGRESS,
            SupportRequestStatus.WAITING_CUSTOMER,
            SupportRequestStatus.COMPLETED
    );

    SupportRequestRepository supportRequestRepository;
    SupportRequestHistoryRepository supportRequestHistoryRepository;
    UserRepository userRepository;
    NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public Page<SupportRequestAdminSummaryResponse> findAllAdminSupportRequests(SupportRequestAdminQuery query, Pageable pageable) {
        if (query != null && query.getFromDate() != null && query.getToDate() != null
                && query.getFromDate().isAfter(query.getToDate())) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        int pageSize = Math.min(Math.max(pageable.getPageSize(), 1), 100);
        int pageNumber = Math.max(pageable.getPageNumber(), 0);
        Sort sort = pageable.getSort().and(Sort.by(Sort.Direction.DESC, "id"));
        Pageable safePageable = PageRequest.of(pageNumber, pageSize, sort);

        Page<SupportRequest> requestsPage = supportRequestRepository.findAll(
                SupportRequestSpecification.filterAdminRequests(query),
                safePageable
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

        SupportRequestHistory history = createHistoryEntry(saved, oldStatus, SupportRequestStatus.RECEIVED, SupportRequestHistoryAction.STATUS_CHANGED, "Đã tiếp nhận ticket", actorId);

        if (notificationService != null) {
            String historyId = (history != null && history.getId() != null) ? history.getId() : java.util.UUID.randomUUID().toString();
            notificationService.createNotification(
                    saved.getCustomerId(),
                    NotificationType.SUPPORT_REQUEST_RECEIVED,
                    "Yêu cầu hỗ trợ đã được tiếp nhận",
                    "Yêu cầu hỗ trợ " + saved.getTicketCode() + " của bạn đã được tiếp nhận và đang chờ xử lý.",
                    "SUPPORT_REQUEST",
                    saved.getId(),
                    historyId
            );
        }

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

        if (!ALLOWED_ASSIGN_STATUSES.contains(req.getStatus())) {
            throw new AppException(ErrorCode.INVALID_SUPPORT_REQUEST_STATUS);
        }

        User staff = validateStaffEligibility(request.getAssignedTo());

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

        String staffDisplay = staff.getUsername() != null ? staff.getUsername() : staff.getId();
        String defaultNote = (oldStatus == SupportRequestStatus.RECEIVED)
                ? "Phân công xử lý sự cố cho nhân viên: " + staffDisplay
                : "Phân công lại sự cố cho nhân viên: " + staffDisplay;

        String note = (request.getNote() != null && !request.getNote().isBlank())
                ? request.getNote().trim()
                : defaultNote;

        SupportRequestHistory history = createHistoryEntry(saved, oldStatus, newStatus, SupportRequestHistoryAction.ASSIGNED, note, actorId);

        if (oldStatus != newStatus && notificationService != null) {
            String historyId = (history != null && history.getId() != null) ? history.getId() : java.util.UUID.randomUUID().toString();
            notificationService.createNotification(
                    saved.getCustomerId(),
                    NotificationType.SUPPORT_REQUEST_STATUS_CHANGED,
                    "Yêu cầu hỗ trợ đã cập nhật trạng thái",
                    "Yêu cầu hỗ trợ " + saved.getTicketCode() + " đã chuyển sang trạng thái " + newStatus + ".",
                    "SUPPORT_REQUEST",
                    saved.getId(),
                    historyId
            );
        }

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

        // Validate transition against centralized state matrix
        Set<SupportRequestStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(oldStatus, Collections.emptySet());
        if (!allowed.contains(targetStatus)) {
            if (oldStatus == SupportRequestStatus.NEW) {
                throw new AppException(ErrorCode.TICKET_MUST_BE_RECEIVED_FIRST);
            }
            throw new AppException(ErrorCode.INVALID_SUPPORT_REQUEST_STATUS);
        }

        String note = request.getNote() != null ? request.getNote().trim() : "";

        // Require note for WAITING_CUSTOMER
        if (targetStatus == SupportRequestStatus.WAITING_CUSTOMER && note.isBlank()) {
            throw new AppException(ErrorCode.SUPPORT_REQUEST_NOTE_REQUIRED);
        }

        // Require non-empty resolution for COMPLETED
        if (targetStatus == SupportRequestStatus.COMPLETED) {
            String resolution = request.getResolution() != null ? request.getResolution().trim() : "";
            if (resolution.isBlank()) {
                throw new AppException(ErrorCode.RESOLUTION_REQUIRED);
            }
            req.setResolution(resolution);
            req.setCompletedAt(Instant.now());
        } else if (targetStatus == SupportRequestStatus.CLOSED) {
            req.setClosedAt(Instant.now());
        }

        req.setStatus(targetStatus);
        SupportRequest saved = supportRequestRepository.save(req);

        SupportRequestHistory history = createHistoryEntry(saved, oldStatus, targetStatus, SupportRequestHistoryAction.STATUS_CHANGED, note.isBlank() ? "Cập nhật trạng thái sang " + targetStatus : note, actorId);

        if (notificationService != null) {
            String historyId = (history != null && history.getId() != null) ? history.getId() : java.util.UUID.randomUUID().toString();
            notificationService.createNotification(
                    saved.getCustomerId(),
                    NotificationType.SUPPORT_REQUEST_STATUS_CHANGED,
                    "Yêu cầu hỗ trợ đã cập nhật trạng thái",
                    "Yêu cầu hỗ trợ " + saved.getTicketCode() + " đã chuyển sang trạng thái " + targetStatus + ".",
                    "SUPPORT_REQUEST",
                    saved.getId(),
                    historyId
            );
        }

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

        if (!ALLOWED_NOTE_STATUSES.contains(req.getStatus())) {
            throw new AppException(ErrorCode.INVALID_SUPPORT_REQUEST_STATUS);
        }

        String note = request.getNote() != null ? request.getNote().trim() : "";
        if (note.isBlank()) {
            throw new AppException(ErrorCode.SUPPORT_REQUEST_NOTE_REQUIRED);
        }

        SupportRequestHistory history = createHistoryEntry(
                req,
                req.getStatus(),
                req.getStatus(),
                SupportRequestHistoryAction.NOTE_ADDED,
                note,
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

    @Override
    @Transactional(readOnly = true)
    public List<SupportRequestAdminAssigneeResponse> findEligibleAssignees(String keyword) {
        List<User> staffList = userRepository.findEligibleSupportAssignees(keyword);
        return staffList.stream()
                .map(SupportRequestMapper::mapToAdminAssigneeResponse)
                .toList();
    }

    private User validateStaffEligibility(String staffId) {
        User staff = userRepository.findByIdWithRoleAndPermissions(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        if (!Boolean.TRUE.equals(staff.getActive())) {
            throw new AppException(ErrorCode.STAFF_NOT_ELIGIBLE);
        }

        Role role = staff.getRole();
        if (role == null || !Boolean.TRUE.equals(role.getActive())) {
            throw new AppException(ErrorCode.STAFF_NOT_ELIGIBLE);
        }

        boolean isAdmin = RoleConstants.ADMIN.equalsIgnoreCase(role.getName())
                || "ROLE_ADMIN".equalsIgnoreCase(role.getName());

        boolean hasProcessPermission = role.getPermissions() != null && role.getPermissions().stream()
                .anyMatch(p -> Boolean.TRUE.equals(p.getActive())
                        && PermissionConstants.Admin.SUPPORT_REQUEST_PROCESS.equals(p.getName()));

        if (!isAdmin && !hasProcessPermission) {
            throw new AppException(ErrorCode.STAFF_NOT_ELIGIBLE);
        }

        return staff;
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

        SupportRequestHistory saved = supportRequestHistoryRepository.save(history);
        return saved != null ? saved : history;
    }

    private UserResponse resolveUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        return userRepository.findById(userId)
                .map(UserMapper::mapToUserResponse)
                .orElse(null);
    }
}
