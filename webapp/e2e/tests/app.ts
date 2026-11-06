import type { App, Screen } from 'e2e'
import type { Browser } from '@e2e-dev/web'
import { expect } from 'e2e'
import { ru } from '../../src/i18n/ru.ts'
import { DEFAULT_LANGUAGE } from '../../src/i18n/languages.ts'
import { PERSONAL_GROUP_ID } from '../../dev/mockApi.ts'
import { apiAuthorization, launchPath, type MockScenario } from './launch.ts'

export const text = ru

const ME_ENDPOINT = '**/api/v1/me'

export async function waitForHome(screen: Screen): Promise<void> {
  await expect(screen.getByRole('navigation', text.tabBarLabel)).toBeVisible()
}

export async function openApp(app: App, screen: Screen, scenario: MockScenario = 'ok'): Promise<void> {
  await app.open(launchPath(scenario))
  await waitForHome(screen)
  const tour = screen.getByRole('dialog', text.welcomeLabel)
  if (await tour.isVisible()) {
    await tour.getByRole('button', text.welcomeSkip).tap()
    await expect(tour).toBeHidden()
  }
}

export async function forgetSeenHints(browser: Browser): Promise<void> {
  await browser.route(ME_ENDPOINT, async (route) => {
    const upstream = await route.fetch()
    const me = await upstream.json() as Record<string, unknown>
    await route.fulfill({ json: { ...me, seenHints: [] } })
  })
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

export async function resetProfile(browser: Browser): Promise<void> {
  await browser.evaluate(async ({ groupId, language, authorization }: { groupId: string; language: string; authorization: string }) => {
    const headers = { 'Content-Type': 'application/json', Authorization: authorization }
    await fetch('/api/v1/me', { method: 'PATCH', headers, body: JSON.stringify({ language }) })
    await fetch(`/api/v1/groups/${groupId}/activate`, { method: 'POST', headers })
  }, { groupId: PERSONAL_GROUP_ID, language: DEFAULT_LANGUAGE, authorization: apiAuthorization() })
}
