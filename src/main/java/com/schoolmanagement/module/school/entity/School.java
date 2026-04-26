// src/main/java/com/schoolmanagement/module/school/entity/School.java
package com.schoolmanagement.module.school.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "schools")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String address;
    private String city;
    private String state;

    @Builder.Default
    private String country = "India";

    private String pincode;
    private String phone;
    private String email;

    @Column(name = "logo_url")
    private String logoUrl;

    private String website;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "subscription_plan")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.DEMO;

    @Column(name = "subscription_start")
    private LocalDate subscriptionStart;

    @Column(name = "subscription_expiry")
    private LocalDate subscriptionExpiry;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── helpers ──────────────────────────────────────────────
    public boolean isSubscriptionActive() {
        if (!isActive) return false;
        if (subscriptionExpiry == null) return true;  // no expiry = active
        return !LocalDate.now().isAfter(subscriptionExpiry);
    }

    public enum SubscriptionPlan {
        DEMO, BASIC, STANDARD, PREMIUM
    }
}