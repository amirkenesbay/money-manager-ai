import { test } from '@e2e-dev/web'
import { expect } from 'e2e'
import { openApp, text } from './app.ts'

async function openNewOperation(screen: Parameters<typeof openApp>[1]) {
  await screen.getByRole('button', text.manualEntryLabel).tap()
  await expect(screen.getByRole('heading', text.newOperationTitle)).toBeVisible()
}

test('records an expense by hand', async ({ app, screen, agent }) => {
  await openApp(app, screen)
  await openNewOperation(screen)
  await screen.getByLabel(text.amountLabel).fill('2500')
  await screen.getByRole('button', /Продукты/).tap()
  await screen.getByRole('button', text.save).tap()
  await expect(screen.getByText(text.savedToast)).toBeVisible()
  await agent.assert('a 2 500 ₸ expense in «Продукты» is now listed among recent operations')
})

test('refuses to save without an amount', async ({ app, screen }) => {
  await openApp(app, screen)
  await openNewOperation(screen)
  await screen.getByRole('button', text.save).tap()
  await expect(screen.getByRole('alert')).toHaveText(text.amountInvalid)
})

test('creates a missing category from the picker', async ({ app, screen }) => {
  await openApp(app, screen)
  await openNewOperation(screen)
  await screen.getByLabel(text.amountLabel).fill('900')
  await screen.getByRole('button', text.addCategory).tap()
  const sheet = screen.getByRole('dialog', text.newCategoryTitle)
  await sheet.getByLabel(text.nameLabel).fill('Кальян')
  await sheet.getByRole('button', '🎬').tap()
  await sheet.getByRole('button', text.save).tap()
  await expect(sheet).toBeHidden()
  await expect(screen.getByRole('button', /Кальян/)).toHaveAttribute('aria-pressed', 'true')
  await screen.getByRole('button', text.save).tap()
  await expect(screen.getByText(text.savedToast)).toBeVisible()
})

test('new category sheet asks for a name', async ({ app, screen }) => {
  await openApp(app, screen)
  await openNewOperation(screen)
  await screen.getByRole('button', text.addCategory).tap()
  const sheet = screen.getByRole('dialog', text.newCategoryTitle)
  await sheet.getByRole('button', text.save).tap()
  await expect(sheet.getByRole('alert')).toHaveText(text.nameRequired)
})

test('creates a missing category while adding a recurring payment', async ({ app, screen, agent }) => {
  await openApp(app, screen)
  await agent.act('open recurring payments from the «Ещё» tab and start adding a new one')
  await expect(screen.getByRole('heading', text.newRecurringTitle)).toBeVisible()
  await screen.getByRole('radio', text.operationType('INCOME')).tap()
  await screen.getByRole('button', text.addCategory).tap()
  const sheet = screen.getByRole('dialog', text.newCategoryTitle)
  await sheet.getByLabel(text.nameLabel).fill('Аренда квартиры')
  await sheet.getByRole('button', text.save).tap()
  await expect(sheet).toBeHidden()
  await expect(screen.getByRole('button', /Аренда квартиры/)).toHaveAttribute('aria-pressed', 'true')
})
