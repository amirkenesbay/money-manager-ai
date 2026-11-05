const SCREEN_PARAMETER = 'screen'
const MONTH_PARAMETER = 'month'
const ISO_MONTH = /^\d{4}-(0[1-9]|1[0-2])$/
const LAUNCH_SCREENS = ['add', 'categories', 'groups', 'reports', 'operations', 'ask'] as const

export type LaunchScreen = (typeof LAUNCH_SCREENS)[number]

export function launchScreen(search: string = window.location.search): LaunchScreen | null {
  const requested = new URLSearchParams(search).get(SCREEN_PARAMETER)
  return LAUNCH_SCREENS.find((screen) => screen === requested) ?? null
}

export function launchMonth(search: string = window.location.search): string | null {
  const requested = new URLSearchParams(search).get(MONTH_PARAMETER)
  return requested && ISO_MONTH.test(requested) ? requested : null
}
