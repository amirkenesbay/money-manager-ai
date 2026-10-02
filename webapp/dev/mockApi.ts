import type { IncomingMessage, ServerResponse } from 'node:http'
import type { Plugin } from 'vite'
import { AUTH_SCHEME } from '../src/api/auth.ts'
import type { ApiErrorBody, Balance, Group, Me, MonthSummary, Operation, OperationsPage, SubscriptionTier } from '../src/api/types.ts'

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
const GROUP_ROUTE = /^\/groups\/([0-9a-f]{24})\/(balance|summary|operations|activate)/

type Body = Me | Group[] | Balance | MonthSummary | OperationsPage | ApiErrorBody | null
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
      historyDaysBack: tier === 'PAID' ? null : 30,
    },
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

function groupRoute(path: string, scenario: string): MockResponse | null {
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
    default: {
      const items = empty ? [] : operations[groupId]
      return ok({ items, page: 0, size: items.length, totalItems: items.length })
    }
  }
}

function route(path: string, scenario: string): MockResponse | null {
  if (path === '/me') return meScenarios[scenario]()
  if (path === '/groups') return ok(groups.map((group) => ({ ...group, isActive: group.id === activeGroupId })))
  return groupRoute(path, scenario)
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
        const path = (request.url ?? '').split('?')[0]
        const mock = route(path, scenarioOf(request))
        if (mock === null) {
          next()
          return
        }
        respond(response, mock)
      })
    },
  }
}
