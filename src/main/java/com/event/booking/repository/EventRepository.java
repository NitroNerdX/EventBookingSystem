package com.event.booking.repository;

import com.event.booking.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    // Fetch event + organizer in one query — avoids N+1 when you need
    // to show organizer info alongside event details
    @Query("SELECT e FROM Event e JOIN FETCH e.organizer WHERE e.id = :id")
    Optional<Event> findByIdWithOrganizer(@Param("id") Long id);

    // The atomic seat decrement — the whole race-condition fix in one query.
    // Returns the number of rows updated: 1 = success, 0 = not enough seats.
    @Modifying
    @Transactional
    @Query("""
        UPDATE Event e
        SET e.availableSeats = e.availableSeats - :seats
        WHERE e.id = :eventId
          AND e.availableSeats >= :seats
        """)
    int decrementAvailableSeats(@Param("eventId") Long eventId, @Param("seats") Integer seats);

    // For releasing seats back on booking cancellation
    @Modifying
    @Transactional
    @Query("""
        UPDATE Event e
        SET e.availableSeats = e.availableSeats + :seats
        WHERE e.id = :eventId
        """)
    void incrementAvailableSeats(@Param("eventId") Long eventId, @Param("seats") Integer seats);

    // Ownership-scoped listing for organizers — the IDOR-safety backbone.
    // Service layer calls this instead of findById + manual check, so an
    // organizer literally cannot fetch another organizer's event this way.
    List<Event> findAllByOrganizerId(Long organizerId);
}