package com.hs.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

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

import com.hs.user.advice.base.AppException;
import com.hs.user.constant.base.ErrorCode;
import com.hs.user.dto.request.AddSupportRequestHistoryRequest;
import com.hs.user.dto.request.AssignSupportRequestRequest;
import com.hs.user.dto.request.SupportRequestAdminQuery;
import com.hs.user.dto.request.UpdateSupportRequestStatusRequest;
import com.hs.user.dto.response.SupportRequestAdminDetailResponse;
import com.hs.user.dto.response.SupportRequestAdminSummaryResponse;
import com.hs.user.dto.response.SupportRequestHistoryResponse;
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
    private SupportRequest ticketClosed;
    private User staffUser;

    @BeforeEach
    void setUp() {
        SupportCategory category = SupportCategory.builder()
                .id("cat-1")
                .code("CONNECTION")
                .name("Kết nối")
                .build();

        staffUser = new User();
        staffUser.setId("staff-123");
        staffUser.setUsername("staff1");
        staffUser.setFirstName("Staff");
        staffUser.setLastName("Member");

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

        ticketClosed = SupportRequest.builder()
                .id("sr-closed-id")
                .ticketCode("SR-20260910-00004")
                .customerId("cust-1")
                .category(category)
                .subject("Kiểm tra đường truyền Internet")
                .content("Chi tiết thông tin nội dung sự cố mạng...")
                .status(SupportRequestStatus.CLOSED)
                .build();
    }

    @Test
    @DisplayName("1. Lấy danh sách ticket quản trị có phân trang & bộ lọc")
    void findAllAdminSupportRequests_Success() {
        SupportRequestAdminQuery query = SupportRequestAdminQuery.builder()
                .keyword("SR-20260910")
                .status(SupportRequestStatus.NEW)
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        when(supportRequestRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ticketNew)));

        Page<SupportRequestAdminSummaryResponse> page = supportRequestAdminService.findAllAdminSupportRequests(query, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTicketCode()).isEqualTo("SR-20260910-00001");
    }

    @Test
    @DisplayName("2. Tiếp nhận Ticket (NEW -> RECEIVED)")
    void receiveSupportRequest_Success() {
        when(supportRequestRepository.findById("sr-new-id"))
                .thenReturn(Optional.of(ticketNew));
        when(supportRequestRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        SupportRequestAdminDetailResponse response = supportRequestAdminService.receiveSupportRequest("sr-new-id", "admin-1");

        assertThat(response).isNotNull();
        assertThat(ticketNew.getStatus()).isEqualTo(SupportRequestStatus.RECEIVED);
        assertThat(ticketNew.getReceivedAt()).isNotNull();

        verify(supportRequestHistoryRepository).save(any(SupportRequestHistory.class));
    }

    @Test
    @DisplayName("3. Tiếp nhận Ticket thất bại nếu trạng thái không phải NEW")
    void receiveSupportRequest_NotNew_ThrowsException() {
        when(supportRequestRepository.findById("sr-received-id"))
                .thenReturn(Optional.of(ticketReceived));

        assertThatThrownBy(() -> supportRequestAdminService.receiveSupportRequest("sr-received-id", "admin-1"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_SUPPORT_REQUEST_STATUS);
    }

    @Test
    @DisplayName("4. Phân công nhân viên (RECEIVED -> IN_PROGRESS)")
    void assignSupportRequest_Success() {
        AssignSupportRequestRequest request = AssignSupportRequestRequest.builder()
                .assignedTo("staff-123")
                .note("Phân công nhân viên kỹ thuật 1")
                .build();

        when(supportRequestRepository.findById("sr-received-id"))
                .thenReturn(Optional.of(ticketReceived));
        when(userRepository.findById("staff-123"))
                .thenReturn(Optional.of(staffUser));
        when(supportRequestRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        SupportRequestAdminDetailResponse response = supportRequestAdminService.assignSupportRequest("sr-received-id", request, "admin-1");

        assertThat(response).isNotNull();
        assertThat(ticketReceived.getAssignedTo()).isEqualTo(staffUser);
        assertThat(ticketReceived.getStatus()).isEqualTo(SupportRequestStatus.IN_PROGRESS);
        assertThat(ticketReceived.getAssignedAt()).isNotNull();
    }

    @Test
    @DisplayName("5. Phân công Ticket thất bại nếu Ticket chưa tiếp nhận (NEW)")
    void assignSupportRequest_TicketNew_ThrowsException() {
        AssignSupportRequestRequest request = AssignSupportRequestRequest.builder()
                .assignedTo("staff-123")
                .build();

        when(supportRequestRepository.findById("sr-new-id"))
                .thenReturn(Optional.of(ticketNew));

        assertThatThrownBy(() -> supportRequestAdminService.assignSupportRequest("sr-new-id", request, "admin-1"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.TICKET_MUST_BE_RECEIVED_FIRST);
    }

    @Test
    @DisplayName("6. Cập nhật sang WAITING_CUSTOMER bắt buộc phải có ghi chú")
    void updateSupportRequestStatus_WaitingCustomerWithoutNote_ThrowsException() {
        UpdateSupportRequestStatusRequest request = UpdateSupportRequestStatusRequest.builder()
                .status(SupportRequestStatus.WAITING_CUSTOMER)
                .note("") // Blank note
                .build();

        when(supportRequestRepository.findById("sr-inprogress-id"))
                .thenReturn(Optional.of(ticketInProgress));

        assertThatThrownBy(() -> supportRequestAdminService.updateSupportRequestStatus("sr-inprogress-id", request, "staff-123"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.SUPPORT_REQUEST_NOTE_REQUIRED);
    }

    @Test
    @DisplayName("7. Cập nhật sang COMPLETED thành công và ghi nhận resolution")
    void updateSupportRequestStatus_Completed_Success() {
        UpdateSupportRequestStatusRequest request = UpdateSupportRequestStatusRequest.builder()
                .status(SupportRequestStatus.COMPLETED)
                .note("Đã xử lý xong cáp quang mạng cho khách hàng")
                .build();

        when(supportRequestRepository.findById("sr-inprogress-id"))
                .thenReturn(Optional.of(ticketInProgress));
        when(supportRequestRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        SupportRequestAdminDetailResponse response = supportRequestAdminService.updateSupportRequestStatus("sr-inprogress-id", request, "staff-123");

        assertThat(response).isNotNull();
        assertThat(ticketInProgress.getStatus()).isEqualTo(SupportRequestStatus.COMPLETED);
        assertThat(ticketInProgress.getResolution()).isEqualTo("Đã xử lý xong cáp quang mạng cho khách hàng");
        assertThat(ticketInProgress.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("8. Không được cập nhật Ticket đã CLOSED")
    void updateSupportRequestStatus_ClosedTicket_ThrowsException() {
        UpdateSupportRequestStatusRequest request = UpdateSupportRequestStatusRequest.builder()
                .status(SupportRequestStatus.IN_PROGRESS)
                .note("Cố gắng cập nhật ticket đã đóng")
                .build();

        when(supportRequestRepository.findById("sr-closed-id"))
                .thenReturn(Optional.of(ticketClosed));

        assertThatThrownBy(() -> supportRequestAdminService.updateSupportRequestStatus("sr-closed-id", request, "staff-123"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CLOSED_TICKET_CANNOT_BE_UPDATED);
    }

    @Test
    @DisplayName("9. Thêm ghi chú xử lý (NOTE_ADDED) vào lịch sử")
    void addSupportRequestHistoryNote_Success() {
        AddSupportRequestHistoryRequest request = AddSupportRequestHistoryRequest.builder()
                .note("Ghi chú bổ sung thông tin từ kỹ thuật viên")
                .build();

        when(supportRequestRepository.findById("sr-inprogress-id"))
                .thenReturn(Optional.of(ticketInProgress));
        when(supportRequestHistoryRepository.save(any()))
                .thenAnswer(inv -> {
                    SupportRequestHistory h = inv.getArgument(0);
                    h.setId("hist-123");
                    return h;
                });

        SupportRequestHistoryResponse response = supportRequestAdminService.addSupportRequestHistoryNote("sr-inprogress-id", request, "staff-123");

        assertThat(response).isNotNull();
        assertThat(response.getNote()).isEqualTo("Ghi chú bổ sung thông tin từ kỹ thuật viên");
        assertThat(response.getAction()).isEqualTo(SupportRequestHistoryAction.NOTE_ADDED);
    }
}
