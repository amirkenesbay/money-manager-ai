import '@fontsource/golos-text/400.css'
import '@fontsource/golos-text/700.css'
import '@fontsource/ibm-plex-mono/400.css'
import '@fontsource/ibm-plex-mono/700.css'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './App'
import './index.css'
import { initTelegram } from './telegram/telegramEnv'
import { applyTheme, systemTheme } from './theme/theme'

const insideTelegram = initTelegram()
if (!insideTelegram) applyTheme(systemTheme())

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App insideTelegram={insideTelegram} />
  </StrictMode>,
)
