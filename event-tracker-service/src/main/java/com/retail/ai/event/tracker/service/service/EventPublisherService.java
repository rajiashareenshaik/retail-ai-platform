package com.retail.ai.event.tracker.service.service;

import com.retail.ai.event.tracker.service.model.CartEventRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisherService {

    private final KafkaTemplate<String, CartEventRequest> kafkaTemplate;

    @Value("${app.kafka.user-events-topic}")
    private String userEventsTopic;

    public void publish(CartEventRequest request) {
        // We key by session so the downstream consumer sees one session mostly in order.
        kafkaTemplate.send(userEventsTopic, request.getSessionId(), request);
        log.debug("Pushed event {} for session {} to topic {}", request.getEventType(), request.getSessionId(), userEventsTopic);
    }
}
