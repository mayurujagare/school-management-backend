package com.schoolmanagement.module.fee.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fee_discounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeDiscount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(nullable = false, length = 100)
    private String name;               // 'Sibling Discount','Merit','Staff Ward'

    @Column(name = "discount_type", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private DiscountType discountType; // PERCENT | FIXED

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum DiscountType {
        PERCENT, FIXED
    }
}