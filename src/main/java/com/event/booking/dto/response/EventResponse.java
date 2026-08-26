package com.event.booking.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record EventResponse(
        Long id,
        String name,
        String venue,
        OffsetDateTime eventTime,
        Integer totalSeats,
        Integer availableSeats,
        BigDecimal price,
        Long organizerId,
        String organizerEmail
) {}