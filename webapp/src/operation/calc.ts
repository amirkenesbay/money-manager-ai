import { groupAmountInput } from './amount'

export const PLUS = '+'
export const MINUS = '−'
export const TIMES = '×'
export const DIVIDE = '÷'
export const OPERATORS = [PLUS, MINUS, TIMES, DIVIDE] as const
export type Operator = (typeof OPERATORS)[number]

const TYPED_OPERATORS: Record<string, Operator> = { '+': PLUS, '-': MINUS, '−': MINUS, '*': TIMES, '×': TIMES, x: TIMES, х: TIMES, '/': DIVIDE, '÷': DIVIDE, ':': DIVIDE }
const CENTS = 100
const SPACES = /[\s ]/g
const NUMBER_CHAR = /[\d.,]/
const OPERATOR_SPLIT = /([+−×÷])/

const isOperator = (token: string): token is Operator => (OPERATORS as readonly string[]).includes(token)

export function normalizeExpression(raw: string): string {
  let result = ''
  for (const char of raw.replace(SPACES, '')) {
    const operator = TYPED_OPERATORS[char]
    if (operator) {
      if (result === '') continue
      result = isOperator(result.at(-1) ?? '') ? result.slice(0, -1) + operator : result + operator
    } else if (NUMBER_CHAR.test(char)) {
      result += char
    }
  }
  return result
}

export const hasOperator = (expression: string): boolean => OPERATORS.some((operator) => expression.includes(operator))

export function evaluateExpression(expression: string): number | null {
  const tokens = normalizeExpression(expression).split(OPERATOR_SPLIT).filter(Boolean)
  if (tokens.length > 0 && isOperator(tokens.at(-1) ?? '')) tokens.pop()
  if (tokens.length === 0) return null
  const values: number[] = []
  const operators: Operator[] = []
  for (const token of tokens) {
    if (isOperator(token)) operators.push(token)
    else {
      const value = Number(token.replace(',', '.'))
      if (!Number.isFinite(value)) return null
      values.push(value)
    }
  }
  for (let index = 0; index < operators.length;) {
    const operator = operators[index]
    if (operator === TIMES || operator === DIVIDE) {
      const right = values[index + 1]
      if (operator === DIVIDE && right === 0) return null
      values.splice(index, 2, operator === TIMES ? values[index] * right : values[index] / right)
      operators.splice(index, 1)
    } else {
      index += 1
    }
  }
  const total = operators.reduce((sum, operator, index) => (operator === PLUS ? sum + values[index + 1] : sum - values[index + 1]), values[0])
  return Math.round(total * CENTS) / CENTS
}

export function displayExpression(expression: string): string {
  return normalizeExpression(expression)
    .split(OPERATOR_SPLIT)
    .filter(Boolean)
    .map((token) => (isOperator(token) ? ` ${token} ` : groupAmountInput(token)))
    .join('')
}

export function amountOfExpression(expression: string): string | null {
  const value = evaluateExpression(expression)
  return value !== null && value > 0 ? value.toFixed(2).replace(/\.00$/, '') : null
}
