package com.retail.ai.ai.orchestrator.service.api;

import com.retail.ai.ai.orchestrator.service.model.InterventionResponse;
import com.retail.ai.ai.orchestrator.service.service.InterventionOrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interventions")
@RequiredArgsConstructor
public class InterventionController {

    private final InterventionOrchestratorService interventionOrchestratorService;

    @PostMapping("/evaluate/{sessionId}")
    public InterventionResponse evaluate(@PathVariable String sessionId) {
        return interventionOrchestratorService.evaluate(sessionId);
    }
}
