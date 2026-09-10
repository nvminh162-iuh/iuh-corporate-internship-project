package com.hs.user.model;

import com.hs.user.model.base.BaseEntity;
import com.hs.user.model.constant.SupportRequestStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "support_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SupportRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "ticket_code", nullable = false, unique = true, length = 50)
    String ticketCode;

    @Column(name = "customer_id", nullable = false, length = 100)
    String customerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    SupportCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_plan_id")
    ServicePlan servicePlan;

    @Column(name = "subject", nullable = false, length = 200)
    String subject;

    @Column(name = "content", nullable = false, length = 2000)
    String content;

    @Column(name = "contact_phone", length = 20)
    String contactPhone;

    @Column(name = "contact_email", length = 100)
    String contactEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    User assignedTo;

    @Column(name = "received_at")
    java.time.Instant receivedAt;

    @Column(name = "assigned_at")
    java.time.Instant assignedAt;

    @Column(name = "completed_at")
    java.time.Instant completedAt;

    @Column(name = "closed_at")
    java.time.Instant closedAt;

    @Column(name = "resolution", length = 2000)
    String resolution;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    SupportRequestStatus status = SupportRequestStatus.NEW;
}
