package com.retail.ai.session.aggregator.service.api;

import com.retail.ai.session.aggregator.service.model.SessionFeatures;
import com.retail.ai.session.aggregator.service.service.SessionFeatureStore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionFeatureStore sessionFeatureStore;

    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionFeatures> getSession(@PathVariable String sessionId) {
        return sessionFeatureStore.findBySessionId(sessionId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "session-aggregator-service");
    }
}
