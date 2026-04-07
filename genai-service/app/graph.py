from typing import TypedDict, List, Dict, Any
from langgraph.graph import StateGraph, START, END
from app.retriever import SimpleVectorRetriever
from app.llm import generate_message

retriever = SimpleVectorRetriever()


class InterventionState(TypedDict, total=False):
    session_features: Dict[str, Any]
    offers: List[Dict[str, Any]]
    recommendations: List[Dict[str, Any]]
    message: str
    rationale: str
    query: str


def fetch_session_data(state: InterventionState) -> InterventionState:
    session = state["session_features"]
    cart_ids = session.get("cartProductIds", [])
    category_hint = " ".join(cart_ids) if cart_ids else "cart checkout retail accessories"
    state["query"] = f"{category_hint} fitness running accessories retail cart"
    return state


def retrieve_products(state: InterventionState) -> InterventionState:
    session = state["session_features"]
    cart_ids = session.get("cartProductIds", [])
    state["recommendations"] = retriever.retrieve(state["query"], exclude_ids=cart_ids, top_k=3)
    return state


def build_message(state: InterventionState) -> InterventionState:
    state["message"] = generate_message(
        session_features=state["session_features"],
        recommendations=state.get("recommendations", []),
        offers=state.get("offers", []),
    )
    state["rationale"] = "Generated from session features, safe offers, and retrieved catalog matches"
    return state


def build_graph():
    graph = StateGraph(InterventionState)
    graph.add_node("fetch_session_data", fetch_session_data)
    graph.add_node("retrieve_products", retrieve_products)
    graph.add_node("build_message", build_message)

    graph.add_edge(START, "fetch_session_data")
    graph.add_edge("fetch_session_data", "retrieve_products")
    graph.add_edge("retrieve_products", "build_message")
    graph.add_edge("build_message", END)
    return graph.compile()


workflow = build_graph()
