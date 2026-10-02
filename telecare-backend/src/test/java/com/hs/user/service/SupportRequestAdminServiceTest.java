package com.hs.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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
import com.hs.user.model.Permission;
import com.hs.user.model.Role;
import com.hs.user.model.SupportCategory;
import com.hs.user.model.SupportRequest;
import com.hs.user.model.SupportRequestHistory;
import com.hs.user.model.User;
import com.hs.user.model.constant.SupportRequestHistoryAction;
import com.hs.user.model.constant.SupportRequestStatus;
import com.hs.user.repository.SupportRequestHistoryRepository;
import com.hs.user.repository.SupportRequestRepository;
import com.hs.user.repository.UserRepository;
import com.hs.user.service.impl.SupportRequestAdminServiceImpl;

@ExtendWith(MockitoExtension.class)
class SupportRequestAdminServiceTest {

    @Mock
    private SupportRequestRepository supportRequestRepository;

    @Mock
    private SupportRequestHistoryRepository supportRequestHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SupportRequestAdminServiceImpl supportRequestAdminService;

    private SupportRequest ticketNew;
    private SupportRequest ticketReceived;
    private SupportRequest ticketInProgress;
    private SupportRequest ticketWaitingCustomer;
    private SupportRequest ticketCompleted;
    private SupportRequest ticketClosed;
    private User staffUser;

    @BeforeEach
    void setUp() {
        SupportCategory category = SupportCategory.builder()
                .id("cat-1")
                .code("CONNECTION")
                .name("Kết nối")
                .build();

        Permission processPerm = Permission.builder()
                .id("perm-proc")
                .name(PermissionConstants.Admin.SUPPORT_REQUEST_PROCESS)
                .build();
        processPerm.setActive(true);

        Role staffRole = Role.builder()
                .id("role-staff")
                .name("STAFF")
                .permissions(Set.of(processPerm))
                .build();
        staffRole.setActive(true);

        staffUser = new User();
        staffUser.setId("staff-123");
        staffUser.setUsername("staff1");
        staffUser.setFirstName("Staff");
        staffUser.setLastName("Member");
        staffUser.setEmail("staff1@telecare.com");
        staffUser.setActive(true);
        staffUser.setRole(staffRole);

        ticketNew = SupportRequest.builder()
                .id("sr-new-id")
                .ticketCode("SR-20260910-00001")
                .customerId("cust-1")
                .category(category)
                .subject("Kiểm tra đường truyền Internet")
                .content("Chi tiết thông tin nội dung sự cố mạng...")
                .status(SupportRequestStatus.NEW)
                .build();

        ticketReceived = SupportRequest.builder()
                .id("sr-received-id")
                .ticketCode("SR-20260910-00002")
                .customerId("cust-1")
                .category(category)
                .subject("Kiểm tra đường truyền Internet")
                .content("Chi tiết thông tin nội dung sự cố mạng...")
                .status(SupportRequestStatus.RECEIVED)
                .build();

        ticketInProgress = SupportRequest.builder()
                .id("sr-inprogress-id")
                .ticketCode("SR-20260910-00003")
                .customerId("cust-1")
                .category(category)
                .subject("Kiểm tra đường truyền Internet")
                .content("Chi tiết thông tin nội dung sự cố mạng...")
                .status(SupportRequestStatus.IN_PROGRESS)
                .assignedTo(staffUser)
                .build();

        ticketWaitingCustomer = SupportRequest.builder()
                .id("sr-waiting-id")
                .ticketCode("SR-20260910-00004")
                .customerId("cust-1")
                .category(category)
                .subject("Kiểm tra đường truyền Internet")
                .content("Chi tiết thông tin nội dung sự cố mạng...")
                .status(SupportRequestStatus.WAITING_CUSTOMER)
                .assignedTo(staffUser)
                .build();

        ticketCompleted = SupportRequest.builder()
                .id("sr-completed-id")
                .ticketCode("SR-20260910-00005")
                .customerId("cust-1")
                .category(category)
                .subject("Kiểm tra đường truyền Internet")
                .content("Chi tiết thông tin nội dung sự cố mạng...")
                .status(SupportRequestStatus.COMPLETED)
                .assignedTo(staffUser)
                .resolution("Đã sửa cáp quang thành công")
                .build();

        ticketClosed = SupportRequest.builder()
                .id("sr-closed-id")
                .ticketCode("SR-20260910-00006")
                .customerId("cust-1")
                .category(category)
                .subject("Kiểm tra đường truyền Internet")
                .content("Chi tiết thông tin nội dung sự cố mạng...")
                .status(SupportRequestStatus.CLOSED)
                .build();
    }

