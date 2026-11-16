package com.example.meetingrooms.shared.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Credentials of the API user allowed to perform write operations.
 * Override with the APP_USERNAME / APP_PASSWORD environment variables.
 */
@Validated
@ConfigurationProperties(prefix = "app.security.user")
public record ApiUserProperties(@NotBlank String username, @NotBlank String password) {
}
