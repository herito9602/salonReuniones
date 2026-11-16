package com.example.meetingrooms.reservation;

import com.example.meetingrooms.shared.error.InvalidRequestException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeSlotTest {

    private static final LocalDateTime DAY = LocalDateTime.of(2099, 1, 15, 0, 0);

    // Existing slot is always 10:00-11:00.
    @ParameterizedTest(name = "[{0}:00, {1}:00) overlaps 10-11 -> {2}")
    @CsvSource({
            "9, 10, false",   // ends exactly when the other starts
            "11, 12, false",  // starts exactly when the other ends
            "9, 11, true",    // overlaps the start
            "10, 12, true",   // overlaps the end
            "10, 11, true",   // identical
            "8, 13, true",    // contains
    })
    void detectsOverlapsUsingHalfOpenIntervals(int startHour, int endHour, boolean expected) {
        TimeSlot existing = slot(10, 11);

        assertThat(slot(startHour, endHour).overlaps(existing)).isEqualTo(expected);
        assertThat(existing.overlaps(slot(startHour, endHour))).isEqualTo(expected);
    }

    @Test
    void rejectsEndNotAfterStart() {
        assertThatThrownBy(() -> slot(11, 11))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("end must be after start");
        assertThatThrownBy(() -> slot(11, 10))
                .isInstanceOf(InvalidRequestException.class);
    }

    private static TimeSlot slot(int startHour, int endHour) {
        return new TimeSlot(DAY.withHour(startHour), DAY.withHour(endHour));
    }
}
