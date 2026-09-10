package com.hs.user.model;

import java.util.UUID;

import com.hs.user.model.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
        name = "plan_features",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_plan_features_plan_code", columnNames = {"plan_id", "code"})
        },
        indexes = {
                @Index(name = "idx_plan_features_plan_id", columnList = "plan_id")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlanFeature extends BaseEntity {

    @Id
    @Column(nullable = false, unique = true)
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    ServicePlan plan;

    @Column(nullable = false, length = 100)
    String code;

    @Column(nullable = false, length = 120)
    String name;

    @Column(nullable = false, length = 255)
    String value;

    @Column(length = 50)
    String unit;

    @Column(name = "display_order")
    Integer displayOrder;

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
    }
}
