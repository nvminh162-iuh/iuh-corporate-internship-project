package com.hs.user.config.database;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hs.user.model.PlanFeature;
import com.hs.user.model.ServiceCategory;
import com.hs.user.model.ServicePlan;
import com.hs.user.model.constant.BillingCycle;
import com.hs.user.model.constant.PlanStatus;
import com.hs.user.repository.ServiceCategoryRepository;
import com.hs.user.repository.ServicePlanRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Order(4)
@RequiredArgsConstructor
@Slf4j
public class ServicePlanDataInitializer implements CommandLineRunner {

    private final ServiceCategoryRepository categoryRepository;
    private final ServicePlanRepository planRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking service category and plan seed data...");

        ServiceCategory catData = seedCategory("DATA", "Gói Data Internet", "Các gói cước truy cập Internet 4G/5G tốc độ cao", 1);
        ServiceCategory catCall = seedCategory("CALL", "Gói Thoại & SMS", "Các gói cước nghe gọi nội mạng, ngoại mạng và nhắn tin", 2);
        ServiceCategory catCombo = seedCategory("COMBO", "Gói Combo Đa Dịch Vụ", "Các gói cước tích hợp cả Data, Gọi thoại và tiện ích bổ sung", 3);
        ServiceCategory catHealth = seedCategory("HEALTH", "Gói Dịch Vụ TeleCare", "Gói cước tư vấn sức khỏe và theo dõi y tế từ xa", 4);

        seedPlan(
                "MAX100",
                "max100",
                "Gói cước Data MAX100",
                "100GB Data 4G/5G tốc độ cao sử dụng trong 30 ngày",
                "Gói cước MAX100 mang đến trải nghiệm truy cập Internet không giới hạn với 100GB Data siêu tốc độ.",
                new BigDecimal("100000.00"),
                BillingCycle.MONTH,
                PlanStatus.PUBLISHED,
                catData,
                true,
                1,
                List.of(
                        buildFeature("DATA_CAP", "Dung lượng Data", "100", "GB", 1),
                        buildFeature("SPEED", "Tốc độ tối đa", "5G / 4G LTE", "Mbps", 2),
                        buildFeature("FREE_MAPS", "Truy cập bản đồ y tế", "Miễn phí", null, 3)
                )
        );

        seedPlan(
                "V90",
                "v90",
                "Gói cước Gọi Thoại V90",
                "Miễn phí tất cả cuộc gọi nội mạng dưới 20 phút và 50 phút ngoại mạng",
                "Gói V90 dành cho khách hàng có nhu cầu liên lạc thường xuyên. Miễn phí không giới hạn cuộc gọi nội mạng ngắn.",
                new BigDecimal("90000.00"),
                BillingCycle.MONTH,
                PlanStatus.PUBLISHED,
                catCall,
                false,
                2,
                List.of(
                        buildFeature("INT_CALL", "Gọi nội mạng", "Miễn phí < 20 phút", "phút", 1),
                        buildFeature("EXT_CALL", "Gọi ngoại mạng", "50", "phút", 2),
                        buildFeature("SMS_FREE", "SMS nội mạng", "100", "tin", 3)
                )
        );

        seedPlan(
                "COMBO5G",
                "combo5g",
                "Gói Combo VIP 5G",
                "Tích hợp 150GB Data, gọi nội mạng thả ga và miễn phí tư vấn bác sĩ",
                "Gói Combo VIP 5G là sự kết hợp hoàn hảo giữa Data dung lượng cực khủng, liên lạc không giới hạn và tính năng TeleCare.",
                new BigDecimal("200000.00"),
                BillingCycle.MONTH,
                PlanStatus.PUBLISHED,
                catCombo,
                true,
                3,
                List.of(
                        buildFeature("DATA_CAP", "Dung lượng Data", "150", "GB", 1),
                        buildFeature("INT_CALL", "Gọi nội mạng", "Không giới hạn", null, 2),
                        buildFeature("EXT_CALL", "Gọi ngoại mạng", "100", "phút", 3),
                        buildFeature("DOCTOR_CALL", "Tư vấn bác sĩ TeleCare", "2 cuộc gọi/tháng", "lần", 4)
                )
        );

        seedPlan(
                "HEALTH_BASIC",
                "health-basic",
                "Gói Bác Sĩ Gia Đình TeleCare",
                "Chăm sóc sức khỏe định kỳ từ xa cho cả gia đình",
                "Gói dịch vụ sức khỏe TeleCare Basic bao gồm đo chỉ số sinh tồn từ xa, lưu trữ hồ sơ bệnh án điện tử.",
                new BigDecimal("300000.00"),
                BillingCycle.MONTH,
                PlanStatus.PUBLISHED,
                catHealth,
                false,
                4,
                List.of(
                        buildFeature("HEALTH_RECORD", "Hồ sơ sức khỏe điện tử", "Không giới hạn", null, 1),
                        buildFeature("CONSULTATION", "Tư vấn y tế từ xa", "24/7", null, 2),
                        buildFeature("FAMILY_MEMBERS", "Số thành viên áp dụng", "4", "người", 3)
                )
        );

        seedPlan(
                "DRAFT_TEST",
                "draft-test",
                "Gói cước Thử Nghiệm 5G",
                "Gói cước nháp dùng cho mục đích kiểm thử hệ thống",
                "Gói cước đang ở trạng thái DRAFT để kiểm thử luồng cập nhật và phát hành gói cước.",
                new BigDecimal("50000.00"),
                BillingCycle.MONTH,
                PlanStatus.DRAFT,
                catData,
                false,
                5,
                List.of(
                        buildFeature("TEST_FEAT", "Tính năng thử nghiệm", "Demo", null, 1)
                )
        );

        log.info("Service categories and plans seed data check completed.");
    }

    private ServiceCategory seedCategory(String code, String name, String description, Integer displayOrder) {
        return categoryRepository.findAll()
                .stream()
                .filter(c -> code.equalsIgnoreCase(c.getCode()))
                .findFirst()
                .orElseGet(() -> {
                    ServiceCategory category = ServiceCategory.builder()
                            .code(code)
                            .name(name)
                            .description(description)
                            .displayOrder(displayOrder)
                            .build();
                    category.setActive(true);
                    log.info("Seeded service category: {}", code);
                    return categoryRepository.save(category);
                });
    }

    private void seedPlan(
            String code,
            String slug,
            String name,
            String summary,
            String description,
            BigDecimal price,
            BillingCycle billingCycle,
            PlanStatus status,
            ServiceCategory category,
            Boolean highlighted,
            Integer displayOrder,
            List<PlanFeature> features
    ) {
        if (planRepository.existsByCode(code)) {
            return;
        }

        ServicePlan plan = ServicePlan.builder()
                .code(code)
                .slug(slug)
                .name(name)
                .summary(summary)
                .description(description)
                .price(price)
                .currency("VND")
                .billingCycle(billingCycle)
                .status(status)
                .category(category)
                .highlighted(highlighted)
                .displayOrder(displayOrder)
                .build();
        plan.setActive(true);

        for (PlanFeature feature : features) {
            plan.addFeature(feature);
        }

        planRepository.save(plan);
        log.info("Seeded service plan: {}", code);
    }

    private PlanFeature buildFeature(String code, String name, String value, String unit, Integer displayOrder) {
        PlanFeature feature = PlanFeature.builder()
                .code(code)
                .name(name)
                .value(value)
                .unit(unit)
                .displayOrder(displayOrder)
                .build();
        feature.setActive(true);
        return feature;
    }
}
