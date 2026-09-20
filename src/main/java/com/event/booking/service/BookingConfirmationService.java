package com.event.booking.service;

import com.event.booking.entity.Booking;
import com.event.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingConfirmationService {

    private final BookingRepository bookingRepository;
    private final JavaMailSender mailSender;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm XXX");

    @Async("notificationExecutor")
    public void sendBookingConfirmation(Long bookingId) {
        bookingRepository.findByIdWithEventAndCustomer(bookingId).ifPresentOrElse(booking -> {
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

                String customerEmail = booking.getCustomer().getEmail();
                String eventName = booking.getEvent().getName();
                String eventTime = booking.getEvent().getEventTime() == null ? "TBD" : booking.getEvent().getEventTime().format(DATE_FMT);
                String venue = booking.getEvent().getVenue();
                Integer seats = booking.getSeatsBooked();
                BigDecimal pricePerSeat = booking.getEvent().getPrice();
                String total = pricePerSeat == null ? "N/A" : pricePerSeat.multiply(BigDecimal.valueOf(seats)).toString();

                String subject = "Booking Confirmation — " + eventName + " (Booking #" + booking.getId() + ")";

                String html = "<html><body>"
                        + "<p>Dear customer,</p>"
                        + "<p>Thank you for your booking. Below are the details of your reservation:</p>"
                        + "<ul>"
                        + "<li><strong>Booking ID:</strong> " + booking.getId() + "</li>"
                        + "<li><strong>Event:</strong> " + escapeHtml(eventName) + "</li>"
                        + "<li><strong>Date &amp; Time:</strong> " + eventTime + "</li>"
                        + "<li><strong>Venue:</strong> " + escapeHtml(venue) + "</li>"
                        + "<li><strong>Seats:</strong> " + seats + "</li>"
                        + "<li><strong>Total Paid:</strong> " + total + "</li>"
                        + "</ul>"
                        + "<p>Please bring a photo ID when attending. If you need to cancel or modify your booking, reply to this email or visit your bookings page.</p>"
                        + "<p>Regards,<br/>Event Booking Team</p>"
                        + "</body></html>";

                helper.setTo(customerEmail);
                helper.setSubject(subject);
                helper.setText(html, true);

                mailSender.send(mimeMessage);

                log.info("Confirmation email sent to {} for bookingId={}", customerEmail, booking.getId());
            } catch (MessagingException ex) {
                log.error("Failed to construct confirmation email for booking {}", bookingId, ex);
            } catch (Exception ex) {
                // Log full stack so SMTP/auth problems can be diagnosed
                log.error("Failed to send confirmation email for booking {}", bookingId, ex);
            }
        }, () -> log.warn("Booking {} not found for confirmation — skipping", bookingId));
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}