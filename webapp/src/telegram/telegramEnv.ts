import { init, isTMA, miniApp, retrieveRawInitData, themeParams } from '@tma.js/sdk-react'

export function initTelegram(): boolean {
  if (!isTMA()) return false
  try {
    init()
    themeParams.mount.ifAvailable()
    themeParams.bindCssVars.ifAvailable()
    miniApp.mount.ifAvailable()
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
