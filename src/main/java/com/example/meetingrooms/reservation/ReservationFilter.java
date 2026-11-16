package com.example.meetingrooms.reservation;

import java.time.LocalDate;

/**
 * Optional search criteria for reservations; any {@code null} field is ignored.
 */
public record ReservationFilter(Long roomId, LocalDate date, ReservationStatus status) {
}
