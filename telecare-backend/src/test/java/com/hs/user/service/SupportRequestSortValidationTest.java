package com.hs.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.hs.user.advice.base.AppException;
import com.hs.user.constant.base.ErrorCode;
import com.hs.user.dto.request.CustomerSupportRequestQuery;
import com.hs.user.dto.request.SupportRequestAdminQuery;
import com.hs.user.model.SupportCategory;
import com.hs.user.model.SupportRequest;
import com.hs.user.model.User;
import com.hs.user.repository.SupportCategoryRepository;
import com.hs.user.repository.SupportRequestHistoryRepository;
import com.hs.user.repository.SupportRequestRepository;
import com.hs.user.repository.UserRepository;
import com.hs.user.service.impl.SupportRequestAdminServiceImpl;
import com.hs.user.service.impl.SupportRequestServiceImpl;

@ExtendWith(MockitoExtension.class)
class SupportRequestSortValidationTest {

    @Mock
    private SupportRequestRepository supportRequestRepository;

    @Mock
    private SupportRequestHistoryRepository supportRequestHistoryRepository;

    @Mock
    private SupportCategoryRepository supportCategoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SupportRequestServiceImpl customerService;

    @InjectMocks
    private SupportRequestAdminServiceImpl adminService;

    @Test
    @DisplayName("Customer support request: invalid sort field should throw INVALID_SORT_FIELD")
    void customerFind_invalidSort_throwsAppException() {
        Pageable requested = PageRequest.of(0, 10, Sort.by("maliciousSqlInjection").descending());

        assertThatThrownBy(() -> customerService.findMySupportRequests("cust-1", new CustomerSupportRequestQuery(), requested))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SORT_FIELD);
    }

    @Test
    @DisplayName("Admin support request: invalid sort field should throw INVALID_SORT_FIELD")
    void adminFind_invalidSort_throwsAppException() {
        Pageable requested = PageRequest.of(0, 10, Sort.by("nonExistentField").descending());

        assertThatThrownBy(() -> adminService.findAllAdminSupportRequests(new SupportRequestAdminQuery(), requested))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SORT_FIELD);
    }

    @Test
    @DisplayName("Admin support request: batch resolves customer profiles without N+1 individual queries")
    void adminFind_batchResolvesCustomers() {
        Pageable requested = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        SupportCategory cat = SupportCategory.builder().id("cat-1").code("CAT").name("Category").build();
        SupportRequest req1 = SupportRequest.builder().id("sr-1").ticketCode("SR-001").customerId("cust-1").category(cat).subject("S1").build();
        SupportRequest req2 = SupportRequest.builder().id("sr-2").ticketCode("SR-002").customerId("cust-2").category(cat).subject("S2").build();

        Page<SupportRequest> page = new PageImpl<>(List.of(req1, req2), requested, 2);
        when(supportRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        User u1 = new User();
        u1.setId("cust-1");
        u1.setUsername("c1");
        u1.setEmail("c1@telecare.vn");

        User u2 = new User();
        u2.setId("cust-2");
        u2.setUsername("c2");
        u2.setEmail("c2@telecare.vn");

        when(userRepository.findAllById(any())).thenReturn(List.of(u1, u2));

        var result = adminService.findAllAdminSupportRequests(new SupportRequestAdminQuery(), requested);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getCustomer().username()).isEqualTo("c1");
        assertThat(result.getContent().get(1).getCustomer().username()).isEqualTo("c2");

        // Verify batch query findAllById was called once with set of IDs
        verify(userRepository).findAllById(any());
    }
}
