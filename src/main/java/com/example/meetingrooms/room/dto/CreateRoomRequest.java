package com.example.meetingrooms.room.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @NotNull(message = "capacity is required")
        @Min(value = 1, message = "capacity must be at least 1")
        @Max(value = 1000, message = "capacity must be at most 1000")
        Integer capacity
) {
}
