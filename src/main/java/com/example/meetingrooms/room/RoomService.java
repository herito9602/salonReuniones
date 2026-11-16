package com.example.meetingrooms.room;

import com.example.meetingrooms.shared.error.ConflictException;
import com.example.meetingrooms.shared.error.NotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final Clock clock;

    public RoomService(RoomRepository roomRepository, Clock clock) {
        this.roomRepository = roomRepository;
        this.clock = clock;
    }

    @Transactional
    public Room create(String name, int capacity) {
        String normalizedName = name.strip();
        if (roomRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ConflictException("A room named '%s' already exists".formatted(normalizedName));
        }
        return roomRepository.save(new Room(normalizedName, capacity, LocalDateTime.now(clock)));
    }

    @Transactional(readOnly = true)
    public List<Room> findAll() {
        return roomRepository.findAll(Sort.by("name"));
    }

    @Transactional(readOnly = true)
    public Room getById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Room %d not found".formatted(id)));
    }
}
