# Retail AI Platform

This project tackles one of the most expensive problems in retail: people fill a cart, leave, and never check out.

Instead of treating that as just a dashboard metric, this repo turns it into a real-time system. We capture user activity, build a lightweight session profile, score abandonment risk, and decide whether to step in with recommendations, bundles, offers, and grounded messaging.

The system intentionally uses a hybrid approach:

- **Rules** catch obvious business cases quickly.
- **ML** estimates how likely a cart is to be abandoned.
- **RAG + LLM orchestration** adds product-aware recommendations and message generation without making up discounts or prices.

## Services

### Java / Spring Boot
- **event-tracker-service**: receives frontend activity and publishes raw events to Kafka.
- **session-aggregator-service**: consumes raw events, computes session features, stores them in Redis, and republishes normalized features.
- **ai-orchestrator-service**: central decision API the frontend calls when it wants intervention advice.
- **recommendation-service**: product catalog and simple fallback recommendation API.
- **offer-service**: business-safe offers and bundles.

### Python / FastAPI
- **ml-prediction-service**: trains and serves the abandonment model.
- **genai-service**: LangChain + LangGraph workflow that retrieves catalog context and generates grounded messaging.

### Frontend
- **frontend-app**: simple React app with a product list, cart page, event tracking, and intervention popup.

### Infra
- **Kafka** for event streaming
- **Redis** for session state
- **Docker Compose** for local orchestration

## End-to-end flow

1. The user browses products and adds items to the cart.
2. The frontend sends events to `event-tracker-service`.
3. `session-aggregator-service` consumes those events, updates Redis, and builds session features.
4. The frontend calls `ai-orchestrator-service` when it wants to know whether to intervene.
5. The orchestrator pulls the latest session profile, calls the ML service, and checks rule-based gates.
6. If the session is risky enough, it fetches safe offers, gets product-aware recommendations, and asks the GenAI workflow to build a grounded message.
7. The frontend shows the intervention popup.

## Why the architecture looks like this

A lot of cart recovery demos skip the awkward parts that matter in production: event ingestion, feature freshness, business guardrails, and wiring multiple systems together. This repo keeps the implementation lightweight, but the edges are realistic:

- Kafka separates event capture from session computation.
- Redis keeps the latest session state cheap to read.
- The orchestrator owns the decision instead of leaking that logic into the UI.
- The LLM is only used after retrieval and offer lookup, so it has less room to drift.

## Repo layout

```text
retail-ai-platform/
├── frontend-app/
├── event-tracker-service/
├── session-aggregator-service/
├── ai-orchestrator-service/
├── recommendation-service/
├── offer-service/
├── ml-prediction-service/
├── genai-service/
├── docker-compose.yml
└── README.md
```

## Local run

### 1) Start everything

```bash
docker compose up --build
```

### 2) Open the UI

```text
http://localhost:3000
```

### 3) Useful service endpoints

- Event tracker health: `GET http://localhost:8081/api/events/health`
- Session features: `GET http://localhost:8082/api/sessions/{sessionId}`
- AI decision: `POST http://localhost:8083/api/interventions/evaluate/{sessionId}`
- Product catalog: `GET http://localhost:8084/api/products`
- Offers: `GET http://localhost:8085/api/offers/cart/{sessionId}`
- ML predict: `POST http://localhost:8001/predict`
- GenAI intervention: `POST http://localhost:8002/interventions/generate`

## Notes on the AI layer

### ML service
The model is intentionally small so the project stays easy to run locally. It uses session-level features like:

- cart value
- cart size
- product view count
- idle seconds
- exit intent count
- session duration

That is enough to show the shape of the system without pretending we trained a miracle model.

### GenAI service
The LangGraph flow is explicit in code:

`START -> fetch session -> score gate input -> retrieve products -> build offer context -> generate message -> END`

The message generator is grounded with:

- retrieved product context
- known cart items
- offers returned by the offer service or orchestrator
- simple guardrails that stop invented discounts or prices

If there is no API key configured, the service falls back to a deterministic local message builder so the rest of the system still works.

## Git setup and push

This repo is ready for git. If you want to push it to GitHub:

```bash
cd retail-ai-platform
git init
git add .
git commit -m "Initial commit: retail AI cart recovery platform"
git branch -M main
git remote add origin https://github.com/<your-username>/retail-ai-platform.git
git push -u origin main
```

## Tech notes

- Spring Boot keeps the Java services consistent and easy to trace.
- FastAPI makes the AI services quick to iterate on.
- LangChain handles retrieval and optional LLM wiring.
- LangGraph keeps the orchestration explicit instead of hiding flow in nested helper calls.

This is still a demo project, but it is wired the way a team could actually extend it.
