package com.event.booking.controller;

import com.event.booking.dto.request.BookTicketRequest;
import com.event.booking.dto.response.BookingResponse;
import com.event.booking.entity.Booking;
import com.event.booking.entity.User;
import com.event.booking.mapper.EntityMapper;
import com.event.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // Customer-only — books tickets for a specific event
    @PostMapping("/api/events/{eventId}/bookings")
    public ResponseEntity<BookingResponse> bookTicket(
            @PathVariable Long eventId,
            @Valid @RequestBody BookTicketRequest request,
            @AuthenticationPrincipal User customer) {
        Booking booking = bookingService.bookTicket(eventId, request, customer);
        return ResponseEntity.status(HttpStatus.CREATED).body(EntityMapper.toBookingResponse(booking));
    }

    // Customer-only — their own booking history
    @GetMapping("/api/bookings/mine")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            @AuthenticationPrincipal User customer) {
        List<BookingResponse> bookings = bookingService.getBookingsForCustomer(customer).stream()
                .map(EntityMapper::toBookingResponse)
                .toList();
        return ResponseEntity.ok(bookings);
    }
}