package com.event.booking.dto.response;

import com.event.booking.entity.Role;

public record UserResponse(
        Long id,
        String email,
        Role role
) {}
