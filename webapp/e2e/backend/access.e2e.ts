import { test } from '@e2e-dev/web'
import { expect } from 'e2e'
import { Api, newUserId, openAs, seedGroup, text } from './session.ts'

test('API refuses requests without Telegram data', async ({ app }) => {
  const api = new Api(app.baseUrl, newUserId())
  expect((await api.raw('/me', {}, false)).status).toBe(401)
  expect((await api.raw('/me', { headers: { Authorization: 'tma auth_date=1&hash=00' } }, false)).status).toBe(401)
})

test('a user cannot read or write someone else\'s group', async ({ app }) => {
  const owner = new Api(app.baseUrl, newUserId())
  const stranger = new Api(app.baseUrl, newUserId())
  const { group, categories } = await seedGroup(owner)
  await stranger.json('/me')
  expect([403, 404]).toContain((await stranger.raw(`/groups/${group.id}/operations`)).status)
  const write = await stranger.raw(`/groups/${group.id}/operations`, { method: 'POST', body: JSON.stringify({ type: 'EXPENSE', amount: '1', categoryId: categories[0].id, operationDate: '2026-11-01', description: null }) })
  expect([403, 404]).toContain(write.status)
  expect(await owner.operations(group.id)).toHaveLength(0)
})

test('a brand new user is asked to start the bot first', async ({ app, screen }) => {
  await openAs(app, screen, newUserId(), false)
  await expect(screen.getByText(text.noGroupTitle)).toBeVisible()
})

test('the app outside Telegram asks to open it from Telegram', async ({ app, agent }) => {
  await app.open('/')
  await agent.assert('the page says the app must be opened from Telegram')
})
