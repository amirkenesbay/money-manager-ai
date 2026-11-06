import { test } from '@e2e-dev/web'
import { expect } from 'e2e'
import { horizontalOverflow, openApp, text } from './app.ts'

const TABS = [text.tabHome, text.operationsTitle, text.reportsTitle, text.tabMore]

for (const tab of TABS) {
  test(`tab «${tab}» opens without horizontal scroll`, async ({ app, screen, browser }) => {
    await openApp(app, screen)
    await screen.getByRole('navigation', text.tabBarLabel).getByRole('button', tab).tap()
    await expect(screen.getByRole('navigation', text.tabBarLabel).getByRole('button', tab)).toHaveAttribute('aria-current', 'page')
    await app.screenshot(tab)
    expect(await horizontalOverflow(browser)).toBe(0)
  })
}
