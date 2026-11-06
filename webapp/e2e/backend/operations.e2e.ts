import { test } from '@e2e-dev/web'
import { expect } from 'e2e'
import { Api, categoryNamed, newUserId, openAs, seedGroup, tabBar, text } from './session.ts'

test('an expense recorded in the app is stored on the server', async ({ app, screen, agent }) => {
  const api = new Api(app.baseUrl, newUserId())
  const { group, categories } = await seedGroup(api)
  await openAs(app, screen, api.userId)
  await screen.getByRole('button', text.manualEntryLabel).tap()
  await screen.getByLabel(text.amountLabel).fill('4 500')
  await screen.getByRole('button', /Такси/).tap()
  await screen.getByLabel(text.commentLabel).fill('в аэропорт')
  await screen.getByRole('button', text.save).tap()
  await expect(screen.getByText(text.savedToast)).toBeVisible()

  const [stored] = await api.operations(group.id)
  expect(stored).toMatchObject({ type: 'EXPENSE', categoryId: categoryNamed(categories, 'Такси').id, description: 'в аэропорт' })
  expect(Number(stored.amount)).toBe(4500)
  await agent.assert('the home screen shows 4 500 ₸ spent this month')
})

test('income raises the balance', async ({ app, screen, agent }) => {
  const api = new Api(app.baseUrl, newUserId())
  const { group } = await seedGroup(api)
  await openAs(app, screen, api.userId)
  await screen.getByRole('button', text.manualEntryLabel).tap()
  await screen.getByRole('radio', text.operationType('INCOME')).tap()
  await screen.getByLabel(text.amountLabel).fill('300000')
  await expect(screen.getByRole('button', /Зарплата/)).toHaveAttribute('aria-pressed', 'true')
  await screen.getByRole('button', text.save).tap()
  await expect(screen.getByText(text.savedToast)).toBeVisible()
  const [stored] = await api.operations(group.id)
  expect(stored.type).toBe('INCOME')
  await agent.assert('the balance on the home screen is 300 000 ₸')
})

test('editing and deleting an operation reach the server', async ({ app, screen, agent }) => {
  const api = new Api(app.baseUrl, newUserId())
  const { group, categories } = await seedGroup(api)
  const today = new Date().toISOString().slice(0, 10)
  await api.post(`/groups/${group.id}/operations`, { type: 'EXPENSE', amount: '1200', categoryId: categoryNamed(categories, 'Продукты').id, operationDate: today, description: 'хлеб' })
  await openAs(app, screen, api.userId)
  await tabBar(screen).getByRole('button', text.operationsTitle).tap()
  await agent.act('open the 1 200 ₸ «Продукты» operation and change its amount to {amount}, then save', { params: { amount: '1500' } })
  await expect.poll(async () => Number((await api.operations(group.id))[0]?.amount)).toBe(1500)
  await agent.act('open that operation again and delete it, confirming the deletion')
  await expect.poll(async () => (await api.operations(group.id)).length).toBe(0)
})

test('operations added on the server show up in the list and the report', async ({ app, screen, agent }) => {
  const api = new Api(app.baseUrl, newUserId())
  const { group, categories } = await seedGroup(api)
  const today = new Date().toISOString().slice(0, 10)
  for (const [name, amount] of [['Продукты', '7000'], ['Такси', '3000'], ['Такси', '2000']] as const) {
    await api.post(`/groups/${group.id}/operations`, { type: 'EXPENSE', amount, categoryId: categoryNamed(categories, name).id, operationDate: today, description: null })
  }
  await openAs(app, screen, api.userId)
  await tabBar(screen).getByRole('button', text.operationsTitle).tap()
  await agent.assert('three expense operations are listed: 7 000 ₸ Продукты and two Такси of 3 000 ₸ and 2 000 ₸')
  await tabBar(screen).getByRole('button', text.reportsTitle).tap()
  await agent.assert('the report splits 12 000 ₸ of spending into Продукты 7 000 ₸ and Такси 5 000 ₸')
})
