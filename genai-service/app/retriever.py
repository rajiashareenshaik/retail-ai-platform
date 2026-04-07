from __future__ import annotations

from dataclasses import dataclass
from typing import List, Dict
import numpy as np
from app.catalog import PRODUCTS


@dataclass
class RetrievedProduct:
    id: str
    name: str
    category: str
    price: float
    description: str
    score: float


class SimpleVectorRetriever:
    """
    Small local retriever so the project runs without external embedding services.
    It's intentionally simple: bag-of-words style vectors over the embedded catalog text.
    """

    def __init__(self) -> None:
        vocab = sorted({token for product in PRODUCTS for token in product["text"].lower().split()})
        self.vocab = {word: idx for idx, word in enumerate(vocab)}
        self.matrix = np.vstack([self._embed(product["text"]) for product in PRODUCTS])

    def _embed(self, text: str) -> np.ndarray:
        vector = np.zeros(len(self.vocab), dtype=float)
        for token in text.lower().split():
            if token in self.vocab:
                vector[self.vocab[token]] += 1.0
        norm = np.linalg.norm(vector)
        return vector / norm if norm else vector

    def retrieve(self, query: str, exclude_ids: List[str] | None = None, top_k: int = 3) -> List[Dict]:
        exclude_ids = set(exclude_ids or [])
        query_vector = self._embed(query)
        scores = self.matrix @ query_vector
        scored = []
        for product, score in zip(PRODUCTS, scores):
            if product["id"] in exclude_ids:
                continue
            scored.append(RetrievedProduct(
                id=product["id"],
                name=product["name"],
                category=product["category"],
                price=product["price"],
                description=product["description"],
                score=float(score),
            ))
        scored.sort(key=lambda item: item.score, reverse=True)
        return [item.__dict__ for item in scored[:top_k]]
