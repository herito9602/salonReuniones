package com.example.meetingrooms.reservation;

import com.example.meetingrooms.room.Room;
import com.example.meetingrooms.room.RoomRepository;
import com.example.meetingrooms.shared.error.ConflictException;
import com.example.meetingrooms.shared.error.InvalidRequestException;
import com.example.meetingrooms.shared.error.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2030, 1, 15, 9, 0);
    private static final Long ROOM_ID = 1L;

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private RoomRepository roomRepository;

    private ReservationService service;
    private final Room room = new Room("Orion", 8, NOW);

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new ReservationService(reservationRepository, roomRepository, fixedClock);
    }

    @Test
    void createsReservationWhenSlotIsFree() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(reservationRepository.existsOverlapping(ROOM_ID, NOW.plusHours(1), NOW.plusHours(2), ReservationStatus.ACTIVE))
                .thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        Reservation created = service.create(ROOM_ID, "  Ada Lovelace ", NOW.plusHours(1), NOW.plusHours(2));

        assertThat(created.getRoom()).isSameAs(room);
        assertThat(created.getResponsible()).isEqualTo("Ada Lovelace");
        assertThat(created.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(created.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsOverlappingReservationWithConflict() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(reservationRepository.existsOverlapping(ROOM_ID, NOW.plusHours(1), NOW.plusHours(2), ReservationStatus.ACTIVE))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(ROOM_ID, "Ada", NOW.plusHours(1), NOW.plusHours(2)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already booked");
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void rejectsReservationStartingInThePast() {
        assertThatThrownBy(() -> service.create(ROOM_ID, "Ada", NOW.minusMinutes(1), NOW.plusHours(1)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("start must be in the future");
        verifyNoInteractions(roomRepository, reservationRepository);
    }

    @Test
    void rejectsReservationEndingBeforeItStarts() {
        assertThatThrownBy(() -> service.create(ROOM_ID, "Ada", NOW.plusHours(2), NOW.plusHours(1)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("end must be after start");
        verifyNoInteractions(roomRepository, reservationRepository);
    }

    @Test
    void rejectsReservationForUnknownRoom() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(ROOM_ID, "Ada", NOW.plusHours(1), NOW.plusHours(2)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void cancelsActiveReservationAndRejectsSecondCancellation() {
        Reservation reservation = new Reservation(room, "Ada", new TimeSlot(NOW.plusHours(1), NOW.plusHours(2)), NOW);
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));

        Reservation cancelled = service.cancel(10L);

        assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(cancelled.getCancelledAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> service.cancel(10L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    void rejectsCancellingFinishedReservation() {
        Reservation finished = new Reservation(room, "Ada",
                new TimeSlot(NOW.minusHours(2), NOW.minusHours(1)), NOW.minusDays(1));
        when(reservationRepository.findById(11L)).thenReturn(Optional.of(finished));

        assertThatThrownBy(() -> service.cancel(11L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already finished");
    }
}
