package com.event.booking.controller;

import com.event.booking.dto.request.CreateEventRequest;
import com.event.booking.dto.request.UpdateEventRequest;
import com.event.booking.dto.response.EventResponse;
import com.event.booking.entity.Event;
import com.event.booking.entity.User;
import com.event.booking.mapper.EntityMapper;
import com.event.booking.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // Public/browse — any authenticated user can view events
    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        List<EventResponse> events = eventService.getAllEvents().stream()
                .map(EntityMapper::toEventResponse)
                .toList();
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable Long id) {
        Event event = eventService.getEventById(id);
        return ResponseEntity.ok(EntityMapper.toEventResponse(event));
    }

    // Organizer-only — enforced at the security layer in Step 10,
    // this controller doesn't need to know about roles at all
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal User organizer) {

        Event event = eventService.createEvent(request, organizer);
        return ResponseEntity.status(HttpStatus.CREATED).body(EntityMapper.toEventResponse(event));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request,
            @AuthenticationPrincipal User requester) {
        Event event = eventService.updateEvent(id, request, requester);
        return ResponseEntity.ok(EntityMapper.toEventResponse(event));
    }

    // Organizer-only — their own events
    @GetMapping("/mine")
    public ResponseEntity<List<EventResponse>> getMyEvents(
            @AuthenticationPrincipal User organizer) {
        List<EventResponse> events = eventService.getEventsByOrganizer(organizer).stream()
                .map(EntityMapper::toEventResponse)
                .toList();
        return ResponseEntity.ok(events);
    }
}