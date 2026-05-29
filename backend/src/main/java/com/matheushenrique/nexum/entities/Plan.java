package com.matheushenrique.nexum.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plans")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "amount_cents", nullable = false)
    private Integer amountCents;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Recurrence recurrence;

    @Column(name = "custom_days")
    private Integer customDays;

    @Column(name = "trial_days", nullable = false)
    @Builder.Default
    private Integer trialDays = 0;

    @Column(name = "max_subscriptions")
    private Integer maxSubscriptions;

    @Column(columnDefinition = "TEXT")
    private String features;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum Recurrence {
        MONTHLY, QUARTERLY, SEMIANNUAL, ANNUAL, CUSTOM
    }
}