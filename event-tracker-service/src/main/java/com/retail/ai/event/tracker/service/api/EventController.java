package com.retail.ai.event.tracker.service.api;

import com.retail.ai.event.tracker.service.model.CartEventRequest;
import com.retail.ai.event.tracker.service.service.EventPublisherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventPublisherService eventPublisherService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> publishEvent(@Valid @RequestBody CartEventRequest request) {
        log.info("Incoming event type={} sessionId={} userId={}", request.getEventType(), request.getSessionId(), request.getUserId());
        eventPublisherService.publish(request);
        return ResponseEntity.accepted().body(Map.of(
                "status", "accepted",
                "sessionId", request.getSessionId(),
                "eventType", request.getEventType()
        ));
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "event-tracker-service");
    }
}
