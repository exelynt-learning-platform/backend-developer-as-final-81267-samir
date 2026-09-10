package com.system.booking.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * JWT signing secret. Must be provided via environment variable JWT_SECRET.
     * No default value is allowed — the application will fail fast at startup
     * if this is missing or blank to prevent insecure deployments.
     */
    private String secret;

    /**
     * Token lifetime in milliseconds. Default: 86400000 (24 hours).
     */
    private long expirationMs = 86400000;

    @PostConstruct
    public void validate() {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException(
                "[SECURITY] app.jwt.secret is not configured. " +
                "Set the JWT_SECRET environment variable to a secure random 256-bit value. " +
                "Example: openssl rand -hex 32"
            );
        }
        if (secret.length() < 32) {
            throw new IllegalStateException(
                "[SECURITY] app.jwt.secret is too short (minimum 32 characters / 256 bits required)."
            );
        }
    }
}
