package com.event.booking.repository;

import com.event.booking.entity.Booking;
import com.event.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Powers Background Task 2 — event update notification.
    // JOIN FETCH pulls the customer eagerly in the same query, so looping
    // over this list to fire notifications never triggers per-row lazy loads.
    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.customer
        WHERE b.event.id = :eventId AND b.status = 'CONFIRMED'
        """)
    List<Booking> findConfirmedBookingsWithCustomerByEventId(@Param("eventId") Long eventId);

    // Ownership-scoped: a customer's own booking history, event details
    // fetched eagerly since the customer will want to see event name/venue
    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.event
        WHERE b.customer.id = :customerId
        ORDER BY b.bookedAt DESC
        """)
    List<Booking> findAllByCustomerIdWithEvent(@Param("customerId") Long customerId);

    // Backs the partial unique index check at the app layer too — belt and
    // suspenders, since the DB constraint is the real guarantee but a
    // pre-check here lets you return a clean 409 instead of a raw
    // DataIntegrityViolationException bubbling up
    boolean existsByCustomerIdAndEventIdAndStatus(Long customerId, Long eventId, BookingStatus status);
}
