from fastapi import FastAPI
from pydantic import BaseModel
from typing import List, Optional
from app.graph import workflow

app = FastAPI(title="genai-service")


class SessionFeatures(BaseModel):
    sessionId: str
    userId: Optional[str] = None
    cartProductIds: List[str] = []
    cartValue: float = 0
    cartSize: int = 0
    productViewCount: int = 0
    addToCartCount: int = 0
    removeFromCartCount: int = 0
    idleSeconds: int = 0
    exitIntentCount: int = 0
    sessionDurationSeconds: int = 0


class Product(BaseModel):
    id: str
    name: str
    category: str
    price: float
    description: str | None = None


class Offer(BaseModel):
    code: str
    title: str
    description: str
    type: str


class InterventionRequest(BaseModel):
    session_features: SessionFeatures
    recommendations: List[Product] = []
    offers: List[Offer] = []


class InterventionResponse(BaseModel):
    message: str
    rationale: str


@app.get("/health")
def health():
    return {"status": "ok", "service": "genai-service"}


@app.post("/interventions/generate", response_model=InterventionResponse)
def generate_intervention(request: InterventionRequest):
    state = workflow.invoke({
        "session_features": request.session_features.model_dump(),
        "offers": [offer.model_dump() for offer in request.offers],
        # The graph will retrieve its own recommendations, but keeping the field in the API
        # makes it easy for the orchestrator to pass in upstream context if we need it later.
        "recommendations": [product.model_dump() for product in request.recommendations],
    })
    return InterventionResponse(message=state["message"], rationale=state["rationale"])
