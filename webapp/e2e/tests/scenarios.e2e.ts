import { test } from '@e2e-dev/web'
import { expect } from 'e2e'
import { launchPath } from './launch.ts'
import { openApp, text, waitForHome } from './app.ts'

test('user without a group is sent to the bot', async ({ app, screen }) => {
  await app.open(launchPath('nogroup'))
  await expect(screen.getByText(text.noGroupTitle)).toBeVisible()
})

test('server error offers a retry', async ({ app, screen }) => {
  await app.open(launchPath('error'))
  await expect(screen.getByRole('button', text.retry)).toBeVisible()
})

test('retry recovers from a flaky server', async ({ app, screen }) => {
  await app.open(launchPath('flaky'))
  await screen.getByRole('button', text.retry).tap()
  await waitForHome(screen)
})

test('outside Telegram the app asks to open it from Telegram', async ({ app, agent }) => {
  await app.open('/')
  await agent.assert('the page says the app must be opened from Telegram')
})

test('empty group shows an empty dashboard', async ({ app, screen, agent }) => {
  await openApp(app, screen, 'empty')
  await agent.assert('the home screen has no operations yet and invites to record the first one')
})
