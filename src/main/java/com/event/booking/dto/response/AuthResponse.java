package com.event.booking.dto.response;


public record AuthResponse(
        String token,
        UserResponse user
) {}
