package com.example.meetingrooms.reservation;

import com.example.meetingrooms.room.Room;
import com.example.meetingrooms.shared.error.ConflictException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

/**
 * A booking of a room for a time slot. Cancelling is a state change (soft delete) so the
 * booking history is preserved and a cancelled slot becomes available again.
 */
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false, updatable = false)
    private Room room;

    @Column(nullable = false, length = 100)
    private String responsible;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Version
    private long version;

    protected Reservation() {
        // required by JPA
    }

    public Reservation(Room room, String responsible, TimeSlot slot, LocalDateTime createdAt) {
        this.room = room;
        this.responsible = responsible;
        this.startTime = slot.start();
        this.endTime = slot.end();
        this.status = ReservationStatus.ACTIVE;
        this.createdAt = createdAt;
    }

    public void cancel(LocalDateTime now) {
        if (status == ReservationStatus.CANCELLED) {
            throw new ConflictException("Reservation %d is already cancelled".formatted(id));
        }
        if (!endTime.isAfter(now)) {
            throw new ConflictException("Reservation %d has already finished and cannot be cancelled".formatted(id));
        }
        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = now;
    }

    public Long getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public String getResponsible() {
        return responsible;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
}
