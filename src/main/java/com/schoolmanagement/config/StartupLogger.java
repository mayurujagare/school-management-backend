package com.schoolmanagement.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StartupLogger {

    @Value("${server.port:8080}")
    private String port;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${spring.application.name}")
    private String appName;

    @EventListener(ApplicationReadyEvent.class)
    @Order(1)   // runs before DatabaseHealthLogger
    public void onStartup() {
        log.info("");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("  🏫 {} started successfully", appName.toUpperCase());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("  🌐 Profile      : {}", activeProfile);
        log.info("  🚀 API Base     : http://localhost:{}/api/v1", port);
        log.info("  📖 Swagger UI   : http://localhost:{}/swagger-ui.html", port);
        log.info("  💊 Health Check : http://localhost:{}/actuator/health", port);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("");
    }
}