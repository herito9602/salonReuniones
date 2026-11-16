package com.example.meetingrooms.reservation.dto;

import com.example.meetingrooms.reservation.Reservation;
import com.example.meetingrooms.reservation.ReservationStatus;

import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long roomId,
        String roomName,
        String responsible,
        LocalDateTime start,
        LocalDateTime end,
        ReservationStatus status,
        LocalDateTime cancelledAt
) {

    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getRoom().getId(),
                reservation.getRoom().getName(),
                reservation.getResponsible(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus(),
                reservation.getCancelledAt());
    }
}
