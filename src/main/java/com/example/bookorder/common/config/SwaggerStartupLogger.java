package com.example.bookorder.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = {"springdoc.swagger-ui.enabled", "springdoc.api-docs.enabled"},
        matchIfMissing = true)
public class SwaggerStartupLogger {

    private static final Logger log = LoggerFactory.getLogger(SwaggerStartupLogger.class);

    private final ServerProperties serverProperties;

    public SwaggerStartupLogger(ServerProperties serverProperties) {
        this.serverProperties = serverProperties;
    }

    /** Log the local Swagger entry point only after successful web application startup. */
    @EventListener(ApplicationReadyEvent.class)
    public void logSwaggerUrl(ApplicationReadyEvent event) {
        if (!(event.getApplicationContext() instanceof ServletWebServerApplicationContext context)
                || context.getWebServer() == null || context.getWebServer().getPort() <= 0) {
            return;
        }

        Environment environment = context.getEnvironment();
        if (environment.getProperty("springdoc.use-management-port", Boolean.class, false)) {
            return;
        }
        String scheme = serverProperties.getSsl() != null && serverProperties.getSsl().isEnabled()
                ? "https" : "http";
        String contextPath = context.getServletContext().getContextPath();
        String servletPath = environment.getProperty("spring.mvc.servlet.path", "");
        String swaggerPath = environment.getProperty("springdoc.swagger-ui.path", "/swagger-ui/index.html");

        log.info("Swagger UI (local): {}://localhost:{}{}{}{}#/", scheme,
                context.getWebServer().getPort(), normalizePath(contextPath),
                normalizePath(servletPath), normalizePath(swaggerPath));
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank() || "/".equals(path)) {
            return "";
        }
        String normalized = path.startsWith("/") ? path : "/" + path;
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
}
