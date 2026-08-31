package com.event.booking.service;

import com.event.booking.entity.Booking;
import com.event.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingConfirmationService {
    private final BookingRepository bookingRepository;

    @Async("notificationExecutor")
    public void sendBookingConfirmation(Long bookingId) {
        bookingRepository.findByIdWithEventAndCustomer(bookingId).ifPresentOrElse(
                booking -> log.info("[EMAIL] Sending booking confirmation to customer '{}' for event '{}' ({} seat(s))",
                        booking.getCustomer().getEmail(),
                        booking.getEvent().getName(),
                        booking.getSeatsBooked()),
                () -> log.warn("Booking {} not found for confirmation — skipping", bookingId)
        );
    }
}