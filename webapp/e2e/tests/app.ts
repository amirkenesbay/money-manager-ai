import type { App, Screen } from 'e2e'
import type { Browser, WebRoute } from '@e2e-dev/web'
import { expect } from 'e2e'
import { APP_HINTS } from '../../src/api/types.ts'
import { ru } from '../../src/i18n/ru.ts'
import { DEFAULT_LANGUAGE } from '../../src/i18n/languages.ts'
import { PERSONAL_GROUP_ID } from '../../dev/mockApi.ts'
import { apiAuthorization, launchPath, type MockScenario } from './launch.ts'

export const text = ru

const ME_ENDPOINT = '**/api/v1/me'
const ME_HINTS_ENDPOINT = '**/api/v1/me/hints'
const API_PREFIX = '/api/v1'

export async function waitForHome(screen: Screen): Promise<void> {
  await expect(screen.getByRole('navigation', text.tabBarLabel)).toBeVisible()
}

export async function openApp(app: App, screen: Screen, scenario: MockScenario = 'ok'): Promise<void> {
  await markEveryHintSeen(app.baseUrl)
  await app.open(launchPath(scenario))
  await waitForHome(screen)
}

const jsonHeaders = () => ({ 'Content-Type': 'application/json', Authorization: apiAuthorization() })

async function markEveryHintSeen(baseUrl: string): Promise<void> {
  await Promise.all(APP_HINTS.map((hint) => fetch(`${baseUrl}${API_PREFIX}/me/hints`, { method: 'POST', headers: jsonHeaders(), body: JSON.stringify({ hint }) })))
}

export async function forgetSeenHints(browser: Browser): Promise<void> {
  const seen = new Set<string>()
  const withSeen = async (route: WebRoute, hint?: string) => {
    if (hint) seen.add(hint)
    const me = await (await route.fetch()).json() as Record<string, unknown>
    await route.fulfill({ json: { ...me, seenHints: [...seen] } })
  }
  await browser.route(ME_ENDPOINT, (route) => withSeen(route))
  await browser.route(ME_HINTS_ENDPOINT, (route) => withSeen(route, (JSON.parse(route.request.postData ?? '{}') as { hint?: string }).hint))
}

export async function horizontalOverflow(browser: Browser): Promise<number> {
  return browser.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
}

export async function bottomOverflow(browser: Browser, label: string): Promise<number> {
  return browser.evaluate((name: string) => {
    const button = [...document.querySelectorAll('button')].find((node) => node.textContent?.trim() === name)
    return button ? Math.max(0, button.getBoundingClientRect().bottom - window.innerHeight) : -1
  }, label)
}

export async function resetProfile(baseUrl: string): Promise<void> {
  await fetch(`${baseUrl}${API_PREFIX}/me`, { method: 'PATCH', headers: jsonHeaders(), body: JSON.stringify({ language: DEFAULT_LANGUAGE }) })
  await fetch(`${baseUrl}${API_PREFIX}/groups/${PERSONAL_GROUP_ID}/activate`, { method: 'POST', headers: jsonHeaders() })
}
