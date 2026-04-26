package com.schoolmanagement.module.staff.entity;

import com.schoolmanagement.module.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "staff")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "employee_code", length = 50)
    private String employeeCode;

    @Column(length = 100)
    private String designation;

    @Column(length = 100)
    private String department;

    @Column(name = "date_of_joining")
    private LocalDate dateOfJoining;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── helpers ───────────────────────────────────────────────
    public String getFullName() {
        return user.getFullName();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getMobile() {
        return user.getMobile();
    }
}