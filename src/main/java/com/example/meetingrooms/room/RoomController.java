package com.example.meetingrooms.room;

import com.example.meetingrooms.room.dto.CreateRoomRequest;
import com.example.meetingrooms.room.dto.RoomResponse;
import com.example.meetingrooms.shared.web.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @SecurityRequirement(name = OpenApiConfig.BASIC_AUTH)
    @PostMapping
    public ResponseEntity<RoomResponse> create(@Valid @RequestBody CreateRoomRequest request) {
        Room room = roomService.create(request.name(), request.capacity());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(room.getId()).toUri();
        return ResponseEntity.created(location).body(RoomResponse.from(room));
    }

    @GetMapping
    public List<RoomResponse> list() {
        return roomService.findAll().stream().map(RoomResponse::from).toList();
    }

    @GetMapping("/{id}")
    public RoomResponse get(@PathVariable Long id) {
        return RoomResponse.from(roomService.getById(id));
    }
}
