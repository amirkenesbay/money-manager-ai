import { test } from '@e2e-dev/web'
import { expect } from 'e2e'
import type { Group, Invite, Me } from '../../src/api/types.ts'
import { en } from '../../src/i18n/en.ts'
import { Api, newUserId, openAs, seedGroup } from './session.ts'

test('a group created in the app becomes active with an invite link', async ({ app, screen, agent }) => {
  const api = new Api(app.baseUrl, newUserId())
  await seedGroup(api)
  await openAs(app, screen, api.userId)
  await agent.act('create a new group named {name} with currency US dollar', { params: { name: 'Поездка' } })
  await expect.poll(async () => (await api.json<Group[]>('/groups')).some((group) => group.name === 'Поездка' && group.currency === 'USD')).toBe(true)
  const trip = (await api.json<Group[]>('/groups')).find((group) => group.name === 'Поездка')!
  expect((await api.json<Me>('/me')).activeGroupId).toBe(trip.id)
  expect((await api.categories(trip.id)).map((category) => category.name)).toEqual(expect.arrayContaining(['Продукты', 'Зарплата']))
  const invite = await api.json<Invite>(`/groups/${trip.id}/invite`)
  expect(JSON.stringify(invite)).toContain('t.me/')
})

test('group settings: rename, currency, month start and initial balance', async ({ app, screen, agent }) => {
  const api = new Api(app.baseUrl, newUserId())
  const { group } = await seedGroup(api)
  await openAs(app, screen, api.userId)
  await agent.act('open the settings of the current group and rename it to {name}', { params: { name: 'Дом' } })
  await expect.poll(async () => (await api.group(group.id)).name).toBe('Дом')
  await agent.act('change the group currency to euro')
  await expect.poll(async () => (await api.group(group.id)).currency).toBe('EUR')
  await agent.act('make the month start on day {day}', { params: { day: '10' } })
  await expect.poll(async () => (await api.group(group.id)).monthStartDay).toBe(10)
  await agent.act('set the initial balance of the group to {amount}', { params: { amount: '100000' } })
  await expect.poll(async () => Number((await api.group(group.id)).initialBalance)).toBe(100000)
  await agent.act('go to the home screen')
  await agent.assert('the home screen shows a balance of 100 000 €')
})

test('switching groups changes the active group on the server', async ({ app, screen, agent }) => {
  const api = new Api(app.baseUrl, newUserId())
  const { group: first } = await seedGroup(api, 'KZT', 'Первая')
  const { group: second } = await seedGroup(api, 'USD', 'Вторая')
  expect((await api.json<Me>('/me')).activeGroupId).toBe(second.id)
  await openAs(app, screen, api.userId)
  await agent.act('switch the active group to «Первая»')
  await expect.poll(async () => (await api.json<Me>('/me')).activeGroupId).toBe(first.id)
})

test('language choice and seen hints survive a reload', async ({ app, screen, agent }) => {
  const api = new Api(app.baseUrl, newUserId())
  await seedGroup(api)
  await openAs(app, screen, api.userId)
  await expect.poll(async () => (await api.json<Me>('/me')).seenHints).toContain('WELCOME')
  await agent.act('change the app language to English in the «Ещё» tab')
  await expect.poll(async () => (await api.json<Me>('/me')).language).toBe('en')
  await openAs(app, screen, api.userId, false)
  await expect(screen.getByRole('button', en.tabHome)).toBeVisible()
  await expect(screen.getByRole('dialog', en.welcomeLabel)).toBeHidden()
})
