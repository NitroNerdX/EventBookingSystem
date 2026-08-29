package com.event.booking.service;

import com.event.booking.dto.request.BookTicketRequest;
import com.event.booking.entity.Booking;
import com.event.booking.entity.BookingStatus;
import com.event.booking.entity.Event;
import com.event.booking.entity.User;
import com.event.booking.exception.DuplicateBookingException;
import com.event.booking.exception.ResourceNotFoundException;
import com.event.booking.exception.SeatUnavailableException;
import com.event.booking.repository.BookingRepository;
import com.event.booking.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final BookingConfirmationService bookingConfirmationService; // Step 11

    @Transactional
    public Booking bookTicket(Long eventId, BookTicketRequest request, User customer) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        if (bookingRepository.existsByCustomerIdAndEventIdAndStatus(
                customer.getId(), eventId, BookingStatus.CONFIRMED)) {
            throw new DuplicateBookingException("You already have a confirmed booking for this event");
        }

        // The atomic decrement from Step 5 — this single call IS the
        // race-condition fix. rowsUpdated == 0 means the WHERE clause
        // failed: not enough seats left, full stop.
        int rowsUpdated = eventRepository.decrementAvailableSeats(eventId, request.seats());
        if (rowsUpdated == 0) {
            throw new SeatUnavailableException("Not enough seats available for this event");
        }

        Booking booking = Booking.builder()
                .event(event)
                .customer(customer)
                .seatsBooked(request.seats())
                .status(BookingStatus.CONFIRMED)
                .bookedAt(OffsetDateTime.now())
                .build();

        Booking saved = bookingRepository.save(booking);

        // Fire Background Task 1 — after the booking is committed
        bookingConfirmationService.sendBookingConfirmation(saved.getId());

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Booking> getBookingsForCustomer(User customer) {
        return bookingRepository.findAllByCustomerIdWithEvent(customer.getId());
    }
}