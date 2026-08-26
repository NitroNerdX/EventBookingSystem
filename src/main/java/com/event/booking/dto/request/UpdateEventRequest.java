package com.event.booking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record UpdateEventRequest(
        String name,
        String venue,

        @Future(message = "Event time must be in the future")
        OffsetDateTime eventTime,

        @DecimalMin(value = "0.0", message = "Price cannot be negative")
        BigDecimal price
) {}