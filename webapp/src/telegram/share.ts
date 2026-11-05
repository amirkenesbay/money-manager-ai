import { openExternalLink } from './telegramEnv'

const SHARE_URL = 'https://t.me/share/url'

export function shareToTelegram(text: string, url?: string): void {
  const params = new URLSearchParams(url ? { url, text } : { text })
  openExternalLink(`${SHARE_URL}?${params}`)
}
