import { test } from '@e2e-dev/web'
import { expect } from 'e2e'
import { launchPath } from './launch.ts'
import { bottomOverflow, forgetSeenHints, text, waitForHome } from './app.ts'

const WELCOME_STEPS = 7

test('welcome tour walks every feature and ends with voice', async ({ app, screen, browser }) => {
  await forgetSeenHints(browser)
  await app.open(launchPath())
  for (let step = 1; step < WELCOME_STEPS; step += 1) {
    await expect(screen.getByText(text.welcomeStep(step, WELCOME_STEPS))).toBeVisible()
    await screen.getByRole('button', text.welcomeNext).tap()
  }
  await expect(screen.getByRole('dialog', text.tourVoiceTitle)).toBeVisible()
  await expect(screen.getByRole('button', text.welcomeTryVoice)).toBeVisible()
})

test('skipping the tour lands on the dashboard', async ({ app, screen, agent, browser }) => {
  await forgetSeenHints(browser)
  await app.open(launchPath())
  await screen.getByRole('button', text.welcomeSkip).tap()
  await expect(screen.getByRole('button', text.welcomeSkip)).toBeHidden()
  await agent.assert('the home screen shows the spending summary of the personal group «Личное» in tenge, with nothing dimmed')
})

test('each screen explains itself once on the first visit', async ({ app, screen, browser }) => {
  await forgetSeenHints(browser)
  await app.open(launchPath())
  await waitForHome(screen)
  await screen.getByRole('button', text.welcomeSkip).tap()
  await screen.getByRole('navigation', text.tabBarLabel).getByRole('button', text.operationsTitle).tap()
  await expect(screen.getByRole('dialog', text.tourTexts.operationsSearch.title)).toBeVisible()
  await screen.getByRole('button', text.welcomeNext).tap()
  await screen.getByRole('button', text.welcomeNext).tap()
  await screen.getByRole('button', text.tourDone).tap()
  await screen.getByRole('navigation', text.tabBarLabel).getByRole('button', text.reportsTitle).tap()
  await expect(screen.getByRole('dialog', text.tourTexts.reportsTabs.title)).toBeVisible()
  await screen.getByRole('button', text.welcomeSkip).tap()
  await screen.getByRole('navigation', text.tabBarLabel).getByRole('button', text.operationsTitle).tap()
  await expect(screen.getByRole('dialog')).toBeHidden()
})

for (const viewport of [{ width: 390, height: 844 }, { width: 360, height: 640 }]) {
  test(`tour card fits on a ${viewport.width}x${viewport.height} phone`, async ({ app, screen, browser }) => {
    await forgetSeenHints(browser)
    await browser.setViewport(viewport)
    await app.open(launchPath())
    for (let step = 1; step < WELCOME_STEPS; step += 1) {
      await expect(screen.getByText(text.welcomeStep(step, WELCOME_STEPS))).toBeVisible()
      expect(await bottomOverflow(browser, text.welcomeNext)).toBe(0)
      await screen.getByRole('button', text.welcomeNext).tap()
    }
  })
}
