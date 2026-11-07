import { backButton, closingBehavior, init, isTMA, miniApp, openTelegramLink, retrieveLaunchParams, retrieveRawInitData, swipeBehavior, themeParams, viewport } from '@tma.js/sdk-react'
import { applyTheme } from '../theme/theme'

const APP_HEIGHT_VAR = '--app-height'
const INIT_DATA_USER = 'user'
const LANGUAGE_CODE_LENGTH = 2

export function initTelegram(): boolean {
  if (!isTMA()) return false
  try {
    init()
    themeParams.mount.ifAvailable()
    miniApp.mount.ifAvailable()
    backButton.mount.ifAvailable()
    closingBehavior.mount.ifAvailable()
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

export function telegramPlatform(): string | undefined {
  try {
    return retrieveLaunchParams().tgWebAppPlatform
  } catch {
    return undefined
  }
}

export function launchLanguage(): string {
  const user = new URLSearchParams(rawInitData() ?? '').get(INIT_DATA_USER)
  try {
    return (user ? (JSON.parse(user) as { language_code?: string }).language_code : undefined) ?? navigator.language.slice(0, LANGUAGE_CODE_LENGTH)
  } catch {
    return navigator.language.slice(0, LANGUAGE_CODE_LENGTH)
  }
}

export function openExternalLink(url: string): void {
  if (openTelegramLink.isAvailable()) {
    openTelegramLink(url)
    return
  }
  window.open(url, '_blank', 'noopener')
}
