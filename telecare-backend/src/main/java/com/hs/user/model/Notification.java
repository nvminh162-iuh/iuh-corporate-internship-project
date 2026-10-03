package com.hs.user.model;

import java.time.Instant;

import com.hs.user.model.base.BaseEntity;
import com.hs.user.model.constant.NotificationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notifications_recipient_created", columnList = "recipient_id, created_at DESC"),
        @Index(name = "idx_notifications_recipient_read", columnList = "recipient_id, is_read")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "recipient_id", nullable = false, length = 100)
    String recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    NotificationType type;

    @Column(name = "title", nullable = false, length = 255)
    String title;

    @Column(name = "message", nullable = false, length = 1000)
    String message;

    @Column(name = "reference_type", length = 50)
    String referenceType;

    @Column(name = "reference_id", length = 100)
    String referenceId;

    @Column(name = "source_history_id", unique = true, length = 100)
    String sourceHistoryId;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    Boolean read = false;

    @Column(name = "read_at")
    Instant readAt;
}
