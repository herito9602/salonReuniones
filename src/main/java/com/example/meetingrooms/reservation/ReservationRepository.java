package com.example.meetingrooms.reservation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ReservationRepository
        extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    /**
     * True when the room has an active reservation intersecting the half-open interval [start, end).
     */
    @Query("""
            select count(r) > 0 from Reservation r
            where r.room.id = :roomId
              and r.status = :status
              and r.startTime < :end
              and r.endTime > :start
            """)
    boolean existsOverlapping(@Param("roomId") Long roomId,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end,
                              @Param("status") ReservationStatus status);

    @Override
    @EntityGraph(attributePaths = "room")
    Optional<Reservation> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "room")
    Page<Reservation> findAll(Specification<Reservation> spec, Pageable pageable);
}
