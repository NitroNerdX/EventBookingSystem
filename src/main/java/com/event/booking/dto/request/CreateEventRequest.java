package com.event.booking.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CreateEventRequest(
        @NotBlank
        String name,

        @NotBlank
        String venue,

        @NotNull @Future(message = "Event time must be in the future")
        OffsetDateTime eventTime,

        @NotNull @Min(value = 1, message = "Total seats must be at least 1")
        Integer totalSeats,

        @NotNull @DecimalMin(value = "0.0", message = "Price cannot be negative")
        BigDecimal price
) {}
