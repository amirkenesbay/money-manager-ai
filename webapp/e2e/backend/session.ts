import type { App, Screen } from 'e2e'
import { expect } from 'e2e'
import { launchParams } from '../../dev/launchParams.ts'
import { authorizationHeader } from '../../src/api/auth.ts'
import { APP_HINTS, type Category, type CategoryType, type Currency, type Group, type Operation } from '../../src/api/types.ts'
import { ru } from '../../src/i18n/ru.ts'

export const text = ru

const API_PREFIX = '/api/v1'
const INIT_DATA_PARAM = 'tgWebAppData'
const TEST_USER_ID_BASE = 900_000_000
const TEST_USER_ID_SPAN = 90_000_000
const DEFAULT_GROUP_NAME = 'Тест'

const SEED_CATEGORIES: { name: string; icon: string; type: CategoryType }[] = [
  { name: 'Продукты', icon: '🛒', type: 'EXPENSE' },
  { name: 'Такси', icon: '🚕', type: 'EXPENSE' },
  { name: 'Зарплата', icon: '💰', type: 'INCOME' },
]

function botToken(): string {
  const token = process.env.BOT_TOKEN
  if (!token) throw new Error('BOT_TOKEN of the TEST bot is not set: run through npm run e2e:backend')
  return token
}

export const newUserId = (): number => TEST_USER_ID_BASE + Math.floor(Math.random() * TEST_USER_ID_SPAN)

export class Api {
  constructor(private readonly baseUrl: string, readonly userId: number) {}

  authorization(): string {
    return authorizationHeader(new URLSearchParams(launchParams(botToken(), this.userId)).get(INIT_DATA_PARAM) ?? '')
  }

  raw(path: string, init: RequestInit = {}, authorized = true): Promise<Response> {
    const headers = new Headers(init.headers)
    if (authorized) headers.set('Authorization', this.authorization())
    if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
    return fetch(`${this.baseUrl}${API_PREFIX}${path}`, { ...init, headers })
  }

  async json<T>(path: string, init: RequestInit = {}): Promise<T> {
    const response = await this.raw(path, init)
    if (!response.ok) throw new Error(`${init.method ?? 'GET'} ${path} → ${response.status}: ${await response.text()}`)
    return response.status === 204 ? (undefined as T) : await response.json() as T
  }

  post<T>(path: string, body: unknown): Promise<T> {
    return this.json<T>(path, { method: 'POST', body: JSON.stringify(body) })
  }

  async group(groupId: string): Promise<Group> {
    const found = (await this.json<Group[]>('/groups')).find((group) => group.id === groupId)
    if (!found) throw new Error(`group ${groupId} is not visible`)
    return found
  }

  categories(groupId: string): Promise<Category[]> {
    return this.json<Category[]>(`/groups/${groupId}/categories`)
  }

  async operations(groupId: string): Promise<Operation[]> {
    return (await this.json<{ items: Operation[] }>(`/groups/${groupId}/operations`)).items
  }
}

export async function seedGroup(api: Api, currency: Currency = 'KZT', name = DEFAULT_GROUP_NAME): Promise<{ group: Group; categories: Category[] }> {
  await api.json('/me')
  await Promise.all(APP_HINTS.map((hint) => api.post('/me/hints', { hint })))
  const group = await api.post<Group>('/groups', { name, currency })
  await api.json(`/groups/${group.id}/activate`, { method: 'POST' })
  const defaults = await api.categories(group.id)
  const missing = SEED_CATEGORIES.filter((seed) => !defaults.some((category) => category.name === seed.name && category.type === seed.type))
  for (const seed of missing) await api.post<Category>(`/groups/${group.id}/categories`, seed)
  return { group, categories: await api.categories(group.id) }
}

export const categoryNamed = (categories: Category[], name: string): Category => {
  const found = categories.find((category) => category.name === name)
  if (!found) throw new Error(`no seeded category ${name}`)
  return found
}

export async function openAs(app: App, screen: Screen, userId: number, waitForHome = true): Promise<void> {
  await app.open(`/?visit=${Date.now()}#${launchParams(botToken(), userId)}`)
  if (!waitForHome) return
  await expect(screen.getByRole('navigation', text.tabBarLabel)).toBeVisible()
  const tour = screen.getByRole('dialog', text.welcomeLabel)
  if (await tour.isVisible()) {
    await tour.getByRole('button', text.welcomeSkip).tap()
    await expect(tour).toBeHidden()
  }
}

export const tabBar = (screen: Screen) => screen.getByRole('navigation', text.tabBarLabel)
