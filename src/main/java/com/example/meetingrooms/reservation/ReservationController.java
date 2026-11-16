package com.example.meetingrooms.reservation;

import com.example.meetingrooms.reservation.dto.CreateReservationRequest;
import com.example.meetingrooms.reservation.dto.ReservationResponse;
import com.example.meetingrooms.shared.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody CreateReservationRequest request) {
        Reservation reservation = reservationService.create(
                request.roomId(), request.responsible(), request.start(), request.end());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(reservation.getId()).toUri();
        return ResponseEntity.created(location).body(ReservationResponse.from(reservation));
    }

    @GetMapping
    public PageResponse<ReservationResponse> list(
            @PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.from(reservationService.search(pageable), ReservationResponse::from);
    }

    @GetMapping("/{id}")
    public ReservationResponse get(@PathVariable Long id) {
        return ReservationResponse.from(reservationService.getById(id));
    }

    /**
     * Cancellation is modelled as an action rather than a DELETE because the reservation is
     * kept (status CANCELLED) for history; the slot is released for new bookings.
     */
    @PostMapping("/{id}/cancel")
    public ReservationResponse cancel(@PathVariable Long id) {
        return ReservationResponse.from(reservationService.cancel(id));
    }
}
