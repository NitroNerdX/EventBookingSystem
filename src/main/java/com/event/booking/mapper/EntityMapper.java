package com.event.booking.mapper;

import com.event.booking.dto.response.*;
import com.event.booking.entity.*;

public class EntityMapper {

    private EntityMapper() {} // no instances — pure utility class

    public static UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole());
    }

    public static EventResponse toEventResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getVenue(),
                event.getEventTime(),
                event.getTotalSeats(),
                event.getAvailableSeats(),
                event.getPrice(),
                event.getOrganizer().getId(),
                event.getOrganizer().getEmail()
        );
    }

    public static BookingResponse toBookingResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getEvent().getId(),
                booking.getEvent().getName(),
                booking.getSeatsBooked(),
                booking.getStatus(),
                booking.getBookedAt()
        );
    }
}
