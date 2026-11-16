package com.example.meetingrooms.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateReservationRequest(
        @NotNull(message = "roomId is required")
        @Positive(message = "roomId must be a positive number")
        Long roomId,

        @NotBlank(message = "responsible is required")
        @Size(max = 100, message = "responsible must be at most 100 characters")
        String responsible,

        @Schema(example = "2030-01-15T10:00:00", description = "Local office time, ISO-8601")
        @NotNull(message = "start is required")
        LocalDateTime start,

        @Schema(example = "2030-01-15T11:00:00", description = "Local office time, ISO-8601; must be after start")
        @NotNull(message = "end is required")
        LocalDateTime end
) {
}
