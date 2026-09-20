package com.event.booking.service;

import com.event.booking.entity.Booking;
import com.event.booking.entity.Event;
import com.event.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventNotificationService {

    private final BookingRepository bookingRepository;
    private final JavaMailSender mailSender;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm XXX");

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
            String customerEmail = booking.getCustomer().getEmail();

            String eventTime = eventInfo.getEventTime() == null ? "TBD" : eventInfo.getEventTime().format(DATE_FMT);
            String subject = "Update: " + eventInfo.getName() + " — Important information about your booking";

            String html = "<html><body>"
                    + "<p>Dear customer,</p>"
                    + "<p>We are writing to inform you that an event you have a confirmed booking for has been updated. Please review the updated details below:</p>"
                    + "<table style=\"border-collapse:collapse;\">"
                    + "<tr><td style=\"padding:4px 8px; font-weight:600;\">Event</td><td style=\"padding:4px 8px;\">" + escapeHtml(eventInfo.getName()) + "</td></tr>"
                    + "<tr><td style=\"padding:4px 8px; font-weight:600;\">Date &amp; Time</td><td style=\"padding:4px 8px;\">" + eventTime + "</td></tr>"
                    + "<tr><td style=\"padding:4px 8px; font-weight:600;\">Venue</td><td style=\"padding:4px 8px;\">" + escapeHtml(eventInfo.getVenue()) + "</td></tr>"
                    + "<tr><td style=\"padding:4px 8px; font-weight:600;\">Your seats</td><td style=\"padding:4px 8px;\">" + booking.getSeatsBooked() + "</td></tr>"
                    + "</table>"
                    + "<p>If you are unable to attend or wish to modify your booking, please reply to this email or visit your bookings page.</p>"
                    + "<p>Kind regards,<br/>Event Booking Team</p>"
                    + "</body></html>";

            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
                helper.setTo(customerEmail);
                helper.setSubject(subject);
                helper.setText(html, true);

                mailSender.send(mimeMessage);
                log.info("[NOTIFY] Update email sent to {} for event {}", customerEmail, event.getId());
            } catch (MessagingException ex) {
                log.error("[NOTIFY] Failed to construct update email for booking {}", booking.getId(), ex);
            } catch (Exception ex) {
                // Never let one failed send abort the loop — every other customer must still get notified
                log.error("[NOTIFY] Failed to send update email to {} for event {}", customerEmail, event.getId(), ex);
            }
        }
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}