import type { IncomingMessage, ServerResponse } from 'node:http'
import type { Plugin } from 'vite'
import { AUTH_SCHEME } from '../src/api/auth.ts'
import type { ApiErrorBody, Balance, Category, Group, Me, MonthSummary, Operation, OperationsPage, SubscriptionTier } from '../src/api/types.ts'

const API_PREFIX = '/api/v1'
const SCENARIO_PARAM = 'mock'
const DEFAULT_SCENARIO = 'ok'
const EMPTY_SCENARIO = 'empty'
const RESPONSE_DELAY_MS = 600
const SLOW_RESPONSE_DELAY_MS = 4000
const FLAKY_FAILURES_BEFORE_SUCCESS = 2
const PERSONAL_GROUP_ID = '6650a1b2c3d4e5f6a7b8c9d0'
const FAMILY_GROUP_ID = '6650a1b2c3d4e5f6a7b8c9d1'
const PREMIUM_EXPIRES_AT = '2026-12-31T00:00:00Z'
const NO_CONTENT_STATUS = 204
const GROUP_ROUTE = /^\/groups\/([0-9a-f]{24})\/(balance|summary|operations|activate|categories)/
const SUBSCRIBE_URL = 'https://t.me/tribute/app?startapp=s11rb'
const FREE_HISTORY_DAYS = 30
const DEFAULT_PAGE_SIZE = 20
const FILLER_OPERATIONS = 26

type Body = Me | Group[] | Balance | MonthSummary | OperationsPage | Category[] | ApiErrorBody | null
type MockResponse = { status: number; body: Body; delayMs?: number }

const ok = (body: Body, delayMs?: number): MockResponse => ({ status: 200, body, delayMs })

const me = (activeGroupId: string | null, tier: SubscriptionTier): Me => ({
  userId: 1,
  firstName: 'Dev',
  username: 'dev',
  language: 'ru',
  timezone: 'Asia/Almaty',
  activeGroupId,
  subscription: {
    tier,
    expiresAt: tier === 'PAID' ? PREMIUM_EXPIRES_AT : null,
    limits: {
      aiRequestsPerDay: tier === 'PAID' ? 100 : 10,
      categoriesPerType: tier === 'PAID' ? 1000 : 10,
      ownedSharedGroups: tier === 'PAID' ? null : 3,
      activeNotifications: tier === 'PAID' ? null : 3,
      historyDaysBack: tier === 'PAID' ? null : FREE_HISTORY_DAYS,
    },
    subscribeUrl: SUBSCRIBE_URL,
  },
})

const serverError: MockResponse = {
  status: 500,
  body: { code: 'INTERNAL_ERROR', message: 'Что-то пошло не так. Попробуйте ещё раз чуть позже.', details: {} },
}

const unauthorized: MockResponse = {
  status: 401,
  body: { code: 'UNAUTHORIZED', message: 'Не удалось подтвердить вход через Telegram. Откройте приложение заново из бота.', details: {} },
}

const groups: Group[] = [
  { id: PERSONAL_GROUP_ID, name: 'Личное', type: 'PERSONAL', currency: 'KZT', isOwner: true, isActive: true, memberCount: 1, initialBalance: '50000' },
  { id: FAMILY_GROUP_ID, name: 'Семья', type: 'SHARED', currency: 'USD', isOwner: false, isActive: false, memberCount: 3, initialBalance: '0' },
]

const balances: Record<string, Balance> = {
  [PERSONAL_GROUP_ID]: { currency: 'KZT', initial: '50000', income: '450000', expense: '187450.5', total: '312549.5' },
  [FAMILY_GROUP_ID]: { currency: 'USD', initial: '0', income: '3200', expense: '4100', total: '-900' },
}

const currentMonth = (): string => new Date().toISOString().slice(0, 7)

const daysAgo = (days: number): string => new Date(Date.now() - days * 86_400_000).toISOString().slice(0, 10)

const operation = (id: string, type: Operation['type'], amount: string, icon: string, name: string, days: number, description: string | null): Operation =>
  ({ id, type, amount, categoryId: name, categoryName: name, categoryIcon: icon, operationDate: daysAgo(days), description, creatorId: 1, creatorName: 'Dev' })

const operations: Record<string, Operation[]> = {
  [PERSONAL_GROUP_ID]: [
    operation('p1', 'EXPENSE', '3500', '🛒', 'Продукты', 0, 'Магнум'),
    operation('p2', 'EXPENSE', '1200', '🚕', 'Такси', 1, null),
    operation('p3', 'INCOME', '450000', '💰', 'Зарплата', 3, null),
    operation('p4', 'EXPENSE', '18750.5', '🎬', 'Развлечения', 4, 'Концерт с очень длинным описанием, которое не должно ломать вёрстку'),
    operation('p5', 'EXPENSE', '8000', '🛒', 'Продукты', 6, null),
    ...Array.from({ length: FILLER_OPERATIONS }, (_, index) =>
      operation(`p-old-${index}`, 'EXPENSE', String(500 + index * 100), '☕', 'Кофе', 7 + index, null)),
  ],
  [FAMILY_GROUP_ID]: [
    operation('f1', 'EXPENSE', '2500', '🏠', 'Аренда', 2, null),
    operation('f2', 'INCOME', '3200', '💰', 'Зарплата', 5, null),
  ],
}

