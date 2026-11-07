import { test } from '@e2e-dev/web'
import { expect } from 'e2e'
import type { Category } from '../../src/api/types.ts'
import { formatDate } from '../../src/format.ts'
import { Api, newUserId, openAs, seedGroup, tabBar, text } from './session.ts'
import { daysFromNow, sendTribute, setAccess, subscriptionOf, tributeEvent } from './subscription.ts'

const LANGUAGE = 'ru'
const FREE_CATEGORIES_PER_TYPE = 10

async function openPremium(app: Parameters<typeof openAs>[0], screen: Parameters<typeof openAs>[1], userId: number) {
  await openAs(app, screen, userId)
  await tabBar(screen).getByRole('button', text.tabMore).tap()
  await screen.getByRole('button', text.premiumButton).tap()
}

test('a monthly purchase in Tribute turns on Premium in the app and lifts the limits', async ({ app, screen }) => {
  const api = new Api(app.baseUrl, newUserId())
  const { group } = await seedGroup(api)
  expect((await subscriptionOf(api)).tier).toBe('FREE')
  const expiresAt = daysFromNow(30)

  expect((await sendTribute(app.baseUrl, tributeEvent('new_subscription', api.userId, 'monthly', expiresAt))).status).toBe(200)

  const subscription = await subscriptionOf(api)
  expect(subscription.tier).toBe('PAID')
  expect(new Date(subscription.expiresAt ?? '').getTime()).toBe(new Date(expiresAt).getTime())
  const existing = (await api.categories(group.id)).filter((category) => category.type === 'EXPENSE').length
  for (let index = existing; index <= FREE_CATEGORIES_PER_TYPE; index += 1) {
    await api.post<Category>(`/groups/${group.id}/categories`, { name: `Сверх ${index}`, icon: '📦', type: 'EXPENSE' })
  }
  expect((await api.categories(group.id)).filter((category) => category.type === 'EXPENSE').length).toBeGreaterThan(FREE_CATEGORIES_PER_TYPE)

  await openPremium(app, screen, api.userId)
  await expect(screen.getByText(text.premiumActiveBody(formatDate(expiresAt, LANGUAGE)))).toBeVisible()
  await expect(screen.getByRole('button', text.extendPremium)).toBeVisible()
})

test('a yearly renewal extends Premium for a year', async ({ app }) => {
  const api = new Api(app.baseUrl, newUserId())
  await seedGroup(api)
  await sendTribute(app.baseUrl, tributeEvent('new_subscription', api.userId, 'monthly', daysFromNow(30)))
  const yearLater = daysFromNow(395)

  await sendTribute(app.baseUrl, tributeEvent('renewed_subscription', api.userId, 'yearly', yearLater))

  expect(new Date((await subscriptionOf(api)).expiresAt ?? '').getTime()).toBe(new Date(yearLater).getTime())
})

test('a cancelled subscription keeps Premium until the paid date', async ({ app }) => {
  const api = new Api(app.baseUrl, newUserId())
  await seedGroup(api)
  const paidUntil = daysFromNow(20)
  await sendTribute(app.baseUrl, tributeEvent('new_subscription', api.userId, 'monthly', paidUntil))

  await sendTribute(app.baseUrl, tributeEvent('cancelled_subscription', api.userId, 'monthly', paidUntil))

  const subscription = await subscriptionOf(api)
  expect(subscription.tier).toBe('PAID')
  expect(new Date(subscription.expiresAt ?? '').getTime()).toBe(new Date(paidUntil).getTime())
})

test('an already expired period leaves the user on Free', async ({ app }) => {
  const api = new Api(app.baseUrl, newUserId())
  await seedGroup(api)

  await sendTribute(app.baseUrl, tributeEvent('cancelled_subscription', api.userId, 'monthly', daysFromNow(-1)))

  expect((await subscriptionOf(api)).tier).toBe('FREE')
})

test('a forged webhook changes nothing', async ({ app }) => {
  const api = new Api(app.baseUrl, newUserId())
  await seedGroup(api)

  const response = await sendTribute(app.baseUrl, tributeEvent('new_subscription', api.userId, 'yearly', daysFromNow(365)), 'f'.repeat(64))

  expect(response.status).toBe(401)
  expect((await subscriptionOf(api)).tier).toBe('FREE')
})

test('a buyer who never opened the app gets Premium waiting for them', async ({ app }) => {
  const api = new Api(app.baseUrl, newUserId())

  await sendTribute(app.baseUrl, tributeEvent('new_subscription', api.userId, 'yearly', daysFromNow(365)))

  expect((await subscriptionOf(api)).tier).toBe('PAID')
})

test('events that are not subscriptions are accepted and ignored', async ({ app }) => {
  const api = new Api(app.baseUrl, newUserId())
  await seedGroup(api)

  expect((await sendTribute(app.baseUrl, JSON.stringify({ name: 'new_donation', payload: { amount: 100 } }))).status).toBe(200)
  expect((await subscriptionOf(api)).tier).toBe('FREE')
})

test('a whitelisted user has Premium forever with nothing to pay', async ({ app, screen }) => {
  const api = new Api(app.baseUrl, newUserId())
  await seedGroup(api)

  setAccess(api.userId, 'WHITELISTED')

  const subscription = await subscriptionOf(api)
  expect(subscription.tier).toBe('PAID')
  expect(subscription.expiresAt).toBeNull()
  await openPremium(app, screen, api.userId)
  await expect(screen.getByText(text.premiumForeverBody)).toBeVisible()
  await expect(screen.getByRole('button', text.extendPremium)).toBeHidden()
  await expect(screen.getByRole('button', text.subscribePremium)).toBeHidden()
})

test('a blacklisted user is shut out and comes back when unblocked', async ({ app, screen }) => {
  const api = new Api(app.baseUrl, newUserId())
  await seedGroup(api)

  setAccess(api.userId, 'BLOCKED')

  const refused = await api.raw('/me')
  expect(refused.status).toBe(403)
  expect(((await refused.json()) as { code: string }).code).toBe('USER_BLOCKED')
  await openAs(app, screen, api.userId, false)
  await expect(screen.getByText(text.blockedTitle)).toBeVisible()

  setAccess(api.userId, 'REGULAR')
  await openAs(app, screen, api.userId)
  await expect(tabBar(screen)).toBeVisible()
})
