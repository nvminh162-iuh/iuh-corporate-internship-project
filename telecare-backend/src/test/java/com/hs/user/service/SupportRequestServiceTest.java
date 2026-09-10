package com.hs.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hs.user.advice.base.AppException;
import com.hs.user.constant.base.ErrorCode;
import com.hs.user.dto.request.CreateSupportRequestRequest;
import com.hs.user.dto.response.SupportCategoryResponse;
import com.hs.user.dto.response.SupportRequestResponse;
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
import com.hs.user.service.impl.SupportRequestServiceImpl;

@ExtendWith(MockitoExtension.class)
class SupportRequestServiceTest {

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

    private SupportCategory categoryConnection;
    private ServicePlan servicePlanMax100;

    @BeforeEach
    void setUp() {
        categoryConnection = SupportCategory.builder()
                .id("cat-conn-id")
                .code("CONNECTION")
                .name("Kết nối Internet")
                .description("Sự cố kết nối mạng")
                .displayOrder(1)
                .build();
        categoryConnection.setActive(true);

        servicePlanMax100 = ServicePlan.builder()
                .id("plan-max100-id")
                .code("MAX100")
                .name("Gói cước MAX100")
                .build();
        servicePlanMax100.setActive(true);
    }

    @Test
    @DisplayName("1. Lấy danh sách nhóm vấn đề đang hoạt động")
    void findAllActiveSupportCategories_ReturnsList() {
        when(supportCategoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(List.of(categoryConnection));

        List<SupportCategoryResponse> categories = supportRequestService.findAllActiveSupportCategories();

        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).getCode()).isEqualTo("CONNECTION");
    }

    @Test
    @DisplayName("2, 3, 4, 5, 6. Tạo Ticket hợp lệ, sinh ticketCode SR-yyyyMMdd-XXXXX, status NEW, history CREATED")
    void createSupportRequest_ValidPayload_Success() {
        CreateSupportRequestRequest request = CreateSupportRequestRequest.builder()
                .categoryCode("CONNECTION")
                .servicePlanId("plan-max100-id")
                .subject("Mạng Internet chập chờn liên tục")
                .content("Tín hiệu mạng bị gián đoạn từ sáng đến giờ, đề nghị kỹ thuật kiểm tra")
                .contactPhone("0987654321")
                .contactEmail("test@gmail.com")
                .build();

        when(supportCategoryRepository.findByCodeAndActiveTrue("CONNECTION"))
                .thenReturn(Optional.of(categoryConnection));
        when(servicePlanRepository.findById("plan-max100-id"))
                .thenReturn(Optional.of(servicePlanMax100));
        when(supportRequestRepository.getNextTicketSequence())
                .thenReturn(1L);
        when(supportRequestRepository.save(any(SupportRequest.class)))
                .thenAnswer(inv -> {
                    SupportRequest req = inv.getArgument(0);
                    req.setId("sr-created-id");
                    return req;
                });

        String customerId = "cust-123-uuid";
        SupportRequestResponse response = supportRequestService.createSupportRequest(request, customerId);

        assertThat(response).isNotNull();
        assertThat(response.getTicketCode()).startsWith("SR-");
        assertThat(response.getCustomerId()).isEqualTo(customerId);
        assertThat(response.getStatus()).isEqualTo(SupportRequestStatus.NEW);
        assertThat(response.getSubject()).isEqualTo("Mạng Internet chập chờn liên tục");

        // Verify history CREATED was saved
        ArgumentCaptor<SupportRequestHistory> historyCaptor = ArgumentCaptor.forClass(SupportRequestHistory.class);
        verify(supportRequestHistoryRepository).save(historyCaptor.capture());
        SupportRequestHistory history = historyCaptor.getValue();
        assertThat(history.getAction()).isEqualTo(SupportRequestHistoryAction.CREATED);
        assertThat(history.getFromStatus()).isNull();
        assertThat(history.getToStatus()).isEqualTo(SupportRequestStatus.NEW);
        assertThat(history.getActorId()).isEqualTo(customerId);
    }

    @Test
    @DisplayName("7. Throw SUPPORT_CATEGORY_NOT_EXISTED khi Category không tồn tại hoặc bị ẩn")
    void createSupportRequest_CategoryNotFound_ThrowsException() {
        CreateSupportRequestRequest request = CreateSupportRequestRequest.builder()
                .categoryCode("NON_EXISTENT")
                .subject("Hỗ trợ kỹ thuật mạng viễn thông")
                .content("Nội dung mô tả yêu cầu kiểm tra kỹ thuật...")
                .build();

        when(supportCategoryRepository.findByCodeAndActiveTrue("NON_EXISTENT"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> supportRequestService.createSupportRequest(request, "cust-123"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.SUPPORT_CATEGORY_NOT_EXISTED);

        verify(supportRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("8. Throw PLAN_NOT_EXISTED khi Service Plan không tồn tại")
    void createSupportRequest_PlanNotFound_ThrowsException() {
        CreateSupportRequestRequest request = CreateSupportRequestRequest.builder()
                .categoryCode("CONNECTION")
                .servicePlanId("invalid-plan-id")
                .subject("Hỗ trợ kỹ thuật mạng viễn thông")
                .content("Nội dung mô tả yêu cầu kiểm tra kỹ thuật...")
                .build();

        when(supportCategoryRepository.findByCodeAndActiveTrue("CONNECTION"))
                .thenReturn(Optional.of(categoryConnection));
        when(servicePlanRepository.findById("invalid-plan-id"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> supportRequestService.createSupportRequest(request, "cust-123"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PLAN_NOT_EXISTED);
    }

    @Test
    @DisplayName("10. Throw UNAUTHENTICATED khi customerId null hoặc rỗng")
    void createSupportRequest_Unauthenticated_ThrowsException() {
        CreateSupportRequestRequest request = CreateSupportRequestRequest.builder()
                .categoryCode("CONNECTION")
                .subject("Hỗ trợ kỹ thuật mạng viễn thông")
                .content("Nội dung mô tả yêu cầu kiểm tra kỹ thuật...")
                .build();

        assertThatThrownBy(() -> supportRequestService.createSupportRequest(request, ""))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHENTICATED);
    }

    @Test
    @DisplayName("11. Rollback transaction nếu tạo History thất bại")
    void createSupportRequest_HistorySaveFails_PropagatesException() {
        CreateSupportRequestRequest request = CreateSupportRequestRequest.builder()
                .categoryCode("CONNECTION")
                .subject("Hỗ trợ kỹ thuật mạng viễn thông")
                .content("Nội dung mô tả yêu cầu kiểm tra kỹ thuật...")
                .build();

        when(supportCategoryRepository.findByCodeAndActiveTrue("CONNECTION"))
                .thenReturn(Optional.of(categoryConnection));
        when(supportRequestRepository.save(any(SupportRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("Database error during history save"))
                .when(supportRequestHistoryRepository).save(any(SupportRequestHistory.class));

        assertThatThrownBy(() -> supportRequestService.createSupportRequest(request, "cust-123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error");
    }

    @Test
    @DisplayName("12. Kiểm thử đồng thời (Concurrency) không sinh mã trùng")
    void createSupportRequest_ConcurrentRequests_GeneratesUniqueTicketCodes() throws InterruptedException {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        ConcurrentHashMap<String, Boolean> generatedCodes = new ConcurrentHashMap<>();

        when(supportCategoryRepository.findByCodeAndActiveTrue("CONNECTION"))
                .thenReturn(Optional.of(categoryConnection));
        when(supportRequestRepository.save(any(SupportRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    CreateSupportRequestRequest req = CreateSupportRequestRequest.builder()
                            .categoryCode("CONNECTION")
                            .subject("Yêu cầu kiểm tra mạng viễn thông")
                            .content("Nội dung kiểm thử truy cập đồng thời...")
                            .build();

                    SupportRequestResponse res = supportRequestService.createSupportRequest(req, "cust-concurrent");
                    generatedCodes.put(res.getTicketCode(), true);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertThat(generatedCodes.size()).isEqualTo(threads);
    }
}
