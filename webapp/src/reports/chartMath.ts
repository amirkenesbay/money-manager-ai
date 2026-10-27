export const FULL_PERCENT = 100

const CHART_COLORS = ['var(--chart-1)', 'var(--chart-2)', 'var(--chart-3)', 'var(--chart-4)', 'var(--chart-5)']

export const chartColor = (index: number): string => CHART_COLORS[index % CHART_COLORS.length]

export const percentOf = (value: number, total: number): number => (total > 0 ? (value / total) * FULL_PERCENT : 0)
