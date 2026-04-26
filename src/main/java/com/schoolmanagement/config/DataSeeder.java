package com.schoolmanagement.config;

import com.schoolmanagement.module.auth.entity.Role;
import com.schoolmanagement.module.auth.entity.User;
import com.schoolmanagement.module.auth.repository.RoleRepository;
import com.schoolmanagement.module.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@Order(1)   // runs first before other ApplicationRunners
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final RoleRepository  roleRepository;
    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.super-admin.email}")
    private String superAdminEmail;

    @Value("${app.super-admin.password}")
    private String superAdminPassword;

    @Value("${app.super-admin.first-name}")
    private String superAdminFirstName;

    @Value("${app.super-admin.last-name}")
    private String superAdminLastName;

    // ── Role names to seed ────────────────────────────────────
    private static final List<String> REQUIRED_ROLES = Arrays.asList(
            Role.SUPER_ADMIN,
            Role.SCHOOL_ADMIN,
            Role.PRINCIPAL,
            Role.TEACHER,
            Role.CLERK,
            Role.PARENT
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("  DATA SEEDER STARTING");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        seedRoles();
        seedSuperAdmin();

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ── Seed Roles ────────────────────────────────────────────
    private void seedRoles() {
        REQUIRED_ROLES.forEach(roleName -> {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(Role.builder().name(roleName).build());
                log.info("  ✅ Role created     : {}", roleName);
            } else {
                log.info("  ⏭️  Role exists      : {}", roleName);
            }
        });
    }

    // ── Seed Super Admin ──────────────────────────────────────
    private void seedSuperAdmin() {
        Optional<User> existing = userRepository.findByEmail(superAdminEmail);

        if (existing.isPresent()) {
            log.info("  ⏭️  Super Admin exists: {}", superAdminEmail);
            return;
        }

        Role superAdminRole = roleRepository.findByName(Role.SUPER_ADMIN)
                .orElseThrow(() -> new RuntimeException(
                        "SUPER_ADMIN role not found. Role seeding may have failed."));

        User superAdmin = User.builder()
                .schoolId(null)                     // no school — platform level
                .firstName(superAdminFirstName)
                .lastName(superAdminLastName)
                .email(superAdminEmail)
                .passwordHash(passwordEncoder.encode(superAdminPassword))
                .authType(User.AuthType.PASSWORD)
                .isActive(true)
                .roles(Set.of(superAdminRole))
                .build();

        userRepository.save(superAdmin);

        log.info("  ✅ Super Admin created: {}", superAdminEmail);
        log.info("  🔑 Password          : {}", superAdminPassword);
        log.warn("  ⚠️  Change this password immediately after first login!");
    }
}