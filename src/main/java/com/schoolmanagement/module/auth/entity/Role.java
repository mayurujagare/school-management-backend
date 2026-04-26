package com.schoolmanagement.module.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String name;

    // Role constants — avoids magic strings across the codebase
    public static final String SUPER_ADMIN  = "SUPER_ADMIN";
    public static final String SCHOOL_ADMIN = "SCHOOL_ADMIN";
    public static final String PRINCIPAL    = "PRINCIPAL";
    public static final String TEACHER      = "TEACHER";
    public static final String CLERK        = "CLERK";
    public static final String PARENT       = "PARENT";
}