package com.event.booking.service;

import com.event.booking.dto.request.CreateEventRequest;
import com.event.booking.dto.request.UpdateEventRequest;
import com.event.booking.entity.Event;
import com.event.booking.entity.User;
import com.event.booking.exception.ResourceNotFoundException;
import com.event.booking.exception.UnauthorizedAccessException;
import com.event.booking.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventNotificationService eventNotificationService;

    @Transactional
    public Event createEvent(CreateEventRequest request, User organizer) {
        Event event = Event.builder()
                .organizer(organizer)
                .name(request.name())
                .venue(request.venue())
                .eventTime(request.eventTime())
                .totalSeats(request.totalSeats())
                .availableSeats(request.totalSeats())   // starts full
                .price(request.price())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        return eventRepository.save(event);
    }

    @Transactional
    public Event updateEvent(Long eventId, UpdateEventRequest request, User requester) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        // Ownership check — the IDOR-safety line. Never trust an ID alone.
        if (!event.getOrganizer().getId().equals(requester.getId())) {
            throw new UnauthorizedAccessException("You do not own this event");
        }

        // Patch-style merge: null in the request means "leave unchanged"
        if (request.name() != null) event.setName(request.name());
        if (request.venue() != null) event.setVenue(request.venue());
        if (request.eventTime() != null) event.setEventTime(request.eventTime());
        if (request.price() != null) event.setPrice(request.price());
        event.setUpdatedAt(OffsetDateTime.now());

        try {
            Event saved = eventRepository.save(event);
            // Fire Background Task 2 — after the update is committed
            eventNotificationService.notifyCustomersOfEventUpdate(saved.getId());
            return saved;
        } catch (ObjectOptimisticLockingFailureException ex) {
            // @Version mismatch — someone else updated this event concurrently
            throw new IllegalStateException(
                    "Event was modified by another request, please retry", ex);
        }
    }

    @Transactional(readOnly = true)
    public Event getEventById(Long eventId) {
        return eventRepository.findByIdWithOrganizer(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
    }

    @Transactional(readOnly = true)
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Event> getEventsByOrganizer(User organizer) {
        // Ownership-scoped by construction — no way to leak another
        // organizer's events through this method
        return eventRepository.findAllByOrganizerId(organizer.getId());
    }
}