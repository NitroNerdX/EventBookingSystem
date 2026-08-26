package com.event.booking.dto.response;

import com.event.booking.entity.BookingStatus;

import java.time.OffsetDateTime;

public record BookingResponse(
        Long id,
        Long eventId,
        String eventName,
        Integer seatsBooked,
        BookingStatus status,
        OffsetDateTime bookedAt
) {}