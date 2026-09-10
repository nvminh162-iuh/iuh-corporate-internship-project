package com.hs.user.config.database;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.jdbc.core.JdbcTemplate;

import com.hs.user.model.SupportCategory;
import com.hs.user.repository.SupportCategoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Order(5)
@RequiredArgsConstructor
@Slf4j
public class SupportCategoryDataInitializer implements CommandLineRunner {

    private final SupportCategoryRepository supportCategoryRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking support category seed data and ticket sequence...");

        try {
            jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS support_request_ticket_seq START WITH 1 INCREMENT BY 1");
            log.info("Sequence support_request_ticket_seq created or verified.");
        } catch (Exception e) {
            log.warn("Could not create sequence support_request_ticket_seq: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE support_requests DROP CONSTRAINT IF EXISTS support_requests_status_check");
            jdbcTemplate.execute("ALTER TABLE support_requests ADD CONSTRAINT support_requests_status_check CHECK (status IN ('NEW', 'RECEIVED', 'IN_PROGRESS', 'WAITING_CUSTOMER', 'COMPLETED', 'CLOSED'))");

            jdbcTemplate.execute("ALTER TABLE support_request_histories DROP CONSTRAINT IF EXISTS support_request_histories_to_status_check");
            jdbcTemplate.execute("ALTER TABLE support_request_histories ADD CONSTRAINT support_request_histories_to_status_check CHECK (to_status IS NULL OR to_status IN ('NEW', 'RECEIVED', 'IN_PROGRESS', 'WAITING_CUSTOMER', 'COMPLETED', 'CLOSED'))");

            jdbcTemplate.execute("ALTER TABLE support_request_histories DROP CONSTRAINT IF EXISTS support_request_histories_from_status_check");
            jdbcTemplate.execute("ALTER TABLE support_request_histories ADD CONSTRAINT support_request_histories_from_status_check CHECK (from_status IS NULL OR from_status IN ('NEW', 'RECEIVED', 'IN_PROGRESS', 'WAITING_CUSTOMER', 'COMPLETED', 'CLOSED'))");
            log.info("Support request status check constraints verified and updated.");
        } catch (Exception e) {
            log.warn("Could not update support request status check constraints: {}", e.getMessage());
        }

        seedCategory("PACKAGE", "Gói cước", "Các vấn đề liên quan đến đăng ký, gia hạn và hủy gói cước", 1);
        seedCategory("SERVICE", "Dịch vụ", "Hỗ trợ tư vấn và hướng dẫn sử dụng dịch vụ TeleCare", 2);
        seedCategory("CONNECTION", "Kết nối", "Sự cố kết nối mạng, sóng yếu hoặc không thể kết nối", 3);
        seedCategory("PAYMENT", "Thanh toán", "Nạp tiền, thanh toán cước và tra cứu hóa đơn", 4);
        seedCategory("OTHER", "Vấn đề khác", "Các thắc mắc và yêu cầu hỗ trợ khác", 5);

        log.info("Support category seed data check completed.");
    }

    private void seedCategory(String code, String name, String description, Integer displayOrder) {
        if (!supportCategoryRepository.existsByCode(code)) {
            SupportCategory category = SupportCategory.builder()
                    .code(code)
                    .name(name)
                    .description(description)
                    .displayOrder(displayOrder)
                    .build();
            category.setActive(true);
            supportCategoryRepository.save(category);
            log.info("Seeded support category: {}", code);
        }
    }
}
