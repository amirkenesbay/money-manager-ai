import { test } from '@e2e-dev/web'
import { expect } from 'e2e'
import { Api, newUserId, openAs, seedGroup, text } from './session.ts'

const FREE_CATEGORIES_PER_TYPE = 10

async function openPicker(screen: Parameters<typeof openAs>[1]) {
  await screen.getByRole('button', text.manualEntryLabel).tap()
  await screen.getByRole('button', text.addCategory).tap()
  return screen.getByRole('dialog', text.newCategoryTitle)
}

test('a category created from the picker is stored and used', async ({ app, screen }) => {
  const api = new Api(app.baseUrl, newUserId())
  const { group } = await seedGroup(api)
  await openAs(app, screen, api.userId)
  await screen.getByRole('button', text.manualEntryLabel).tap()
  await screen.getByLabel(text.amountLabel).fill('800')
  await screen.getByRole('button', text.addCategory).tap()
  const sheet = screen.getByRole('dialog', text.newCategoryTitle)
  await sheet.getByLabel(text.nameLabel).fill('Кофейни')
  await sheet.getByRole('button', '☕').tap()
  await sheet.getByRole('button', text.save).tap()
  await expect(sheet).toBeHidden()
  await screen.getByRole('button', text.save).tap()
  await expect(screen.getByText(text.savedToast)).toBeVisible()

  const created = (await api.categories(group.id)).find((category) => category.name === 'Кофейни')
  expect(created).toMatchObject({ icon: '☕', type: 'EXPENSE' })
  expect((await api.operations(group.id))[0].categoryId).toBe(created?.id)
})

test('the picker refuses a duplicate name with the server message', async ({ app, screen }) => {
  const api = new Api(app.baseUrl, newUserId())
  const { group } = await seedGroup(api)
  await openAs(app, screen, api.userId)
  const sheet = await openPicker(screen)
  await sheet.getByLabel(text.nameLabel).fill('Такси')
  await sheet.getByRole('button', text.save).tap()
  await expect(sheet.getByRole('alert')).toBeVisible()
  await expect(sheet).toBeVisible()
  expect((await api.categories(group.id)).filter((category) => category.name === 'Такси')).toHaveLength(1)
})

test('the picker explains the Free category limit', async ({ app, screen, agent }) => {
  const api = new Api(app.baseUrl, newUserId())
  const { group } = await seedGroup(api)
  const existing = (await api.categories(group.id)).filter((category) => category.type === 'EXPENSE').length
  for (let index = existing; index < FREE_CATEGORIES_PER_TYPE; index += 1) {
    await api.post(`/groups/${group.id}/categories`, { name: `Запас ${index}`, icon: '📦', type: 'EXPENSE' })
  }
  await openAs(app, screen, api.userId)
  const sheet = await openPicker(screen)
  await sheet.getByLabel(text.nameLabel).fill('Одиннадцатая')
  await sheet.getByRole('button', text.save).tap()
  await expect(sheet.getByRole('alert')).toBeVisible()
  await agent.assert('the new category sheet shows an error that the category limit is reached')
  expect((await api.categories(group.id)).filter((category) => category.type === 'EXPENSE')).toHaveLength(FREE_CATEGORIES_PER_TYPE)
})

test('categories screen renames and deletes on the server', async ({ app, screen, agent }) => {
  const api = new Api(app.baseUrl, newUserId())
  const { group } = await seedGroup(api)
  await openAs(app, screen, api.userId)
  await agent.act('open the categories screen from the «Ещё» tab and rename the expense category «Такси» to {name}', { params: { name: 'Поездки' } })
  await expect.poll(async () => (await api.categories(group.id)).map((category) => category.name)).toContain('Поездки')
  await agent.act('delete the category «Поездки», confirming the deletion')
  await expect.poll(async () => (await api.categories(group.id)).map((category) => category.name)).not.toContain('Поездки')
})
