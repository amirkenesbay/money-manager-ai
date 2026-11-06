import { test } from '@e2e-dev/web'
import { expect } from 'e2e'
import { launchPath } from './launch.ts'
import { bottomOverflow, forgetSeenHints, text } from './app.ts'

test('welcome tour ends with an offer to try voice', async ({ app, screen, browser }) => {
  await forgetSeenHints(browser)
  await app.open(launchPath())
  const tour = screen.getByRole('dialog', text.welcomeLabel)
  await expect(tour.getByText(text.welcomeVoiceTitle)).toBeVisible()
  await tour.getByRole('button', text.welcomeNext).tap()
  await expect(tour.getByText(text.welcomeBalanceTitle)).toBeVisible()
  await tour.getByRole('button', text.welcomeNext).tap()
  await expect(tour.getByText(text.welcomeTogetherTitle)).toBeVisible()
  await expect(tour.getByRole('button', text.welcomeTryVoice)).toBeVisible()
})

test('skipping the tour lands on the dashboard', async ({ app, screen, agent, browser }) => {
  await forgetSeenHints(browser)
  await app.open(launchPath())
  await screen.getByRole('button', text.welcomeSkip).tap()
  await expect(screen.getByRole('dialog', text.welcomeLabel)).toBeHidden()
  await agent.assert('the home screen shows the balance of the personal group «Личное» in tenge')
})

for (const viewport of [{ width: 390, height: 844 }, { width: 360, height: 640 }]) {
  test(`welcome tour buttons fit on a ${viewport.width}x${viewport.height} phone`, async ({ app, screen, browser }) => {
    await forgetSeenHints(browser)
    await browser.setViewport(viewport)
    await app.open(launchPath())
    await expect(screen.getByRole('button', text.welcomeNext)).toBeVisible()
    await app.screenshot('tour')
    expect(await bottomOverflow(browser, text.welcomeNext)).toBe(0)
    expect(await bottomOverflow(browser, text.welcomeSkip)).toBe(0)
  })
}
