import { test } from '@e2e-dev/web'
import { expect } from 'e2e'
import type { Budget, Debts, RecurringList } from '../../src/api/types.ts'
import { Api, categoryNamed, newUserId, openAs, seedGroup } from './session.ts'

const FREE_RECURRING = 3

test('a monthly budget set in the app is tracked by the server', async ({ app, screen, agent }) => {
  const api = new Api(app.baseUrl, newUserId())
  const { group, categories } = await seedGroup(api)
  const today = new Date().toISOString().slice(0, 10)
  await api.post(`/groups/${group.id}/operations`, { type: 'EXPENSE', amount: '40000', categoryId: categoryNamed(categories, 'Продукты').id, operationDate: today, description: null })
  await openAs(app, screen, api.userId)
  await agent.act('open the categories screen from the «Ещё» tab, open «Продукты» and set its monthly limit to {limit}, then save', { params: { limit: '50000' } })
  await expect.poll(async () => (await api.json<Budget[]>(`/groups/${group.id}/budgets`)).map((budget) => [budget.name, Number(budget.limit), Number(budget.spent)])).toEqual([['Продукты', 50000, 40000]])
  await agent.act('go back to the home screen and start a new expense of {amount} in «Продукты»', { params: { amount: '15000' } })
  await agent.assert('the form warns that this expense goes over the «Продукты» budget')
})

const recurringSeed = (categoryId: string, name: string) => ({
  type: 'EXPENSE', amount: '100', categoryId, description: name, unit: 'MONTH', interval: 1, startDate: new Date().toISOString().slice(0, 10), endDate: null, mode: 'REMIND',
})

test('a recurring payment created in the app is stored on the server', async ({ app, screen, agent }) => {
  const api = new Api(app.baseUrl, newUserId())
  const { group, categories } = await seedGroup(api)
  await openAs(app, screen, api.userId)
  await agent.act('open recurring payments from the «Ещё» tab and add a monthly expense of {amount} ₸ in «Такси» named {name}', { params: { amount: '25000', name: 'Абонемент' } })
  await expect.poll(async () => (await api.json<RecurringList>(`/groups/${group.id}/recurring`)).items.length).toBe(1)
  const [payment] = (await api.json<RecurringList>(`/groups/${group.id}/recurring`)).items
  expect(payment).toMatchObject({ type: 'EXPENSE', unit: 'MONTH', categoryId: categoryNamed(categories, 'Такси').id })
  expect(Number(payment.amount)).toBe(25000)
})

test('the recurring limit screen counts the payments that hit the limit', async ({ app, screen, agent }) => {
  const api = new Api(app.baseUrl, newUserId())
  const { group: other, categories: otherCategories } = await seedGroup(api, 'KZT', 'Другая')
  for (let index = 0; index < FREE_RECURRING; index += 1) await api.post(`/groups/${other.id}/recurring`, recurringSeed(otherCategories[0].id, `Платёж ${index}`))
  await seedGroup(api, 'KZT', 'Текущая')
  await openAs(app, screen, api.userId)
  await agent.act('open recurring payments from the «Ещё» tab and tap the button that adds a new recurring payment')
  await agent.assert(`the app says the Free limit of ${FREE_RECURRING} recurring payments is reached and shows ${FREE_RECURRING} of ${FREE_RECURRING} in use`)
})

test('a reminder created in the app is stored on the server', async ({ app, screen, agent }) => {
  const api = new Api(app.baseUrl, newUserId())
  await seedGroup(api)
  await openAs(app, screen, api.userId)
  await agent.act('open reminders from the «Ещё» tab and create a daily reminder at 21:00 named {name}', { params: { name: 'Записать траты' } })
  await expect.poll(async () => (await api.json<{ name: string; hour: number; frequencyType: string }[]>('/notifications')).map((reminder) => [reminder.name, reminder.hour, reminder.frequencyType])).toEqual([['Записать траты', 21, 'DAILY']])
})

test('a personal loan is recorded and repaid', async ({ app, screen, agent }) => {
  const api = new Api(app.baseUrl, newUserId())
  const { group } = await seedGroup(api)
  await openAs(app, screen, api.userId)
  await agent.act('open debts and record that {person} owes me {amount} ₸', { params: { person: 'Ерлан', amount: '20000' } })
  const loans = async () => (await api.json<Debts>(`/groups/${group.id}/debts`)).loans
  await expect.poll(async () => (await loans()).map((loan) => `${loan.person}:${Number(loan.remaining)}`)).toEqual(['Ерлан:20000'])
  await agent.act('mark that Ерлан returned the whole debt')
  await expect.poll(async () => (await loans()).map((loan) => Number(loan.remaining)).filter((remaining) => remaining > 0)).toEqual([])
})
