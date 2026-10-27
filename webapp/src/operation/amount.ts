const SPACES = /[\s ]/g
const DECIMAL_COMMA = ','
const AMOUNT_PATTERN = /^\d+(\.\d{1,2})?$/

export const HUGE_AMOUNT = 1_000_000_000

export function parseAmount(raw: string): string | null {
  const normalized = raw.replace(SPACES, '').replace(DECIMAL_COMMA, '.')
  if (!AMOUNT_PATTERN.test(normalized) || Number(normalized) <= 0) return null
  return normalized
}
