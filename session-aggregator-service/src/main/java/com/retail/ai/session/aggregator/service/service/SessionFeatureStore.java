package com.retail.ai.session.aggregator.service.service;

import com.retail.ai.session.aggregator.service.model.SessionFeatures;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SessionFeatureStore {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String PREFIX = "session:features:";

    public void save(SessionFeatures features) {
        redisTemplate.opsForValue().set(PREFIX + features.getSessionId(), features, Duration.ofHours(2));
    }

    public Optional<SessionFeatures> findBySessionId(String sessionId) {
        Object result = redisTemplate.opsForValue().get(PREFIX + sessionId);
        if (result instanceof SessionFeatures features) {
            return Optional.of(features);
        }
        return Optional.empty();
    }
}
