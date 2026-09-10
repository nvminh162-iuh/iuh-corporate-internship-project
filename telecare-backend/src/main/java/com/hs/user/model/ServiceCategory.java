package com.hs.user.model;

import java.util.UUID;

import com.hs.user.model.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
        name = "service_categories",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_service_categories_code", columnNames = {"code"})
        },
        indexes = {
                @Index(name = "idx_service_categories_active", columnList = "active"),
                @Index(name = "idx_service_categories_display_order", columnList = "display_order")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceCategory extends BaseEntity {

    @Id
    @Column(nullable = false, unique = true)
    String id;

    @Column(nullable = false, unique = true, length = 100)
    String code;

    @Column(nullable = false, length = 120)
    String name;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "display_order")
    Integer displayOrder;

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
    }
}
