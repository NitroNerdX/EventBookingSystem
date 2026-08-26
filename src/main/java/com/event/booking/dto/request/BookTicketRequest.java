package com.event.booking.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BookTicketRequest(
        @NotNull @Min(value = 1, message = "Must book at least 1 seat")
        Integer seats
) {}
