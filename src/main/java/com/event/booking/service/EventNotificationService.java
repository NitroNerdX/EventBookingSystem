package com.event.booking.service;

import com.event.booking.entity.Booking;
import com.event.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventNotificationService {

    private final BookingRepository bookingRepository;

    @Async("notificationExecutor")
    @Transactional(readOnly = true)
    public void notifyCustomersOfEventUpdate(Long eventId) {
        // The Step 5 repository method — customer already eagerly fetched,
        // so this loop never triggers per-row lazy queries (the N+1 fix)
        List<Booking> bookings = bookingRepository.findConfirmedBookingsWithCustomerByEventId(eventId);

        if (bookings.isEmpty()) {
            log.info("Event {} updated — no confirmed bookings to notify", eventId);
            return;
        }

        for (Booking booking : bookings) {
            log.info("[NOTIFY] Notifying customer '{}' --- event '{}' has been updated",
                    booking.getCustomer().getEmail(),
                    booking.getEvent().getName());
        }
    }
}