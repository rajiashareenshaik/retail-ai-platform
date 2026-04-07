package com.retail.ai.session.aggregator.service.service;

import com.retail.ai.session.aggregator.service.model.CartEvent;
import com.retail.ai.session.aggregator.service.model.SessionFeatures;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionAggregatorConsumer {

    private final SessionFeatureStore sessionFeatureStore;
    private final KafkaTemplate<String, SessionFeatures> sessionFeaturesKafkaTemplate;

    @Value("${app.kafka.session-features-topic}")
    private String sessionFeaturesTopic;

    @KafkaListener(topics = "${app.kafka.user-events-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(CartEvent event) {
        SessionFeatures features = sessionFeatureStore.findBySessionId(event.getSessionId())
                .orElseGet(SessionFeatures::new);

        features.apply(event);
        sessionFeatureStore.save(features);

        // Publishing normalized features gives us a clean seam if another service wants online features later.
        sessionFeaturesKafkaTemplate.send(sessionFeaturesTopic, features.getSessionId(), features);
        log.info("Updated session={} cartSize={} cartValue={} idleSeconds={}",
                features.getSessionId(), features.getCartSize(), features.getCartValue(), features.getIdleSeconds());
    }
}
