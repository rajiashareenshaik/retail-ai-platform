from pathlib import Path
import joblib
import numpy as np
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

MODEL_PATH = Path(__file__).resolve().parent / "model" / "abandonment_model.joblib"
MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)


def build_dataset():
    # The goal here isn't perfect accuracy. We just want a dataset that behaves like the signals
    # a retail team would actually care about: high idle time, exit intent, tiny carts, and short sessions.
    rows = []
    labels = []

    for cart_value in [20, 35, 50, 75, 120, 180]:
        for cart_size in [1, 2, 3, 4]:
            for product_view_count in [2, 4, 7, 10]:
                for idle_seconds in [5, 20, 45, 90]:
                    for exit_intent_count in [0, 1, 2]:
                        for session_duration_seconds in [60, 180, 420, 900]:
                            risk_score = 0
                            risk_score += 2 if idle_seconds >= 45 else 0
                            risk_score += 2 if exit_intent_count > 0 else 0
                            risk_score += 1 if cart_size == 1 else 0
                            risk_score += 1 if cart_value < 40 else 0
                            risk_score += 1 if product_view_count > 6 and cart_size <= 1 else 0
                            risk_score -= 1 if cart_value >= 100 else 0
                            risk_score -= 1 if cart_size >= 3 else 0
                            label = 1 if risk_score >= 3 else 0
                            rows.append([
                                cart_value,
                                cart_size,
                                product_view_count,
                                idle_seconds,
                                exit_intent_count,
                                session_duration_seconds,
                            ])
                            labels.append(label)

    return np.array(rows, dtype=float), np.array(labels)


def train():
    X, y = build_dataset()
    model = Pipeline([
        ("scaler", StandardScaler()),
        ("classifier", LogisticRegression(max_iter=500)),
    ])
    model.fit(X, y)
    joblib.dump(model, MODEL_PATH)
    print(f"saved model to {MODEL_PATH}")


if __name__ == "__main__":
    train()
