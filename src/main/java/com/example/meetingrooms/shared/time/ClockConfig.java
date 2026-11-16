package com.example.meetingrooms.shared.time;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Exposes the clock as a bean so time-dependent rules can be tested deterministically.
 * Reservation times are local office time, so "now" must be evaluated in the office time zone
 * ({@code app.time-zone}), not in whatever zone the server or container happens to run.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.time-zone:}") String timeZone) {
        return timeZone.isBlank() ? Clock.systemDefaultZone() : Clock.system(ZoneId.of(timeZone));
    }
}
