const SPACES = /[\s ]/g
const DECIMAL_COMMA = ','
const AMOUNT_PATTERN = /^\d+(\.\d{1,2})?$/

export const HUGE_AMOUNT = 1_000_000_000

export function normalizeNumber(raw: string): string {
  return raw.replace(SPACES, '').replace(DECIMAL_COMMA, '.')
}

export function parseBalance(raw: string): string | null {
  const normalized = normalizeNumber(raw)
  return AMOUNT_PATTERN.test(normalized) ? normalized : null
}

export function parseAmount(raw: string): string | null {
  const amount = parseBalance(raw)
  return amount !== null && Number(amount) > 0 ? amount : null
}

const DIGIT_GROUP = /\B(?=(\d{3})+(?!\d))/g
const GROUP_SEPARATOR = ' '
const DECIMAL_SEPARATORS = /[.,]/

export function groupAmountInput(raw: string): string {
  const compact = raw.replace(SPACES, '')
  const separator = compact.match(DECIMAL_SEPARATORS)?.[0]
  const [whole, fraction] = separator ? [compact.slice(0, compact.indexOf(separator)), compact.slice(compact.indexOf(separator))] : [compact, '']
  if (!/^\d*$/.test(whole)) return raw
  return whole.replace(DIGIT_GROUP, GROUP_SEPARATOR) + fraction
}
