import os
from typing import List, Dict

try:
    from langchain_openai import ChatOpenAI
except Exception:  # pragma: no cover
    ChatOpenAI = None


def build_grounded_message(session_features: Dict, recommendations: List[Dict], offers: List[Dict]) -> str:
    cart_size = session_features.get("cartSize") or session_features.get("cart_size") or 0
    cart_value = session_features.get("cartValue") or session_features.get("cart_value") or 0

    parts = []
    if offers:
        parts.append(offers[0]["title"])
    else:
        parts.append("Your cart is still ready")

    if recommendations:
        names = ", ".join(item["name"] for item in recommendations[:2])
        parts.append(f"You might also want to look at {names}")

    parts.append(f"You already have {cart_size} item(s) worth about ${cart_value} in the cart")
    return ". ".join(parts) + "."


def generate_message(session_features: Dict, recommendations: List[Dict], offers: List[Dict]) -> str:
    provider = os.getenv("LLM_PROVIDER", "stub").lower()
    api_key = os.getenv("OPENAI_API_KEY")

    if provider != "openai" or not api_key or ChatOpenAI is None:
        return build_grounded_message(session_features, recommendations, offers)

    model = ChatOpenAI(model="gpt-4.1-mini", temperature=0)
    offer_lines = "
".join(f"- {offer['title']}: {offer['description']}" for offer in offers) or "- No discount or shipping offer"
    rec_lines = "
".join(f"- {item['name']} ({item['category']})" for item in recommendations) or "- No recommendations"
    prompt = f"""
You are helping recover an eCommerce cart.
Write one short, grounded message for the shopper.
Do not invent prices, discounts, urgency, inventory, or shipping promises.
Only use the facts below.

Cart summary:
- cart size: {session_features.get('cartSize', session_features.get('cart_size', 0))}
- cart value: {session_features.get('cartValue', session_features.get('cart_value', 0))}

Offers:
{offer_lines}

Recommendations:
{rec_lines}

Return just the message.
""".strip()
    return model.invoke(prompt).content.strip()
