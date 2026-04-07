package com.retail.ai.ai.orchestrator.service.service;

import com.retail.ai.ai.orchestrator.service.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterventionOrchestratorService {

    private final WebClient webClient;

    @Value("${SESSION_AGGREGATOR_BASE_URL:http://localhost:8082}")
    private String sessionAggregatorBaseUrl;

    @Value("${ML_SERVICE_BASE_URL:http://localhost:8001}")
    private String mlServiceBaseUrl;

    @Value("${GENAI_SERVICE_BASE_URL:http://localhost:8002}")
    private String genAiServiceBaseUrl;

    @Value("${RECOMMENDATION_SERVICE_BASE_URL:http://localhost:8084}")
    private String recommendationServiceBaseUrl;

    @Value("${OFFER_SERVICE_BASE_URL:http://localhost:8085}")
    private String offerServiceBaseUrl;

    public InterventionResponse evaluate(String sessionId) {
        SessionFeatures session = fetchSessionFeatures(sessionId);
        if (session == null) {
            return InterventionResponse.builder()
                    .shouldIntervene(false)
                    .reason("No session state found yet")
                    .message("No intervention needed")
                    .build();
        }

        MlPredictionResponse prediction = score(session);
        boolean ruleTriggered = session.getIdleSeconds() >= 45 || session.getExitIntentCount() > 0;
        boolean shouldIntervene = prediction.getAbandonmentProbability() >= 0.55 || ruleTriggered;

        if (!shouldIntervene) {
            return InterventionResponse.builder()
                    .shouldIntervene(false)
                    .abandonmentProbability(prediction.getAbandonmentProbability())
                    .riskBand(prediction.getRiskBand())
                    .reason("Risk below threshold")
                    .message("Session still looks healthy")
                    .build();
        }

        List<Offer> offers = fetchOffers(session);
        List<Product> recommendations = fetchRecommendations(session.getCartProductIds());
        GenAiResponse genAiResponse = generateMessage(session, recommendations, offers);

        return InterventionResponse.builder()
                .shouldIntervene(true)
                .abandonmentProbability(prediction.getAbandonmentProbability())
                .riskBand(prediction.getRiskBand())
                .offers(offers)
                .recommendations(recommendations)
                .message(genAiResponse.getMessage())
                .reason(Optional.ofNullable(genAiResponse.getRationale()).orElse("Intervention generated"))
                .build();
    }

    private SessionFeatures fetchSessionFeatures(String sessionId) {
        return webClient.get()
                .uri(sessionAggregatorBaseUrl + "/api/sessions/" + sessionId)
                .retrieve()
                .bodyToMono(SessionFeatures.class)
                .onErrorReturn(null)
                .block();
    }

    private MlPredictionResponse score(SessionFeatures session) {
        MlPredictionRequest request = MlPredictionRequest.builder()
                .cart_value(value(session.getCartValue()))
                .cart_size(session.getCartSize())
                .product_view_count(session.getProductViewCount())
                .idle_seconds(session.getIdleSeconds())
                .exit_intent_count(session.getExitIntentCount())
                .session_duration_seconds(session.getSessionDurationSeconds())
                .build();

        MlPredictionResponse fallback = new MlPredictionResponse();
        fallback.setAbandonmentProbability(0.25);
        fallback.setRiskBand("LOW");

        return webClient.post()
                .uri(mlServiceBaseUrl + "/predict")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(MlPredictionResponse.class)
                .onErrorReturn(fallback)
                .block();
    }

    private List<Product> fetchRecommendations(List<String> cartProductIds) {
        return webClient.post()
                .uri(recommendationServiceBaseUrl + "/api/products/recommendations")
                .bodyValue(Optional.ofNullable(cartProductIds).orElseGet(Collections::emptyList))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Product>>() {})
                .onErrorReturn(List.of())
                .block();
    }

    private List<Offer> fetchOffers(SessionFeatures session) {
        String url = offerServiceBaseUrl + "/api/offers/cart/" + session.getSessionId()
                + "?cartSize=" + session.getCartSize()
                + "&cartValue=" + value(session.getCartValue());
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Offer>>() {})
                .onErrorReturn(List.of())
                .block();
    }

    private GenAiResponse generateMessage(SessionFeatures session, List<Product> recommendations, List<Offer> offers) {
        GenAiRequest request = GenAiRequest.builder()
                .session_features(session)
                .recommendations(recommendations)
                .offers(offers)
                .build();

        GenAiResponse fallback = new GenAiResponse();
        fallback.setMessage("You’re close to checkout. Your cart is still ready when you are.");
        fallback.setRationale("Fallback message used because GenAI service was unavailable");

        return webClient.post()
                .uri(genAiServiceBaseUrl + "/interventions/generate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GenAiResponse.class)
                .onErrorReturn(fallback)
                .block();
    }

    private double value(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }
}
