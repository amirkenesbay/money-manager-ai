import { backButton, init, isTMA, miniApp, openTelegramLink, retrieveRawInitData, swipeBehavior, themeParams, viewport } from '@tma.js/sdk-react'
import { applyTheme } from '../theme/theme'

const APP_HEIGHT_VAR = '--app-height'

export function initTelegram(): boolean {
  if (!isTMA()) return false
  try {
    init()
    themeParams.mount.ifAvailable()
    miniApp.mount.ifAvailable()
    backButton.mount.ifAvailable()
    swipeBehavior.mount.ifAvailable()
    swipeBehavior.disableVertical.ifAvailable()
    followColorScheme()
    miniApp.ready.ifAvailable()
    void mountViewport()
  } catch (error) {
    console.warn('Telegram SDK initialization failed, continuing with default styling', error)
  }
  return true
}

function followColorScheme(): void {
  paintTelegram(miniApp.isDark())
  miniApp.isDark.sub(paintTelegram)
}

function paintTelegram(isDark: boolean): void {
  const background = applyTheme(isDark ? 'dark' : 'light')
  miniApp.setHeaderColor.ifAvailable(background)
  miniApp.setBgColor.ifAvailable(background)
  miniApp.setBottomBarColor.ifAvailable(background)
}

async function mountViewport(): Promise<void> {
  const mounting = viewport.mount.ifAvailable()
  if (!mounting.ok) return
  await mounting.data
  viewport.expand.ifAvailable()
  setAppHeight(viewport.stableHeight())
  viewport.stableHeight.sub(setAppHeight)
}

function setAppHeight(height: number): void {
  if (height > 0) document.documentElement.style.setProperty(APP_HEIGHT_VAR, `${height}px`)
}

export function rawInitData(): string | undefined {
  try {
    return retrieveRawInitData()
  } catch {
    return undefined
  }
}

export function openExternalLink(url: string): void {
  if (openTelegramLink.isAvailable()) {
    openTelegramLink(url)
    return
  }
  window.open(url, '_blank', 'noopener')
}
