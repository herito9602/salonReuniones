package com.example.meetingrooms.reservation;

import com.example.meetingrooms.room.Room;
import com.example.meetingrooms.room.RoomRepository;
import com.example.meetingrooms.shared.error.ConflictException;
import com.example.meetingrooms.shared.error.InvalidRequestException;
import com.example.meetingrooms.shared.error.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final Clock clock;

    public ReservationService(ReservationRepository reservationRepository,
                              RoomRepository roomRepository,
                              Clock clock) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.clock = clock;
    }

    /**
     * Books a room. The room row is locked for the duration of the transaction so two
     * concurrent requests for the same room are serialized and cannot both pass the overlap check.
     */
    @Transactional
    public Reservation create(Long roomId, String responsible, LocalDateTime start, LocalDateTime end) {
        LocalDateTime now = LocalDateTime.now(clock);
        TimeSlot slot = new TimeSlot(start, end);
        if (!slot.start().isAfter(now)) {
            throw new InvalidRequestException("start", "start must be in the future");
        }

        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new NotFoundException("Room %d not found".formatted(roomId)));

        if (reservationRepository.existsOverlapping(roomId, slot.start(), slot.end(), ReservationStatus.ACTIVE)) {
            throw new ConflictException(
                    "Room %d is already booked between %s and %s".formatted(roomId, slot.start(), slot.end()));
        }
        return reservationRepository.save(new Reservation(room, responsible.strip(), slot, now));
    }

    @Transactional
    public Reservation cancel(Long id) {
        Reservation reservation = getById(id);
        reservation.cancel(LocalDateTime.now(clock));
        return reservation;
    }

    @Transactional(readOnly = true)
    public Reservation getById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reservation %d not found".formatted(id)));
    }

    @Transactional(readOnly = true)
    public Page<Reservation> search(Pageable pageable) {
        return reservationRepository.findAll(Specification.unrestricted(), pageable);
    }
}