    // ==========================================
    // 1. findAllAdminSupportRequests
    // ==========================================
    @Test
    @DisplayName("1. Lấy danh sách ticket quản trị có phân trang & bộ lọc LocalDate hợp lệ")
    void findAllAdminSupportRequests_Success() {
        SupportRequestAdminQuery query = SupportRequestAdminQuery.builder()
                .status(SupportRequestStatus.NEW)
                .categoryCode("CONNECTION")
                .fromDate(LocalDate.of(2026, 9, 1))
                .toDate(LocalDate.of(2026, 10, 31))
                .build();

        Page<SupportRequest> page = new PageImpl<>(List.of(ticketNew), PageRequest.of(0, 10), 1);
        when(supportRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<SupportRequestAdminSummaryResponse> result = supportRequestAdminService.findAllAdminSupportRequests(query, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTicketCode()).isEqualTo("SR-20260910-00001");
    }

    @Test
    @DisplayName("2. Lấy danh sách thất bại khi fromDate sau toDate")
    void findAllAdminSupportRequests_FromDateAfterToDate_ThrowsException() {
        SupportRequestAdminQuery query = SupportRequestAdminQuery.builder()
                .fromDate(LocalDate.of(2026, 10, 31))
                .toDate(LocalDate.of(2026, 9, 1))
                .build();

        assertThatThrownBy(() -> supportRequestAdminService.findAllAdminSupportRequests(query, PageRequest.of(0, 10)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_DATE_RANGE);
    }

    // ==========================================
    // 2. receiveSupportRequest
    // ==========================================
    @Test
    @DisplayName("3. Tiếp nhận ticket: NEW -> RECEIVED thành công, ghi receivedAt và history")
    void receiveSupportRequest_Success() {
        when(supportRequestRepository.findById("sr-new-id")).thenReturn(Optional.of(ticketNew));
        when(supportRequestRepository.save(any(SupportRequest.class))).thenAnswer(i -> i.getArgument(0));

        SupportRequestAdminDetailResponse response = supportRequestAdminService.receiveSupportRequest("sr-new-id", "actor-admin-1");

        assertThat(response).isNotNull();
        assertThat(ticketNew.getStatus()).isEqualTo(SupportRequestStatus.RECEIVED);
        assertThat(ticketNew.getReceivedAt()).isNotNull();

        verify(supportRequestHistoryRepository).save(any(SupportRequestHistory.class));
    }

    @Test
    @DisplayName("4. Tiếp nhận ticket không phải NEW thất bại")
    void receiveSupportRequest_NotNew_ThrowsException() {
        when(supportRequestRepository.findById("sr-received-id")).thenReturn(Optional.of(ticketReceived));

        assertThatThrownBy(() -> supportRequestAdminService.receiveSupportRequest("sr-received-id", "actor-admin-1"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SUPPORT_REQUEST_STATUS);
    }

    // ==========================================
    // 3. assignSupportRequest
    // ==========================================
    @Test
    @DisplayName("5. Phân công ticket từ RECEIVED -> chuyển sang IN_PROGRESS và ghi assignedAt, history ASSIGNED")
    void assignSupportRequest_FromReceived_Success() {
        AssignSupportRequestRequest request = AssignSupportRequestRequest.builder()
                .assignedTo("staff-123")
                .note("Phân công nhân viên 1")
                .build();

        when(supportRequestRepository.findById("sr-received-id")).thenReturn(Optional.of(ticketReceived));
        when(userRepository.findByIdWithRoleAndPermissions("staff-123")).thenReturn(Optional.of(staffUser));
        when(supportRequestRepository.save(any(SupportRequest.class))).thenAnswer(i -> i.getArgument(0));

        SupportRequestAdminDetailResponse response = supportRequestAdminService.assignSupportRequest("sr-received-id", request, "actor-admin-1");

        assertThat(response).isNotNull();
        assertThat(ticketReceived.getAssignedTo()).isEqualTo(staffUser);
        assertThat(ticketReceived.getStatus()).isEqualTo(SupportRequestStatus.IN_PROGRESS);
        assertThat(ticketReceived.getAssignedAt()).isNotNull();

        ArgumentCaptor<SupportRequestHistory> historyCaptor = ArgumentCaptor.forClass(SupportRequestHistory.class);
        verify(supportRequestHistoryRepository).save(historyCaptor.capture());
        SupportRequestHistory captured = historyCaptor.getValue();
        assertThat(captured.getAction()).isEqualTo(SupportRequestHistoryAction.ASSIGNED);
        assertThat(captured.getFromStatus()).isEqualTo(SupportRequestStatus.RECEIVED);
        assertThat(captured.getToStatus()).isEqualTo(SupportRequestStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("6. Phân công lại khi ticket đang IN_PROGRESS: giữ nguyên IN_PROGRESS và ghi history phân công lại")
    void assignSupportRequest_FromInProgress_ReassignSuccess() {
        AssignSupportRequestRequest request = AssignSupportRequestRequest.builder()
                .assignedTo("staff-123")
                .note("Chuyển cho nhân viên khác")
                .build();

        when(supportRequestRepository.findById("sr-inprogress-id")).thenReturn(Optional.of(ticketInProgress));
        when(userRepository.findByIdWithRoleAndPermissions("staff-123")).thenReturn(Optional.of(staffUser));
        when(supportRequestRepository.save(any(SupportRequest.class))).thenAnswer(i -> i.getArgument(0));

        supportRequestAdminService.assignSupportRequest("sr-inprogress-id", request, "actor-admin-1");

        assertThat(ticketInProgress.getStatus()).isEqualTo(SupportRequestStatus.IN_PROGRESS);

        ArgumentCaptor<SupportRequestHistory> historyCaptor = ArgumentCaptor.forClass(SupportRequestHistory.class);
        verify(supportRequestHistoryRepository).save(historyCaptor.capture());
        SupportRequestHistory captured = historyCaptor.getValue();
        assertThat(captured.getAction()).isEqualTo(SupportRequestHistoryAction.ASSIGNED);
        assertThat(captured.getFromStatus()).isEqualTo(SupportRequestStatus.IN_PROGRESS);
        assertThat(captured.getToStatus()).isEqualTo(SupportRequestStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("7. Phân công lại khi ticket đang WAITING_CUSTOMER: giữ nguyên WAITING_CUSTOMER")
    void assignSupportRequest_FromWaitingCustomer_ReassignSuccess() {
        AssignSupportRequestRequest request = AssignSupportRequestRequest.builder()
                .assignedTo("staff-123")
                .build();

        when(supportRequestRepository.findById("sr-waiting-id")).thenReturn(Optional.of(ticketWaitingCustomer));
        when(userRepository.findByIdWithRoleAndPermissions("staff-123")).thenReturn(Optional.of(staffUser));
        when(supportRequestRepository.save(any(SupportRequest.class))).thenAnswer(i -> i.getArgument(0));

        supportRequestAdminService.assignSupportRequest("sr-waiting-id", request, "actor-admin-1");

        assertThat(ticketWaitingCustomer.getStatus()).isEqualTo(SupportRequestStatus.WAITING_CUSTOMER);
    }

    @Test
    @DisplayName("8. Phân công ticket chưa tiếp nhận (NEW) thất bại")
    void assignSupportRequest_TicketNew_ThrowsException() {
        AssignSupportRequestRequest request = AssignSupportRequestRequest.builder()
                .assignedTo("staff-123")
                .build();

        when(supportRequestRepository.findById("sr-new-id")).thenReturn(Optional.of(ticketNew));

        assertThatThrownBy(() -> supportRequestAdminService.assignSupportRequest("sr-new-id", request, "actor-admin-1"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TICKET_MUST_BE_RECEIVED_FIRST);
    }

    @Test
    @DisplayName("9. Phân công ticket COMPLETED thất bại (F2)")
    void assignSupportRequest_TicketCompleted_ThrowsException() {
        AssignSupportRequestRequest request = AssignSupportRequestRequest.builder()
                .assignedTo("staff-123")
                .build();

        when(supportRequestRepository.findById("sr-completed-id")).thenReturn(Optional.of(ticketCompleted));

        assertThatThrownBy(() -> supportRequestAdminService.assignSupportRequest("sr-completed-id", request, "actor-admin-1"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SUPPORT_REQUEST_STATUS);
    }

    @Test
    @DisplayName("10. Phân công ticket CLOSED thất bại")
    void assignSupportRequest_TicketClosed_ThrowsException() {
        AssignSupportRequestRequest request = AssignSupportRequestRequest.builder()
                .assignedTo("staff-123")
                .build();

        when(supportRequestRepository.findById("sr-closed-id")).thenReturn(Optional.of(ticketClosed));

        assertThatThrownBy(() -> supportRequestAdminService.assignSupportRequest("sr-closed-id", request, "actor-admin-1"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CLOSED_TICKET_CANNOT_BE_UPDATED);
    }

    // ==========================================
    // 4. Staff Eligibility (F3)
    // ==========================================
    @Test
    @DisplayName("11. Phân công nhân viên không tồn tại thất bại")
    void assignSupportRequest_StaffNotFound_ThrowsException() {
        AssignSupportRequestRequest request = AssignSupportRequestRequest.builder()
                .assignedTo("non-existent-user")
                .build();

        when(supportRequestRepository.findById("sr-received-id")).thenReturn(Optional.of(ticketReceived));
        when(userRepository.findByIdWithRoleAndPermissions("non-existent-user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supportRequestAdminService.assignSupportRequest("sr-received-id", request, "actor-admin-1"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_NOT_FOUND);
    }

    @Test
    @DisplayName("12. Phân công nhân viên inactive thất bại")
    void assignSupportRequest_StaffInactive_ThrowsException() {
        staffUser.setActive(false);
        AssignSupportRequestRequest request = AssignSupportRequestRequest.builder()
                .assignedTo("staff-123")
                .build();

        when(supportRequestRepository.findById("sr-received-id")).thenReturn(Optional.of(ticketReceived));
        when(userRepository.findByIdWithRoleAndPermissions("staff-123")).thenReturn(Optional.of(staffUser));

        assertThatThrownBy(() -> supportRequestAdminService.assignSupportRequest("sr-received-id", request, "actor-admin-1"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_NOT_ELIGIBLE);
    }

    @Test
    @DisplayName("13. Phân công nhân viên có role USER thất bại")
    void assignSupportRequest_StaffRoleUser_ThrowsException() {
        Role userRole = Role.builder().id("role-user").name(RoleConstants.USER).build();
        userRole.setActive(true);
        staffUser.setRole(userRole);

        AssignSupportRequestRequest request = AssignSupportRequestRequest.builder()
                .assignedTo("staff-123")
                .build();

        when(supportRequestRepository.findById("sr-received-id")).thenReturn(Optional.of(ticketReceived));
        when(userRepository.findByIdWithRoleAndPermissions("staff-123")).thenReturn(Optional.of(staffUser));

        assertThatThrownBy(() -> supportRequestAdminService.assignSupportRequest("sr-received-id", request, "actor-admin-1"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_NOT_ELIGIBLE);
    }

    @Test
    @DisplayName("14. Phân công nhân viên có role không có quyền SUPPORT_REQUEST_PROCESS thất bại")
    void assignSupportRequest_RoleWithoutProcessPermission_ThrowsException() {
        Role customRole = Role.builder().id("role-custom").name("MARKETING").permissions(Set.of()).build();
        customRole.setActive(true);
        staffUser.setRole(customRole);

        AssignSupportRequestRequest request = AssignSupportRequestRequest.builder()
                .assignedTo("staff-123")
                .build();

        when(supportRequestRepository.findById("sr-received-id")).thenReturn(Optional.of(ticketReceived));
        when(userRepository.findByIdWithRoleAndPermissions("staff-123")).thenReturn(Optional.of(staffUser));

        assertThatThrownBy(() -> supportRequestAdminService.assignSupportRequest("sr-received-id", request, "actor-admin-1"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_NOT_ELIGIBLE);
    }

    @Test
    @DisplayName("15. Phân công nhân viên có role ADMIN thành công")
    void assignSupportRequest_AdminRole_Success() {
        Role adminRole = Role.builder().id("role-admin").name(RoleConstants.ADMIN).build();
        adminRole.setActive(true);
        staffUser.setRole(adminRole);

        AssignSupportRequestRequest request = AssignSupportRequestRequest.builder()
                .assignedTo("staff-123")
                .build();

        when(supportRequestRepository.findById("sr-received-id")).thenReturn(Optional.of(ticketReceived));
        when(userRepository.findByIdWithRoleAndPermissions("staff-123")).thenReturn(Optional.of(staffUser));
        when(supportRequestRepository.save(any(SupportRequest.class))).thenAnswer(i -> i.getArgument(0));

        SupportRequestAdminDetailResponse response = supportRequestAdminService.assignSupportRequest("sr-received-id", request, "actor-admin-1");

        assertThat(response).isNotNull();
        assertThat(ticketReceived.getAssignedTo()).isEqualTo(staffUser);
    }

    @Test
    @DisplayName("16. Phân công nhân viên có quyền nhưng quyền inactive thất bại")
    void assignSupportRequest_PermissionInactive_ThrowsException() {
        Permission inactivePerm = Permission.builder()
                .id("perm-proc")
                .name(PermissionConstants.Admin.SUPPORT_REQUEST_PROCESS)
                .build();
        inactivePerm.setActive(false);

        Role staffRole = Role.builder()
                .id("role-staff")
                .name("STAFF")
                .permissions(Set.of(inactivePerm))
                .build();
        staffRole.setActive(true);
        staffUser.setRole(staffRole);

        AssignSupportRequestRequest request = AssignSupportRequestRequest.builder()
                .assignedTo("staff-123")
                .build();

        when(supportRequestRepository.findById("sr-received-id")).thenReturn(Optional.of(ticketReceived));
        when(userRepository.findByIdWithRoleAndPermissions("staff-123")).thenReturn(Optional.of(staffUser));

        assertThatThrownBy(() -> supportRequestAdminService.assignSupportRequest("sr-received-id", request, "actor-admin-1"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_NOT_ELIGIBLE);
    }

    // ==========================================
    // 5. updateSupportRequestStatus
    // ==========================================
    @Test
    @DisplayName("17. Chuyển trạng thái: RECEIVED -> IN_PROGRESS thành công")
    void updateStatus_ReceivedToInProgress_Success() {
        UpdateSupportRequestStatusRequest request = UpdateSupportRequestStatusRequest.builder()
                .status(SupportRequestStatus.IN_PROGRESS)
                .note("Bắt đầu xử lý")
                .build();

        when(supportRequestRepository.findById("sr-received-id")).thenReturn(Optional.of(ticketReceived));
        when(supportRequestRepository.save(any(SupportRequest.class))).thenAnswer(i -> i.getArgument(0));

        supportRequestAdminService.updateSupportRequestStatus("sr-received-id", request, "actor-1");

        assertThat(ticketReceived.getStatus()).isEqualTo(SupportRequestStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("18. Chuyển sang WAITING_CUSTOMER thiếu note ném ngoại lệ")
    void updateStatus_WaitingCustomerMissingNote_ThrowsException() {
        UpdateSupportRequestStatusRequest request = UpdateSupportRequestStatusRequest.builder()
                .status(SupportRequestStatus.WAITING_CUSTOMER)
                .note("   ")
                .build();

        when(supportRequestRepository.findById("sr-inprogress-id")).thenReturn(Optional.of(ticketInProgress));

        assertThatThrownBy(() -> supportRequestAdminService.updateSupportRequestStatus("sr-inprogress-id", request, "actor-1"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SUPPORT_REQUEST_NOTE_REQUIRED);
    }

    @Test
    @DisplayName("19. Chuyển sang COMPLETED thiếu resolution ném ngoại lệ")
    void updateStatus_CompletedMissingResolution_ThrowsException() {
        UpdateSupportRequestStatusRequest request = UpdateSupportRequestStatusRequest.builder()
                .status(SupportRequestStatus.COMPLETED)
                .note("Xong")
                .resolution("")
                .build();

        when(supportRequestRepository.findById("sr-inprogress-id")).thenReturn(Optional.of(ticketInProgress));

        assertThatThrownBy(() -> supportRequestAdminService.updateSupportRequestStatus("sr-inprogress-id", request, "actor-1"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOLUTION_REQUIRED);
    }

    @Test
    @DisplayName("20. Chuyển sang COMPLETED có resolution thành công và ghi completedAt")
    void updateStatus_CompletedWithResolution_Success() {
        UpdateSupportRequestStatusRequest request = UpdateSupportRequestStatusRequest.builder()
                .status(SupportRequestStatus.COMPLETED)
                .note("Hoàn thành xử lý sự cố")
                .resolution("Thay dây cáp quang mới")
                .build();

        when(supportRequestRepository.findById("sr-inprogress-id")).thenReturn(Optional.of(ticketInProgress));
        when(supportRequestRepository.save(any(SupportRequest.class))).thenAnswer(i -> i.getArgument(0));

        supportRequestAdminService.updateSupportRequestStatus("sr-inprogress-id", request, "actor-1");

        assertThat(ticketInProgress.getStatus()).isEqualTo(SupportRequestStatus.COMPLETED);
        assertThat(ticketInProgress.getResolution()).isEqualTo("Thay dây cáp quang mới");
        assertThat(ticketInProgress.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("21. Chuyển sang CLOSED ghi closedAt thành công")
    void updateStatus_CompletedToClosed_Success() {
        UpdateSupportRequestStatusRequest request = UpdateSupportRequestStatusRequest.builder()
                .status(SupportRequestStatus.CLOSED)
                .note("Đóng ticket")
                .build();

        when(supportRequestRepository.findById("sr-completed-id")).thenReturn(Optional.of(ticketCompleted));
        when(supportRequestRepository.save(any(SupportRequest.class))).thenAnswer(i -> i.getArgument(0));

        supportRequestAdminService.updateSupportRequestStatus("sr-completed-id", request, "actor-1");

        assertThat(ticketCompleted.getStatus()).isEqualTo(SupportRequestStatus.CLOSED);
        assertThat(ticketCompleted.getClosedAt()).isNotNull();
    }

    @Test
    @DisplayName("22. Cập nhật ticket đã CLOSED thất bại")
    void updateStatus_ClosedTicket_ThrowsException() {
        UpdateSupportRequestStatusRequest request = UpdateSupportRequestStatusRequest.builder()
                .status(SupportRequestStatus.IN_PROGRESS)
                .build();

        when(supportRequestRepository.findById("sr-closed-id")).thenReturn(Optional.of(ticketClosed));

        assertThatThrownBy(() -> supportRequestAdminService.updateSupportRequestStatus("sr-closed-id", request, "actor-1"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CLOSED_TICKET_CANNOT_BE_UPDATED);
    }

    // ==========================================
    // 6. addSupportRequestHistoryNote (F5)
    // ==========================================
    @Test
    @DisplayName("23. Thêm ghi chú xử lý khi ticket NEW thất bại (F5)")
    void addHistoryNote_FromNew_ThrowsException() {
        AddSupportRequestHistoryRequest request = AddSupportRequestHistoryRequest.builder()
                .note("Ghi chú cho NEW")
                .build();

        when(supportRequestRepository.findById("sr-new-id")).thenReturn(Optional.of(ticketNew));

        assertThatThrownBy(() -> supportRequestAdminService.addSupportRequestHistoryNote("sr-new-id", request, "actor-1"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SUPPORT_REQUEST_STATUS);
    }

    @Test
    @DisplayName("24. Thêm ghi chú xử lý khi ticket CLOSED thất bại")
    void addHistoryNote_FromClosed_ThrowsException() {
        AddSupportRequestHistoryRequest request = AddSupportRequestHistoryRequest.builder()
                .note("Ghi chú cho CLOSED")
                .build();

        when(supportRequestRepository.findById("sr-closed-id")).thenReturn(Optional.of(ticketClosed));

        assertThatThrownBy(() -> supportRequestAdminService.addSupportRequestHistoryNote("sr-closed-id", request, "actor-1"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CLOSED_TICKET_CANNOT_BE_UPDATED);
    }

    @Test
    @DisplayName("25. Thêm ghi chú xử lý khi ticket IN_PROGRESS thành công (F5)")
    void addHistoryNote_FromInProgress_Success() {
        AddSupportRequestHistoryRequest request = AddSupportRequestHistoryRequest.builder()
                .note("Đã liên hệ khách hàng")
                .build();

        when(supportRequestRepository.findById("sr-inprogress-id")).thenReturn(Optional.of(ticketInProgress));
        when(supportRequestHistoryRepository.save(any(SupportRequestHistory.class))).thenAnswer(i -> {
            SupportRequestHistory h = i.getArgument(0);
            h.setId("hist-1");
            return h;
        });

        SupportRequestHistoryResponse response = supportRequestAdminService.addSupportRequestHistoryNote("sr-inprogress-id", request, "actor-1");

        assertThat(response).isNotNull();
        assertThat(response.getNote()).isEqualTo("Đã liên hệ khách hàng");
    }

    @Test
    @DisplayName("26. Thêm ghi chú xử lý rỗng thất bại")
    void addHistoryNote_BlankNote_ThrowsException() {
        AddSupportRequestHistoryRequest request = AddSupportRequestHistoryRequest.builder()
                .note("   ")
                .build();

        when(supportRequestRepository.findById("sr-inprogress-id")).thenReturn(Optional.of(ticketInProgress));

        assertThatThrownBy(() -> supportRequestAdminService.addSupportRequestHistoryNote("sr-inprogress-id", request, "actor-1"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SUPPORT_REQUEST_NOTE_REQUIRED);
    }

    // ==========================================
    // 7. findEligibleAssignees (F4)
    // ==========================================
    @Test
    @DisplayName("27. Lấy danh sách nhân viên đủ điều kiện phân công thành công (F4)")
    void findEligibleAssignees_Success() {
        when(userRepository.findEligibleSupportAssignees("staff")).thenReturn(List.of(staffUser));

        List<SupportRequestAdminAssigneeResponse> assignees = supportRequestAdminService.findEligibleAssignees("staff");

        assertThat(assignees).hasSize(1);
        assertThat(assignees.get(0).getId()).isEqualTo("staff-123");
        assertThat(assignees.get(0).getUsername()).isEqualTo("staff1");
        assertThat(assignees.get(0).getFullName()).isEqualTo("Staff Member");
        assertThat(assignees.get(0).getRoleName()).isEqualTo("STAFF");
    }
}
