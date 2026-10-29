import { Color } from 'three'
import { THEME_ATTRIBUTE } from '../theme/theme'

const CHART_VARIABLES = ['--chart-1', '--chart-2', '--chart-3', '--chart-4', '--chart-5']
const DARK_THEME = 'dark'

export interface Palette {
  dark: boolean
  accent: Color
  soft: Color
  surface: Color
  shadow: Color
  line: Color
  glow: Color
  muted: Color
  charts: Color[]
  surfaceCss: string
  monoFont: string
}

export function readPalette(): Palette {
  const root = document.documentElement
  const style = getComputedStyle(root)
  const css = (name: string) => style.getPropertyValue(name).trim()
  const color = (name: string) => new Color(css(name))
  return {
    dark: root.getAttribute(THEME_ATTRIBUTE) === DARK_THEME,
    accent: color('--accent'),
    soft: color('--accent-soft'),
    surface: color('--bg'),
    shadow: color('--shadow-dark'),
    line: color('--line'),
    glow: color('--chart-4'),
    muted: color('--muted'),
    charts: CHART_VARIABLES.map(color),
    surfaceCss: css('--bg'),
    monoFont: css('--font-mono'),
  }
}
