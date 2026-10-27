const SPACES = /[\s ]/g
const DECIMAL_COMMA = ','
const AMOUNT_PATTERN = /^\d+(\.\d{1,2})?$/

export const HUGE_AMOUNT = 1_000_000_000

export function parseBalance(raw: string): string | null {
  const normalized = raw.replace(SPACES, '').replace(DECIMAL_COMMA, '.')
  return AMOUNT_PATTERN.test(normalized) ? normalized : null
}

export function parseAmount(raw: string): string | null {
  const amount = parseBalance(raw)
  return amount !== null && Number(amount) > 0 ? amount : null
}
