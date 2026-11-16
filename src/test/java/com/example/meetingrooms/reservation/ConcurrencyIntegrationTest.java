package com.example.meetingrooms.reservation;

import com.example.meetingrooms.room.Room;
import com.example.meetingrooms.room.RoomRepository;
import com.example.meetingrooms.room.RoomService;
import com.example.meetingrooms.shared.error.ConflictException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Not {@code @Transactional} on purpose: every booking must run and commit in its own
 * transaction so the row lock on the room is really exercised across threads.
 */
@SpringBootTest
class ConcurrencyIntegrationTest {

    private static final int THREADS = 8;

    @Autowired
    private RoomService roomService;
    @Autowired
    private ReservationService reservationService;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private RoomRepository roomRepository;

    @AfterEach
    void cleanUp() {
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
    }

    @Test
    void onlyOneOfManyConcurrentBookingsForTheSameSlotSucceeds() throws Exception {
        Room room = roomService.create("Concurrency", 4);
        LocalDateTime start = LocalDateTime.of(2099, 6, 1, 10, 0);
        CountDownLatch startSignal = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            String responsible = "user-" + i;
            Callable<Boolean> attempt = () -> {
                startSignal.await();
                try {
                    reservationService.create(room.getId(), responsible, start, start.plusHours(1));
                    return true;
                } catch (ConflictException expected) {
                    return false;
                }
            };
            results.add(executor.submit(attempt));
        }
        startSignal.countDown();

        int successes = 0;
        for (Future<Boolean> result : results) {
            if (result.get(30, TimeUnit.SECONDS)) {
                successes++;
            }
        }
        executor.shutdown();

        assertThat(successes).isEqualTo(1);
        assertThat(reservationRepository.count()).isEqualTo(1);
    }

    @Test
    void onlyOneOfManyConcurrentCancellationsSucceeds() throws Exception {
        Room room = roomService.create("Concurrency", 4);
        LocalDateTime start = LocalDateTime.of(2099, 6, 1, 10, 0);
        Long reservationId = reservationService.create(room.getId(), "owner", start, start.plusHours(1)).getId();
        CountDownLatch startSignal = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            results.add(executor.submit(() -> {
                startSignal.await();
                try {
                    reservationService.cancel(reservationId);
                    return true;
                } catch (ConflictException | ObjectOptimisticLockingFailureException expected) {
                    return false;
                }
            }));
        }
        startSignal.countDown();

        int successes = 0;
        for (Future<Boolean> result : results) {
            if (result.get(30, TimeUnit.SECONDS)) {
                successes++;
            }
        }
        executor.shutdown();

        assertThat(successes).isEqualTo(1);
        assertThat(reservationService.getById(reservationId).getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }
}
