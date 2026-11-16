package com.example.meetingrooms.reservation;

import com.example.meetingrooms.reservation.dto.CreateReservationRequest;
import com.example.meetingrooms.reservation.dto.ReservationResponse;
import com.example.meetingrooms.shared.web.PageResponse;
import com.example.meetingrooms.shared.web.OpenApiConfig;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
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
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @SecurityRequirement(name = OpenApiConfig.BASIC_AUTH)
    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody CreateReservationRequest request) {
        Reservation reservation = reservationService.create(
                request.roomId(), request.responsible(), request.start(), request.end());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(reservation.getId()).toUri();
        return ResponseEntity.created(location).body(ReservationResponse.from(reservation));
    }

    /**
     * Paginated search. {@code date} returns reservations overlapping that day; filters combine with AND.
     * Supports {@code page}, {@code size} (max 100) and {@code sort} (e.g. {@code sort=startTime,desc}).
     */
    @GetMapping
    public PageResponse<ReservationResponse> list(
            @Parameter(description = "Only reservations of this room")
            @RequestParam(required = false) @Positive(message = "roomId must be a positive number") Long roomId,
            @Parameter(description = "Only reservations overlapping this day (ISO date, e.g. 2030-01-15)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Only reservations with this status")
            @RequestParam(required = false) ReservationStatus status,
            @ParameterObject
            @PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.ASC) Pageable pageable) {
        ReservationFilter filter = new ReservationFilter(roomId, date, status);
        return PageResponse.from(reservationService.search(filter, pageable), ReservationResponse::from);
    }

    @GetMapping("/{id}")
    public ReservationResponse get(@PathVariable Long id) {
        return ReservationResponse.from(reservationService.getById(id));
    }

    /**
     * Cancellation is modelled as an action rather than a DELETE because the reservation is
     * kept (status CANCELLED) for history; the slot is released for new bookings.
     */
    @SecurityRequirement(name = OpenApiConfig.BASIC_AUTH)
    @PostMapping("/{id}/cancel")
    public ReservationResponse cancel(@PathVariable Long id) {
        return ReservationResponse.from(reservationService.cancel(id));
    }
}
