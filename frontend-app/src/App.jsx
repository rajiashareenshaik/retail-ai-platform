import React, { useEffect, useMemo, useState } from 'react'

const products = [
  { id: 'sku-100', name: 'Performance Running Shoes', price: 89.99, category: 'footwear' },
  { id: 'sku-101', name: 'Sports Crew Socks', price: 12.99, category: 'accessories' },
  { id: 'sku-102', name: 'Hydration Bottle', price: 18.5, category: 'accessories' },
  { id: 'sku-103', name: 'Yoga Mat', price: 29.0, category: 'fitness' },
  { id: 'sku-104', name: 'Wireless Earbuds', price: 59.99, category: 'electronics' },
  { id: 'sku-105', name: 'Compression Shirt', price: 34.99, category: 'apparel' },
]

const eventBaseUrl = import.meta.env.VITE_EVENT_TRACKER_BASE_URL || 'http://localhost:8081'
const aiBaseUrl = import.meta.env.VITE_AI_ORCHESTRATOR_BASE_URL || 'http://localhost:8083'

function App() {
  const [cart, setCart] = useState([])
  const [pageViews, setPageViews] = useState(0)
  const [intervention, setIntervention] = useState(null)
  const sessionId = useMemo(() => crypto.randomUUID(), [])
  const userId = 'demo-user-001'
  const cartValue = cart.reduce((sum, item) => sum + item.price, 0)

  useEffect(() => {
    const timer = setInterval(() => {
      publishEvent('HEARTBEAT', { idleSeconds: 20, sessionDurationSeconds: Math.floor(performance.now() / 1000) })
    }, 20000)
    return () => clearInterval(timer)
  }, [])

  async function publishEvent(eventType, extra = {}) {
    await fetch(`${eventBaseUrl}/api/events`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionId,
        userId,
        eventType,
        cartProductIds: cart.map(item => item.id),
        cartValue: Number(cartValue.toFixed(2)),
        pageViews,
        quantity: 1,
        sessionDurationSeconds: Math.floor(performance.now() / 1000),
        ...extra,
      })
    }).catch(() => null)
  }

  async function viewProduct(product) {
    const next = pageViews + 1
    setPageViews(next)
    await publishEvent('PRODUCT_VIEW', { productId: product.id, pageViews: next })
  }

  async function addToCart(product) {
    const nextCart = [...cart, product]
    setCart(nextCart)
    await fetch(`${eventBaseUrl}/api/events`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionId,
        userId,
        eventType: 'ADD_TO_CART',
        productId: product.id,
        cartProductIds: nextCart.map(item => item.id),
        cartValue: Number(nextCart.reduce((sum, item) => sum + item.price, 0).toFixed(2)),
        pageViews,
        quantity: 1,
        sessionDurationSeconds: Math.floor(performance.now() / 1000),
      })
    }).catch(() => null)
  }

  async function triggerIntervention() {
    await publishEvent('EXIT_INTENT', { exitIntentCount: 1, idleSeconds: 60 })
    const response = await fetch(`${aiBaseUrl}/api/interventions/evaluate/${sessionId}`, { method: 'POST' })
    const payload = await response.json()
    if (payload.shouldIntervene) {
      setIntervention(payload)
    }
  }

  return (
    <div className="app">
      <div className="header">
        <div>
          <h1>Retail AI Platform</h1>
          <div className="small">Cart recovery demo with rules, ML, and GenAI orchestration</div>
        </div>
        <button className="secondary" onClick={triggerIntervention}>Simulate checkout hesitation</button>
      </div>

      <div className="layout">
        <div>
          <h2>Products</h2>
          <div className="grid">
            {products.map(product => (
              <div className="card" key={product.id} onMouseEnter={() => viewProduct(product)}>
                <h3>{product.name}</h3>
                <div className="small">{product.category}</div>
                <div className="price">${product.price.toFixed(2)}</div>
                <button className="primary" onClick={() => addToCart(product)}>Add to cart</button>
              </div>
            ))}
          </div>
        </div>

        <div className="cart">
          <h2>Cart</h2>
          {cart.length === 0 ? <div className="small">Your cart is empty.</div> : cart.map(item => (
            <div key={item.id} className="recommendation">
              <strong>{item.name}</strong>
              <div className="small">${item.price.toFixed(2)}</div>
            </div>
          ))}
          <div className="price">Total: ${cartValue.toFixed(2)}</div>
          <div className="small">Session: {sessionId}</div>
        </div>
      </div>

      {intervention && (
        <div className="popup-backdrop">
          <div className="popup">
            <h2>Checkout nudge</h2>
            <p>{intervention.message}</p>
            <div className="small">Risk: {intervention.riskBand} ({intervention.abandonmentProbability})</div>

            <h3>Offers</h3>
            {intervention.offers?.map(offer => (
              <div className="offer" key={offer.code}>
                <strong>{offer.title}</strong>
                <div>{offer.description}</div>
              </div>
            ))}

            <h3>Recommendations</h3>
            {intervention.recommendations?.map(product => (
              <div className="recommendation" key={product.id}>
                <strong>{product.name}</strong>
                <div className="small">{product.category}</div>
              </div>
            ))}

            <button className="primary" onClick={() => setIntervention(null)}>Close</button>
          </div>
        </div>
      )}
    </div>
  )
}

export default App
