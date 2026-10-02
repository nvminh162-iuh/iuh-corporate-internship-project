package com.hs.user.repository.specification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import com.hs.user.constant.PermissionConstants;
import com.hs.user.constant.RoleConstants;
import com.hs.user.dto.request.SupportRequestAdminQuery;
import com.hs.user.model.Permission;
import com.hs.user.model.Role;
import com.hs.user.model.SupportCategory;
import com.hs.user.model.SupportRequest;
import com.hs.user.model.User;
import com.hs.user.model.constant.SupportRequestStatus;
import com.hs.user.repository.PermissionRepository;
import com.hs.user.repository.RoleRepository;
import com.hs.user.repository.SupportCategoryRepository;
import com.hs.user.repository.SupportRequestRepository;
import com.hs.user.repository.UserRepository;

@SpringBootTest
@Transactional
class SupportRequestSpecificationTest {

    @Autowired
    private SupportRequestRepository supportRequestRepository;

    @Autowired
    private SupportCategoryRepository supportCategoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    private SupportCategory netCategory;
    private SupportCategory packCategory;
    private User staff1;
    private User staff2;
    private SupportRequest ticket1;
    private SupportRequest ticket2;
    private SupportRequest ticket3;

    @BeforeEach
    void setUp() {
        // Setup Categories
        netCategory = supportCategoryRepository.save(SupportCategory.builder()
                .code("SPEC_NET_" + UUID.randomUUID().toString().substring(0, 8))
                .name("Sự cố mạng Specification")
                .build());

        packCategory = supportCategoryRepository.save(SupportCategory.builder()
                .code("SPEC_PACK_" + UUID.randomUUID().toString().substring(0, 8))
                .name("Gói cước Specification")
                .build());

        // Setup Roles and Permissions
        Permission processPerm = permissionRepository.findByName(PermissionConstants.Admin.SUPPORT_REQUEST_PROCESS)
                .orElseGet(() -> {
                    Permission p = Permission.builder()
                            .id("perm-spec-proc-" + UUID.randomUUID())
                            .name(PermissionConstants.Admin.SUPPORT_REQUEST_PROCESS)
                            .description("Quy trình xử lý support")
                            .build();
                    p.setActive(true);
                    return permissionRepository.save(p);
                });

        Role supportRole = Role.builder()
                .id("role-spec-support-" + UUID.randomUUID())
                .name("SPEC_SUPPORT_" + UUID.randomUUID().toString().substring(0, 8))
                .permissions(Set.of(processPerm))
                .build();
        supportRole.setActive(true);
        supportRole = roleRepository.save(supportRole);

        // Setup Staff
        staff1 = new User();
        staff1.setId("spec-staff-1-" + UUID.randomUUID());
        staff1.setUsername("specstaff1_" + UUID.randomUUID().toString().substring(0, 8));
        staff1.setEmail("specstaff1_" + UUID.randomUUID().toString().substring(0, 8) + "@telecare.com");
        staff1.setFirstName("Van A");
        staff1.setLastName("Nguyen");
        staff1.setActive(true);
        staff1.setRole(supportRole);
        staff1 = userRepository.save(staff1);

        staff2 = new User();
        staff2.setId("spec-staff-2-" + UUID.randomUUID());
        staff2.setUsername("specstaff2_" + UUID.randomUUID().toString().substring(0, 8));
        staff2.setEmail("specstaff2_" + UUID.randomUUID().toString().substring(0, 8) + "@telecare.com");
        staff2.setFirstName("Thi B");
        staff2.setLastName("Tran");
        staff2.setActive(true);
        staff2.setRole(supportRole);
        staff2 = userRepository.save(staff2);

        // Setup Tickets
        ticket1 = SupportRequest.builder()
                .ticketCode("SR-SPEC-001-" + UUID.randomUUID().toString().substring(0, 6))
                .customerId("spec-cust-001")
                .category(netCategory)
                .subject("Mạng Internet chập chờn phòng 101")
                .content("Nội dung mô tả sự cố kỹ thuật mạng chi tiết...")
                .contactPhone("0912345678")
                .contactEmail("cust1@example.com")
                .status(SupportRequestStatus.NEW)
                .build();
        ticket1 = supportRequestRepository.save(ticket1);

        ticket2 = SupportRequest.builder()
                .ticketCode("SR-SPEC-002-" + UUID.randomUUID().toString().substring(0, 6))
                .customerId("spec-cust-002")
                .category(packCategory)
                .subject("Hỗ trợ đổi gói cước tốc độ cao")
                .content("Muốn nâng cấp gói cước băng thông rộng...")
                .contactPhone("0987654321")
                .contactEmail("cust2@example.com")
                .status(SupportRequestStatus.IN_PROGRESS)
                .assignedTo(staff1)
                .build();
        ticket2 = supportRequestRepository.save(ticket2);

        ticket3 = SupportRequest.builder()
                .ticketCode("SR-SPEC-003-" + UUID.randomUUID().toString().substring(0, 6))
                .customerId("spec-cust-003")
                .category(netCategory)
                .subject("Không thể truy cập wifi tầng 3")
                .content("Đèn wifi sáng đỏ liên tục từ hôm qua...")
                .contactPhone("0933445566")
                .contactEmail("cust3@example.com")
                .status(SupportRequestStatus.COMPLETED)
                .assignedTo(staff2)
                .resolution("Đã thay converter quang")
                .build();
        ticket3 = supportRequestRepository.save(ticket3);
    }

