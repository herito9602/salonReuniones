package com.example.meetingrooms.reservation;

import com.example.meetingrooms.shared.error.InvalidRequestException;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A half-open time interval {@code [start, end)}: a slot ending at 10:00 does not
 * overlap one starting at 10:00, so back-to-back meetings are allowed.
 */
public record TimeSlot(LocalDateTime start, LocalDateTime end) {

    public TimeSlot {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (!end.isAfter(start)) {
            throw new InvalidRequestException("end", "end must be after start");
        }
    }

    public boolean overlaps(TimeSlot other) {
        return start.isBefore(other.end) && end.isAfter(other.start);
    }
}
