package com.hs.user.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.hs.user.model.base.BaseEntity;
import com.hs.user.model.constant.BillingCycle;
import com.hs.user.model.constant.PlanStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Entity
@Table(
        name = "service_plans",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_service_plans_code", columnNames = {"code"}),
                @UniqueConstraint(name = "uk_service_plans_slug", columnNames = {"slug"})
        },
        indexes = {
                @Index(name = "idx_service_plans_category_id", columnList = "category_id"),
                @Index(name = "idx_service_plans_status", columnList = "status"),
                @Index(name = "idx_service_plans_active", columnList = "active"),
                @Index(name = "idx_service_plans_created_at", columnList = "created_at")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServicePlan extends BaseEntity {

    @Id
    @Column(nullable = false, unique = true)
    String id;

    @Column(nullable = false, unique = true, length = 100)
    String code;

    @Column(nullable = false, unique = true, length = 120)
    String slug;

    @Column(nullable = false, length = 120)
    String name;

    @Column(length = 255)
    String summary;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(nullable = false, precision = 15, scale = 2)
    BigDecimal price;

    @Column(nullable = false, length = 3)
    @Builder.Default
    String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    BillingCycle billingCycle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    PlanStatus status = PlanStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    ServiceCategory category;

    @Column(nullable = false)
    @Builder.Default
    Boolean highlighted = false;

    @Column(name = "display_order")
    Integer displayOrder;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<PlanFeature> features = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
    }

    public void addFeature(PlanFeature feature) {
        features.add(feature);
        feature.setPlan(this);
    }

    public void removeFeature(PlanFeature feature) {
        features.remove(feature);
        feature.setPlan(null);
    }

    public void replaceFeatures(List<PlanFeature> newFeatures) {
        this.features.clear();
        if (newFeatures != null) {
            for (PlanFeature feature : newFeatures) {
                addFeature(feature);
            }
        }
    }
}
