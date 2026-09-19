package com.event.booking.service;

import com.event.booking.entity.Booking;
import com.event.booking.entity.Event;
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
    public void notifyCustomersOfEventUpdate(Event event) {
        // The Step 5 repository method — customer already eagerly fetched,
        // so this loop never triggers per-row lazy queries (the N+1 fix)
        List<Booking> bookings = bookingRepository.findConfirmedBookingsWithCustomerByEventId(event.getId());


        if (bookings.isEmpty()) {
            log.info("Event {} updated — no confirmed bookings to notify", event.getId());
            return;
        }

        for (Booking booking : bookings) {
            Event eventInfo = booking.getEvent();
            String message = """
        Hello %s,
        updated event info:
        Event: %s
        Date: %s
        Venue: %s
        """.formatted(
                    booking.getCustomer().getEmail(),
                    eventInfo.getName(),
                    eventInfo.getEventTime(),   // or getStartDate()
                    eventInfo.getVenue()
            );

            log.info("[NOTIFY] Sending update to {}: {}", booking.getCustomer().getEmail(), message);

            // If you have email/sms/notification service:
            // emailService.send(booking.getCustomer().getEmail(), "Event Updated: " + eventInfo.getName(), message);
        }
    }
}