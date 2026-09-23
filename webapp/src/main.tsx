import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './App'
import './index.css'
import { initTelegram } from './telegram/telegramEnv'

const insideTelegram = initTelegram()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App insideTelegram={insideTelegram} />
  </StrictMode>,
)
