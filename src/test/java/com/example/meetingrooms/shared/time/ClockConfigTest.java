package com.example.meetingrooms.shared.time;

import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClockConfigTest {

    private final ClockConfig config = new ClockConfig();

    @Test
    void usesConfiguredOfficeTimeZone() {
        assertThat(config.clock("Europe/Madrid").getZone()).isEqualTo(ZoneId.of("Europe/Madrid"));
    }

    @Test
    void fallsBackToServerZoneWhenNotConfigured() {
        assertThat(config.clock("").getZone()).isEqualTo(ZoneId.systemDefault());
    }

    @Test
    void failsFastOnInvalidZone() {
        assertThatThrownBy(() -> config.clock("Mars/Olympus")).isInstanceOf(DateTimeException.class);
    }
}