const summaryOf = (groupId: string): MonthSummary => {
  const groupOperations = operations[groupId]
  const sum = (type: Operation['type']) => groupOperations.filter((op) => op.type === type).reduce((total, op) => total + Number(op.amount), 0)
  const totals = new Map<string, { icon: string; total: number }>()
  groupOperations.filter((op) => op.type === 'EXPENSE').forEach((op) => {
    const current = totals.get(op.categoryName) ?? { icon: op.categoryIcon, total: 0 }
    totals.set(op.categoryName, { icon: current.icon, total: current.total + Number(op.amount) })
  })
  return {
    month: currentMonth(),
    currency: balances[groupId].currency,
    income: String(sum('INCOME')),
    expense: String(sum('EXPENSE')),
    net: String(sum('INCOME') - sum('EXPENSE')),
    operationCount: groupOperations.length,
    expenseByCategory: [...totals.entries()]
      .sort(([, a], [, b]) => b.total - a.total)
      .map(([name, { icon, total }]) => ({ icon, name, total: String(total) })),
  }
}

const emptySummary = (groupId: string): MonthSummary =>
  ({ ...summaryOf(groupId), income: '0', expense: '0', net: '0', operationCount: 0, expenseByCategory: [] })

let activeGroupId = PERSONAL_GROUP_ID
let flakyCalls = 0

const meScenarios: Record<string, () => MockResponse> = {
  ok: () => ok(me(activeGroupId, 'FREE')),
  [EMPTY_SCENARIO]: () => ok(me(activeGroupId, 'FREE')),
  premium: () => ok(me(activeGroupId, 'PAID')),
  nogroup: () => ok(me(null, 'FREE')),
  error: () => serverError,
  slow: () => ok(me(activeGroupId, 'FREE'), SLOW_RESPONSE_DELAY_MS),
  flaky: () => (flakyCalls++ % (FLAKY_FAILURES_BEFORE_SUCCESS + 1) < FLAKY_FAILURES_BEFORE_SUCCESS
    ? serverError
    : ok(me(activeGroupId, 'FREE'))),
}

function categoriesOf(groupId: string): Category[] {
  const seen = new Map<string, Category>()
  operations[groupId].forEach((op) => seen.set(op.categoryName, { id: op.categoryId, name: op.categoryName, icon: op.categoryIcon, type: op.type }))
  return [...seen.values()]
}

function operationsResponse(groupId: string, query: URLSearchParams, scenario: string): MockResponse {
  const from = query.get('from')
  const to = query.get('to')
  if (scenario !== 'premium' && from !== null && from < daysAgo(FREE_HISTORY_DAYS)) {
    return { status: 422, body: { code: 'PREMIUM_REQUIRED', message: `На Free доступна история только за последние ${FREE_HISTORY_DAYS} дн.`, details: { historyDaysBack: FREE_HISTORY_DAYS } } }
  }
  const page = Number(query.get('page') ?? 0)
  const size = Number(query.get('size') ?? DEFAULT_PAGE_SIZE)
  const matching = operations[groupId].filter((op) =>
    (from === null || op.operationDate >= from) &&
    (to === null || op.operationDate <= to) &&
    (query.get('type') === null || op.type === query.get('type')) &&
    (query.get('categoryId') === null || op.categoryId === query.get('categoryId')))
  return ok({ items: matching.slice(page * size, (page + 1) * size), page, size, totalItems: matching.length })
}

function groupRoute(path: string, query: URLSearchParams, scenario: string): MockResponse | null {
  const match = GROUP_ROUTE.exec(path)
  if (!match || !(match[1] in balances)) return null
  const [, groupId, resource] = match
  const empty = scenario === EMPTY_SCENARIO
  switch (resource) {
    case 'activate':
      activeGroupId = groupId
      return { status: NO_CONTENT_STATUS, body: null }
    case 'balance':
      return ok(empty ? { ...balances[groupId], income: '0', expense: '0', total: balances[groupId].initial } : balances[groupId])
    case 'summary':
      return ok(empty ? emptySummary(groupId) : summaryOf(groupId))
    case 'categories':
      return ok(categoriesOf(groupId))
    default:
      return empty ? ok({ items: [], page: 0, size: DEFAULT_PAGE_SIZE, totalItems: 0 }) : operationsResponse(groupId, query, scenario)
  }
}

function route(path: string, query: URLSearchParams, scenario: string): MockResponse | null {
  if (path === '/me') return meScenarios[scenario]()
  if (path === '/groups') return ok(groups.map((group) => ({ ...group, isActive: group.id === activeGroupId })))
  return groupRoute(path, query, scenario)
}

function scenarioOf(request: IncomingMessage): string {
  const referer = request.headers.referer
  const name = referer ? new URL(referer).searchParams.get(SCENARIO_PARAM) : null
  return name && name in meScenarios ? name : DEFAULT_SCENARIO
}

function respond(response: ServerResponse, { status, body, delayMs = RESPONSE_DELAY_MS }: MockResponse) {
  setTimeout(() => {
    response.statusCode = status
    if (body === null) {
      response.end()
      return
    }
    response.setHeader('Content-Type', 'application/json')
    response.end(JSON.stringify(body))
  }, delayMs)
}

export function mockApi(): Plugin {
  return {
    name: 'money-manager-mock-api',
    configureServer(server) {
      server.middlewares.use(API_PREFIX, (request, response, next) => {
        if (request.headers.authorization?.startsWith(`${AUTH_SCHEME} `) !== true) {
          respond(response, unauthorized)
          return
        }
        const [path, search] = (request.url ?? '').split('?')
        const mock = route(path, new URLSearchParams(search), scenarioOf(request))
        if (mock === null) {
          next()
          return
        }
        respond(response, mock)
      })
    },
  }
}
