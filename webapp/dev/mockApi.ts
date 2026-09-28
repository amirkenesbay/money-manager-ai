import type { IncomingMessage, ServerResponse } from 'node:http'
import type { Plugin } from 'vite'
import { AUTH_SCHEME } from '../src/api/auth.ts'
import type { ApiErrorBody, Me, SubscriptionTier } from '../src/api/types.ts'

const ME_PATH = '/api/v1/me'
const SCENARIO_PARAM = 'mock'
const DEFAULT_SCENARIO = 'ok'
const RESPONSE_DELAY_MS = 600
const SLOW_RESPONSE_DELAY_MS = 4000
const FLAKY_FAILURES_BEFORE_SUCCESS = 2
const MOCK_GROUP_ID = '6650a1b2c3d4e5f6a7b8c9d0'
const PREMIUM_EXPIRES_AT = '2026-12-31T00:00:00Z'

type MockResponse = { status: number; body: Me | ApiErrorBody; delayMs?: number }

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

let flakyCalls = 0

const scenarios: Record<string, () => MockResponse> = {
  ok: () => ({ status: 200, body: me(MOCK_GROUP_ID, 'FREE') }),
  premium: () => ({ status: 200, body: me(MOCK_GROUP_ID, 'PAID') }),
  nogroup: () => ({ status: 200, body: me(null, 'FREE') }),
  error: () => serverError,
  slow: () => ({ status: 200, body: me(MOCK_GROUP_ID, 'FREE'), delayMs: SLOW_RESPONSE_DELAY_MS }),
  flaky: () => (flakyCalls++ % (FLAKY_FAILURES_BEFORE_SUCCESS + 1) < FLAKY_FAILURES_BEFORE_SUCCESS
    ? serverError
    : { status: 200, body: me(MOCK_GROUP_ID, 'FREE') }),
}

function scenarioOf(request: IncomingMessage): string {
  const referer = request.headers.referer
  const name = referer ? new URL(referer).searchParams.get(SCENARIO_PARAM) : null
  return name && name in scenarios ? name : DEFAULT_SCENARIO
}

function respond(response: ServerResponse, { status, body, delayMs = RESPONSE_DELAY_MS }: MockResponse) {
  setTimeout(() => {
    response.statusCode = status
    response.setHeader('Content-Type', 'application/json')
    response.end(JSON.stringify(body))
  }, delayMs)
}

export function mockApi(): Plugin {
  return {
    name: 'money-manager-mock-api',
    configureServer(server) {
      server.middlewares.use(ME_PATH, (request, response) => {
        const authorized = request.headers.authorization?.startsWith(`${AUTH_SCHEME} `) === true
        respond(response, authorized ? scenarios[scenarioOf(request)]() : unauthorized)
      })
    },
  }
}
