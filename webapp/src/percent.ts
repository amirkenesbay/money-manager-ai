export const FULL_PERCENT = 100

export const percentOf = (value: number, total: number): number => (total > 0 ? (value / total) * FULL_PERCENT : 0)
