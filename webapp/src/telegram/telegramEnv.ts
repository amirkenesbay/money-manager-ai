import { backButton, init, isTMA, miniApp, openTelegramLink, retrieveRawInitData, themeParams } from '@tma.js/sdk-react'

export function initTelegram(): boolean {
  if (!isTMA()) return false
  try {
    init()
    themeParams.mount.ifAvailable()
    themeParams.bindCssVars.ifAvailable()
    miniApp.mount.ifAvailable()
    backButton.mount.ifAvailable()
    miniApp.ready.ifAvailable()
  } catch (error) {
    console.warn('Telegram SDK initialization failed, continuing with default styling', error)
  }
  return true
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
