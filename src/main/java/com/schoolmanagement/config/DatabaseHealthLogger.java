package com.schoolmanagement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseHealthLogger {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource   dataSource;

    // Runs once after Spring context is fully started
    @EventListener(ApplicationReadyEvent.class)
    public void verifyDatabaseConnection() {

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("  DATABASE CONNECTION VERIFICATION");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        try (Connection connection = dataSource.getConnection()) {

            DatabaseMetaData meta = connection.getMetaData();

            log.info("  ✅ Database connected successfully");
            log.info("  📦 Database    : {}", meta.getDatabaseProductName());
            log.info("  🔢 Version     : {}", meta.getDatabaseProductVersion());
            log.info("  🔗 URL         : {}", meta.getURL());
            log.info("  👤 Username    : {}", meta.getUserName());
            log.info("  🔌 Driver      : {}", meta.getDriverName());

            // Verify schema by counting tables
            Integer tableCount = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM information_schema.tables
                    WHERE table_schema = 'public'
                    AND table_type = 'BASE TABLE'
                    """,
                    Integer.class);

            log.info("  📋 Tables found: {}", tableCount);

            // Warn if tables are missing
            if (tableCount == null || tableCount < 25) {
                log.warn("  ⚠️  Expected 25+ tables but found {}.", tableCount);
                log.warn("  ⚠️  Run Flyway migration or check schema.");
            } else {
                log.info("  ✅ Schema looks complete ({} tables)", tableCount);
            }

            // Verify seed data — roles table
            Integer roleCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM roles",
                    Integer.class);

            if (roleCount == null || roleCount < 6) {
                log.warn("  ⚠️  Roles not seeded properly. Found {} roles", roleCount);
            } else {
                log.info("  ✅ Roles seeded : {} roles found", roleCount);
            }

        } catch (Exception e) {
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.error("  ❌ DATABASE CONNECTION FAILED");
            log.error("  Error : {}", e.getMessage());
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        }

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}