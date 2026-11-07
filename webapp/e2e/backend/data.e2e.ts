import { writeFileSync, mkdirSync } from 'node:fs'
import { test } from '@e2e-dev/web'
import { expect } from 'e2e'
import { Api, categoryNamed, newUserId, openAs, seedGroup, tabBar, text } from './session.ts'

const IMPORT_FILE = '.e2e/fixtures/import.csv'
const DAY_MS = 86_400_000

const isoDaysAgo = (days: number) => new Date(Date.now() - days * DAY_MS).toISOString().slice(0, 10)

function writeImportFile(): void {
  mkdirSync('.e2e/fixtures', { recursive: true })
  const rows = [
    'Дата;Категория;Расход;Доход;Комментарий',
    `${isoDaysAgo(1)};Продукты;5400;;Магнум`,
    `${isoDaysAgo(2)};Кофейни;1500;;Starbucks`,
    `${isoDaysAgo(3)};Зарплата;;450000;аванс`,
    `${isoDaysAgo(4)};Кофейни;1800;;`,
  ]
  writeFileSync(IMPORT_FILE, rows.join('\n'))
}

test('a CSV from another app is imported with new categories', async ({ app, screen, agent }) => {
  const api = new Api(app.baseUrl, newUserId())
  const { group } = await seedGroup(api)
  writeImportFile()
  await openAs(app, screen, api.userId)
  await tabBar(screen).getByRole('button', text.tabMore).tap()
  await screen.getByRole('button', text.importRow).tap()
  await screen.getByLabel(text.importPick).setInputFiles(IMPORT_FILE)
  await expect(screen.getByText(text.importCount(4), { exact: false })).toBeVisible()
  await agent.act('confirm the import with the suggested categories')
  await expect.poll(async () => (await api.operations(group.id)).length).toBe(4)
  const categoryNames = (await api.categories(group.id)).map((category) => category.name)
  expect(categoryNames).toContain('Кофейни')
  await agent.assert('the app confirms that 4 operations were imported')
})

test('tags and search narrow the operations list', async ({ app, screen, agent }) => {
  const api = new Api(app.baseUrl, newUserId())
  const { group, categories } = await seedGroup(api)
  const add = (amount: string, name: string, description: string) => api.post(`/groups/${group.id}/operations`, { type: 'EXPENSE', amount, categoryId: categoryNamed(categories, name).id, operationDate: isoDaysAgo(0), description })
  await add('9000', 'Продукты', 'ужин #отпуск')
  await add('4000', 'Такси', 'аэропорт #отпуск')
  await add('2500', 'Продукты', 'обед')
  await openAs(app, screen, api.userId)
  await tabBar(screen).getByRole('button', text.operationsTitle).tap()
  await agent.act('filter the operations by the tag {tag}', { params: { tag: 'отпуск' } })
  await agent.assert('only the two #отпуск operations of 9 000 ₸ and 4 000 ₸ are listed')
  await agent.act('clear the filter and search the operations for {query}', { params: { query: 'обед' } })
  await agent.assert('only the 2 500 ₸ «обед» operation is listed')
})

test('undo after recording removes the operation from the server', async ({ app, screen }) => {
  const api = new Api(app.baseUrl, newUserId())
  const { group } = await seedGroup(api)
  await openAs(app, screen, api.userId)
  await screen.getByRole('button', text.manualEntryLabel).tap()
  await screen.getByLabel(text.amountLabel).fill('777')
  await screen.getByRole('button', text.save).tap()
  await expect(screen.getByText(text.savedToast)).toBeVisible()
  await expect.poll(async () => (await api.operations(group.id)).length).toBe(1)
  await screen.getByRole('button', text.undo).tap()
  await expect.poll(async () => (await api.operations(group.id)).length).toBe(0)
})

test('export reports a readable error when the bot cannot reach the chat', async ({ app, screen, agent }) => {
  const api = new Api(app.baseUrl, newUserId())
  const { group, categories } = await seedGroup(api)
  await api.post(`/groups/${group.id}/operations`, { type: 'EXPENSE', amount: '1000', categoryId: categoryNamed(categories, 'Такси').id, operationDate: isoDaysAgo(0), description: null })
  await openAs(app, screen, api.userId)
  await tabBar(screen).getByRole('button', text.tabMore).tap()
  await screen.getByRole('button', text.exportTitle).tap()
  await screen.getByRole('button', text.exportSend).tap()
  await agent.assert('the app shows a short human-readable message, not a raw technical error, and does not claim the file was sent')
})
