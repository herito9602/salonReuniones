package com.example.meetingrooms.reservation;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Composable filters for the reservation search. A {@code null} argument means "no filter".
 */
final class ReservationSpecifications {

    private ReservationSpecifications() {
    }

    static Specification<Reservation> matching(ReservationFilter filter) {
        return Specification.allOf(
                inRoom(filter.roomId()),
                onDate(filter.date()),
                withStatus(filter.status()));
    }

    static Specification<Reservation> inRoom(Long roomId) {
        return (root, query, cb) -> roomId == null ? null : cb.equal(root.get("room").get("id"), roomId);
    }

    /**
     * Reservations that overlap the given calendar day, i.e. intersect [date 00:00, next day 00:00).
     * A meeting from 23:00 to 01:00 therefore shows up on both days.
     */
    static Specification<Reservation> onDate(LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) {
                return null;
            }
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime nextDayStart = date.plusDays(1).atStartOfDay();
            return cb.and(
                    cb.lessThan(root.get("startTime"), nextDayStart),
                    cb.greaterThan(root.get("endTime"), dayStart));
        };
    }

    static Specification<Reservation> withStatus(ReservationStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }
}
