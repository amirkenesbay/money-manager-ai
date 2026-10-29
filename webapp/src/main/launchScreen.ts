const SCREEN_PARAMETER = 'screen'
const LAUNCH_SCREENS = ['add', 'categories'] as const

export type LaunchScreen = (typeof LAUNCH_SCREENS)[number]

export function launchScreen(search: string = window.location.search): LaunchScreen | null {
  const requested = new URLSearchParams(search).get(SCREEN_PARAMETER)
  return LAUNCH_SCREENS.find((screen) => screen === requested) ?? null
}
