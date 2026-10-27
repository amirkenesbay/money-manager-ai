export type Theme = 'light' | 'dark'

const THEME_ATTRIBUTE = 'data-theme'
const DARK_SCHEME_QUERY = '(prefers-color-scheme: dark)'
const BACKGROUND_VAR = '--bg'

export function applyTheme(theme: Theme): `#${string}` {
  const root = document.documentElement
  root.setAttribute(THEME_ATTRIBUTE, theme)
  return getComputedStyle(root).getPropertyValue(BACKGROUND_VAR).trim() as `#${string}`
}

export function systemTheme(): Theme {
  return window.matchMedia?.(DARK_SCHEME_QUERY).matches ? 'dark' : 'light'
}
