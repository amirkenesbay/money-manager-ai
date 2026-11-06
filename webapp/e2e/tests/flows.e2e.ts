import { afterEach, test } from '@e2e-dev/web'
import { expect } from 'e2e'
import { openApp, resetProfile, text } from './app.ts'

afterEach(async ({ browser }) => {
  await resetProfile(browser)
})

test('creates a category in settings', async ({ app, screen, agent }) => {
  await openApp(app, screen)
  await agent.act('open the categories screen from the «Ещё» tab')
  await expect(screen.getByRole('heading', text.categoriesTitle)).toBeVisible()
  await agent.act('create an expense category named {name}', { params: { name: 'Спортзал' } })
  await expect(screen.getByText(text.categoryCreatedToast)).toBeVisible()
  await agent.assert('«Спортзал» is in the list of expense categories')
})

test('switches to the shared group', async ({ app, screen, agent }) => {
  await openApp(app, screen)
  await agent.act('switch the active group to «Семья»')
  await agent.assert('the home screen now shows the group «Семья» with amounts in US dollars')
})

test('switches the interface language to English', async ({ app, screen, agent }) => {
  await openApp(app, screen)
  await agent.act('change the app language to English in the «Ещё» tab')
  await expect(screen.getByRole('button', 'Home')).toBeVisible()
})

test('opens the debts of the shared group', async ({ app, screen, agent }) => {
  await openApp(app, screen)
  await agent.act('switch the active group to «Семья», then open its debts')
  await expect(screen.getByRole('button', 'Айгерим вернёт 30 $ · Семья')).toBeVisible()
  await screen.getByRole('tab', new RegExp(text.debtsTabMine)).tap()
  await agent.assert('the screen now shows a debt of 15 $ owed by me to Данияр')
})

test('finds an operation in the list', async ({ app, screen, agent }) => {
  await openApp(app, screen)
  await screen.getByRole('navigation', text.tabBarLabel).getByRole('button', text.operationsTitle).tap()
  await agent.act('search the operations for {query}', { params: { query: 'такси' } })
  await agent.assert('only taxi operations are listed')
})

test('reports show spending by category', async ({ app, screen, agent }) => {
  await openApp(app, screen)
  await screen.getByRole('navigation', text.tabBarLabel).getByRole('button', text.reportsTitle).tap()
  await agent.assert('the report shows expenses broken down by category with amounts')
})
