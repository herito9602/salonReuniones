package com.example.meetingrooms.room.dto;

import com.example.meetingrooms.room.Room;

public record RoomResponse(Long id, String name, int capacity) {

    public static RoomResponse from(Room room) {
        return new RoomResponse(room.getId(), room.getName(), room.getCapacity());
    }
}
