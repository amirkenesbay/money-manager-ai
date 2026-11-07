import { execFileSync } from 'node:child_process'
import { createHmac } from 'node:crypto'
import { readFileSync } from 'node:fs'
import type { Me } from '../../src/api/types.ts'
import type { Api } from './session.ts'

const WEBHOOK_PATH = '/api/v1/webhooks/tribute'
const SIGNATURE_HEADER = 'trbt-signature'
const MONGO_CONTAINER = 'mongodb-money-manager-ai'
const MONGO_INIT = '../../docker-entrypoint-initdb.d/mongo-init.js'
const DATABASE = 'moneyManagerBot'
const USERS = 'money_manager_user'
const DAY_MS = 86_400_000

export type TributeEventName = 'new_subscription' | 'renewed_subscription' | 'cancelled_subscription' | 'new_donation'
export type TributePeriod = 'monthly' | 'yearly'
export type Access = 'REGULAR' | 'WHITELISTED' | 'BLOCKED'

const PRICES: Record<TributePeriod, number> = { monthly: 441, yearly: 4969 }

function tributeKey(): string {
  const key = process.env.TRIBUTE_API_KEY
  if (!key) throw new Error('TRIBUTE_API_KEY is not set: run through npm run test:backend')
  return key
}

export const daysFromNow = (days: number): string => new Date(Date.now() + days * DAY_MS).toISOString()

export function tributeEvent(name: TributeEventName, telegramUserId: number, period: TributePeriod, expiresAt: string): string {
  return JSON.stringify({
    name,
    created_at: new Date().toISOString(),
    sent_at: new Date().toISOString(),
    payload: {
      subscription_name: 'Money Manager Premium', subscription_id: 1, period_id: 2, period, type: 'regular',
      price: PRICES[period], amount: PRICES[period], currency: 'usd', trb_user_id: `T-${telegramUserId}`,
      telegram_user_id: telegramUserId, channel_id: 3, channel_name: 'mm', expires_at: expiresAt,
    },
  })
}

export function sendTribute(baseUrl: string, body: string, signature = createHmac('sha256', tributeKey()).update(body).digest('hex')): Promise<Response> {
  return fetch(`${baseUrl}${WEBHOOK_PATH}`, { method: 'POST', headers: { 'Content-Type': 'application/json', [SIGNATURE_HEADER]: signature }, body })
}

function mongoUri(): string {
  const init = readFileSync(MONGO_INIT, 'utf8')
  const value = (field: string) => new RegExp(`${field}: *'([^']*)'`).exec(init)?.[1] ?? ''
  return `mongodb://${value('user')}:${value('pwd')}@localhost:27017/${DATABASE}`
}

export function setAccess(telegramUserId: number, access: Access): void {
  const script = `db.${USERS}.updateOne({ telegramUserId: ${telegramUserId} }, { $set: { access: '${access}' } })`
  execFileSync('docker', ['exec', MONGO_CONTAINER, 'mongosh', '--quiet', mongoUri(), '--eval', script])
}

export const subscriptionOf = async (api: Api) => (await api.json<Me>('/me')).subscription