    @Test
    @DisplayName("Filter by keyword (ticketCode, subject, phone, email, customerId)")
    void filterByKeyword_Success() {
        SupportRequestAdminQuery query = SupportRequestAdminQuery.builder()
                .keyword("phòng 101")
                .build();

        Page<SupportRequest> result = supportRequestRepository.findAll(
                SupportRequestSpecification.filterAdminRequests(query),
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).extracting("id").contains(ticket1.getId());
        assertThat(result.getContent()).extracting("id").doesNotContain(ticket2.getId(), ticket3.getId());
    }

    @Test
    @DisplayName("Filter by status")
    void filterByStatus_Success() {
        SupportRequestAdminQuery query = SupportRequestAdminQuery.builder()
                .status(SupportRequestStatus.IN_PROGRESS)
                .build();

        Page<SupportRequest> result = supportRequestRepository.findAll(
                SupportRequestSpecification.filterAdminRequests(query),
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).extracting("id").contains(ticket2.getId());
        assertThat(result.getContent()).extracting("id").doesNotContain(ticket1.getId(), ticket3.getId());
    }

    @Test
    @DisplayName("Filter by categoryCode")
    void filterByCategoryCode_Success() {
        SupportRequestAdminQuery query = SupportRequestAdminQuery.builder()
                .categoryCode(packCategory.getCode())
                .build();

        Page<SupportRequest> result = supportRequestRepository.findAll(
                SupportRequestSpecification.filterAdminRequests(query),
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).extracting("id").contains(ticket2.getId());
        assertThat(result.getContent()).extracting("id").doesNotContain(ticket1.getId(), ticket3.getId());
    }

    @Test
    @DisplayName("Filter by assignedTo staff ID")
    void filterByAssignedTo_Success() {
        SupportRequestAdminQuery query = SupportRequestAdminQuery.builder()
                .assignedTo(staff1.getId())
                .build();

        Page<SupportRequest> result = supportRequestRepository.findAll(
                SupportRequestSpecification.filterAdminRequests(query),
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).extracting("id").contains(ticket2.getId());
        assertThat(result.getContent()).extracting("id").doesNotContain(ticket1.getId(), ticket3.getId());
    }

    @Test
    @DisplayName("Filter by date range using Asia/Bangkok timezone boundaries")
    void filterByDateRange_Success() {
        ZoneId zone = ZoneId.of("Asia/Bangkok");
        LocalDate today = LocalDate.now(zone);

        SupportRequestAdminQuery query = SupportRequestAdminQuery.builder()
                .fromDate(today.minusDays(1))
                .toDate(today.plusDays(1))
                .build();

        Page<SupportRequest> result = supportRequestRepository.findAll(
                SupportRequestSpecification.filterAdminRequests(query),
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).extracting("id").contains(ticket1.getId(), ticket2.getId(), ticket3.getId());

        // Future date range should not match today's created tickets
        SupportRequestAdminQuery futureQuery = SupportRequestAdminQuery.builder()
                .fromDate(today.plusDays(5))
                .toDate(today.plusDays(10))
                .build();

        Page<SupportRequest> futureResult = supportRequestRepository.findAll(
                SupportRequestSpecification.filterAdminRequests(futureQuery),
                PageRequest.of(0, 10)
        );

        assertThat(futureResult.getContent()).extracting("id").doesNotContain(ticket1.getId(), ticket2.getId(), ticket3.getId());
    }

    @Test
    @DisplayName("Combined multi-filter query")
    void filterByCombinedCriteria_Success() {
        SupportRequestAdminQuery query = SupportRequestAdminQuery.builder()
                .keyword("băng thông")
                .status(SupportRequestStatus.IN_PROGRESS)
                .categoryCode(packCategory.getCode())
                .assignedTo(staff1.getId())
                .fromDate(LocalDate.now(ZoneId.of("Asia/Bangkok")).minusDays(1))
                .toDate(LocalDate.now(ZoneId.of("Asia/Bangkok")).plusDays(1))
                .build();

        Page<SupportRequest> result = supportRequestRepository.findAll(
                SupportRequestSpecification.filterAdminRequests(query),
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(ticket2.getId());
    }

    @Test
    @DisplayName("Stable sort by createdAt DESC, id DESC")
    void stableSort_Success() {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
        PageRequest pageRequest = PageRequest.of(0, 10, sort);

        Page<SupportRequest> page = supportRequestRepository.findAll(
                SupportRequestSpecification.filterAdminRequests(null),
                pageRequest
        );

        assertThat(page.getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("UserRepository: findEligibleSupportAssignees returns active staff with SUPPORT_REQUEST_PROCESS")
    void findEligibleSupportAssignees_Success() {
        List<User> eligible = userRepository.findEligibleSupportAssignees(staff1.getUsername());

        assertThat(eligible).isNotEmpty();
        assertThat(eligible).extracting("id").contains(staff1.getId());
    }
}
