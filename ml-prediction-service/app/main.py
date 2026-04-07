from pathlib import Path
import joblib
from fastapi import FastAPI
from pydantic import BaseModel

MODEL_PATH = Path(__file__).resolve().parent / "model" / "abandonment_model.joblib"
model = joblib.load(MODEL_PATH)

app = FastAPI(title="ml-prediction-service")


class PredictionRequest(BaseModel):
    cart_value: float
    cart_size: int
    product_view_count: int
    idle_seconds: int
    exit_intent_count: int
    session_duration_seconds: int


class PredictionResponse(BaseModel):
    abandonment_probability: float
    risk_band: str


@app.get("/health")
def health():
    return {"status": "ok", "service": "ml-prediction-service"}


@app.post("/predict", response_model=PredictionResponse)
def predict(request: PredictionRequest):
    row = [[
        request.cart_value,
        request.cart_size,
        request.product_view_count,
        request.idle_seconds,
        request.exit_intent_count,
        request.session_duration_seconds,
    ]]
    probability = float(model.predict_proba(row)[0][1])

    if probability >= 0.75:
        band = "HIGH"
    elif probability >= 0.45:
        band = "MEDIUM"
    else:
        band = "LOW"

    return PredictionResponse(abandonment_probability=round(probability, 4), risk_band=band)
