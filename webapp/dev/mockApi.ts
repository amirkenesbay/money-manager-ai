import type { IncomingMessage, ServerResponse } from 'node:http'
import type { Plugin } from 'vite'
import { AUTH_SCHEME } from '../src/api/auth.ts'
import type { AppHint, VoiceResult, AnalyticsReport, ApiErrorBody, ApiErrorCode, Balance, Category, CategoryReport, ComparisonReport, Currency, Group, Invite, Limits, Me, Member, MembersReport, MonthSummary, Operation, OperationsPage, Reminder, ReminderInput, SubscriptionTier } from '../src/api/types.ts'

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
const GROUP_ROUTE = /^\/groups\/([0-9a-f]{24})(?:\/(balance|summary|operations|activate|categories|members|invite|initial-balance|voice|reports\/[a-z]+))?(?:\/([^/]+))?$/
const REMINDER_ROUTE = /^\/notifications(?:\/([^/]+))?$/
const GROUP_ID_PREFIX = '6650a1b2c3d4e5f6a7b8'
const GROUP_ID_DIGITS = 4
const BOT_LINK = 'https://t.me/moneyManagerAIbot?start=join_'
const CONFLICT_STATUS = 409
const LIMIT_STATUS = 422
const CREATED_STATUS = 201
const DEFAULT_REMINDER_ICON = '🔔'
const VOICE_DELAY_MS = 1500
const SUBSCRIBE_URL = 'https://t.me/tribute/app?startapp=s11rb'
const FREE_HISTORY_DAYS = 30
const DEFAULT_PAGE_SIZE = 20
const FILLER_OPERATIONS = 26

type Body = Me | Group | Group[] | Balance | MonthSummary | Operation | OperationsPage | Category | Category[] | AnalyticsReport | ComparisonReport | MembersReport | CategoryReport | Member[] | Invite | Reminder | Reminder[] | VoiceResult | ApiErrorBody | null
type MockResponse = { status: number; body: Body; delayMs?: number }

const ok = (body: Body, delayMs?: number): MockResponse => ({ status: 200, body, delayMs })

const FREE_LIMITS: Limits = { aiRequestsPerDay: 10, categoriesPerType: 10, ownedSharedGroups: 3, activeNotifications: 3, historyDaysBack: FREE_HISTORY_DAYS }
const PAID_LIMITS: Limits = { aiRequestsPerDay: 100, categoriesPerType: 1000, ownedSharedGroups: null, activeNotifications: null, historyDaysBack: null }

const profile = { language: 'ru', timezone: null as string | null, seenHints: [] as AppHint[] }

const me = (activeGroupId: string | null, tier: SubscriptionTier): Me => ({
  userId: 1,
  firstName: 'Dev',
  username: 'dev',
  language: profile.language,
  timezone: profile.timezone,
  activeGroupId,
  seenHints: profile.seenHints,
  subscription: {
    tier,
    expiresAt: tier === 'PAID' ? PREMIUM_EXPIRES_AT : null,
    limits: tier === 'PAID' ? PAID_LIMITS : FREE_LIMITS,
    freeLimits: FREE_LIMITS,
    paidLimits: PAID_LIMITS,
    price: { amount: '990', currency: 'KZT' },
    subscribeUrl: SUBSCRIBE_URL,
  },
})

const failure = (status: number, code: ApiErrorCode, message: string, details: Record<string, unknown> = {}): MockResponse =>
  ({ status, body: { code, message, details } })

const serverError: MockResponse = {
  status: 500,
  body: { code: 'INTERNAL_ERROR', message: 'Что-то пошло не так. Попробуйте ещё раз чуть позже.', details: {} },
}

const unauthorized: MockResponse = {
  status: 401,
  body: { code: 'UNAUTHORIZED', message: 'Не удалось подтвердить вход через Telegram. Откройте приложение заново из бота.', details: {} },
}

const premiumRequired: MockResponse = {
  status: 422,
  body: { code: 'PREMIUM_REQUIRED', message: `На Free доступна история только за последние ${FREE_HISTORY_DAYS} дн.`, details: { historyDaysBack: FREE_HISTORY_DAYS } },
}

let groups: Group[] = [
  { id: PERSONAL_GROUP_ID, name: 'Личное', type: 'PERSONAL', currency: 'KZT', isOwner: true, isActive: true, memberCount: 1, initialBalance: '50000', monthStartDay: 1 },
  { id: FAMILY_GROUP_ID, name: 'Семья', type: 'SHARED', currency: 'USD', isOwner: false, isActive: false, memberCount: 3, initialBalance: '0', monthStartDay: 1 },
]

const groupOf = (groupId: string): Group => groups.find((group) => group.id === groupId) ?? groups[0]

const currencyOf = (groupId: string): Currency => groupOf(groupId).currency

const currentMonth = (): string => new Date().toISOString().slice(0, 7)

const isoDay = (date: Date): string => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`

const periodOf = (groupId: string, month: string): { from: string; to: string } => {
  const [year, monthNumber] = month.split('-').map(Number)
  const startDay = groupOf(groupId).monthStartDay
  return { from: isoDay(new Date(year, monthNumber - 1, startDay)), to: isoDay(new Date(year, monthNumber, startDay - 1)) }
}

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

const balanceOf = (groupId: string): Balance => {
  const sum = (type: Operation['type']) => operations[groupId].filter((op) => op.type === type).reduce((total, op) => total + Number(op.amount), 0)
  const initial = Number(groupOf(groupId).initialBalance)
  return { currency: currencyOf(groupId), initial: String(initial), income: String(sum('INCOME')), expense: String(sum('EXPENSE')), total: String(initial + sum('INCOME') - sum('EXPENSE')) }
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
    ...periodOf(groupId, currentMonth()),
    currency: currencyOf(groupId),
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

const categories: Record<string, Category[]> = {}
let createdCategories = 0

function categoriesOf(groupId: string): Category[] {
  if (!categories[groupId]) {
    const seen = new Map<string, Category>()
    operations[groupId].forEach((op) => seen.set(op.categoryName, { id: op.categoryId, name: op.categoryName, icon: op.categoryIcon, type: op.type }))
    categories[groupId] = [...seen.values()]
  }
  return categories[groupId]
}

const freeTier = (scenario: string) => scenario !== 'premium'

function mutateCategory(groupId: string, method: string, categoryId: string | undefined, body: string, scenario: string): MockResponse {
  const list = categoriesOf(groupId)
  if (method === 'DELETE') {
    categories[groupId] = list.filter((category) => category.id !== categoryId)
    return { status: NO_CONTENT_STATUS, body: null }
  }
  const input = JSON.parse(body) as Partial<Category>
  const current = list.find((category) => category.id === categoryId)
  const type = current?.type ?? input.type ?? 'EXPENSE'
  const name = input.name?.trim() ?? current?.name ?? ''
  if (list.some((category) => category.name === name && category.type === type && category.id !== categoryId)) {
    return failure(CONFLICT_STATUS, 'CATEGORY_DUPLICATE', `Категория «${name}» уже есть.`)
  }
  if (!current) {
    const limit = FREE_LIMITS.categoriesPerType
    if (freeTier(scenario) && list.filter((category) => category.type === type).length >= limit) {
      return failure(LIMIT_STATUS, 'CATEGORY_LIMIT_REACHED', `На Free — до ${limit} категорий каждого типа.`, { limit })
    }
    createdCategories += 1
    const created: Category = { id: `cat-new-${createdCategories}`, name, icon: input.icon ?? '📌', type }
    categories[groupId] = [...list, created]
    return { status: CREATED_STATUS, body: created }
  }
  const updated: Category = { ...current, name, icon: input.icon ?? current.icon }
  categories[groupId] = list.map((category) => (category.id === categoryId ? updated : category))
  return ok(updated)
}

function operationsResponse(groupId: string, query: URLSearchParams, scenario: string): MockResponse {
  const from = query.get('from')
  const to = query.get('to')
  if (scenario !== 'premium' && from !== null && from < daysAgo(FREE_HISTORY_DAYS)) {
    return premiumRequired
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

const monthOperations = (groupId: string, month: string) => operations[groupId].filter((op) => op.operationDate.startsWith(month))

const sumOf = (ops: Operation[], type: Operation['type']) => ops.filter((op) => op.type === type).reduce((total, op) => total + Number(op.amount), 0)

const shiftMonth = (month: string, delta: number): string => {
  const [year, monthNumber] = month.split('-').map(Number)
  const date = new Date(Date.UTC(year, monthNumber - 1 + delta, 1))
  return date.toISOString().slice(0, 7)
}

function analyticsReport(groupId: string, month: string): AnalyticsReport {
  const ops = monthOperations(groupId, month)
  const expenses = ops.filter((op) => op.type === 'EXPENSE')
  const totals = new Map<string, { icon: string; total: number }>()
  expenses.forEach((op) => totals.set(op.categoryName, { icon: op.categoryIcon, total: (totals.get(op.categoryName)?.total ?? 0) + Number(op.amount) }))
  const max = [...expenses].sort((a, b) => Number(b.amount) - Number(a.amount))[0]
  return {
    month,
    ...periodOf(groupId, month),
    currency: currencyOf(groupId),
    income: String(sumOf(ops, 'INCOME')),
    expense: String(sumOf(ops, 'EXPENSE')),
    operationCount: ops.length,
    daysInMonth: 31,
    topExpenses: [...totals.entries()].sort(([, a], [, b]) => b.total - a.total).slice(0, 3).map(([name, { icon, total }]) => ({ icon, name, total: String(total) })),
    maxExpense: max ? { amount: max.amount, icon: max.categoryIcon, categoryName: max.categoryName, date: max.operationDate } : null,
    mostExpensiveDay: max ? { date: max.operationDate, total: max.amount } : null,
  }
}

function comparisonReport(groupId: string, month: string): ComparisonReport {
  const previous = monthOperations(groupId, shiftMonth(month, -1))
  const current = monthOperations(groupId, month)
  return {
    month,
    ...periodOf(groupId, month),
    previousMonth: shiftMonth(month, -1),
    currency: currencyOf(groupId),
    income: { previous: String(sumOf(previous, 'INCOME')), current: String(sumOf(current, 'INCOME')) },
    expense: { previous: String(sumOf(previous, 'EXPENSE')), current: String(sumOf(current, 'EXPENSE')) },
    categories: [],
  }
}

function membersReport(groupId: string, month: string): MembersReport {
  const ops = monthOperations(groupId, month)
  const expense = sumOf(ops, 'EXPENSE')
  const shared = groups.find((group) => group.id === groupId)?.type === 'SHARED'
  const expenseByMember = expense === 0 ? [] : shared
    ? [{ name: 'Dev', total: String(Math.round(expense * 0.6)) }, { name: 'Айгерим', total: String(Math.round(expense * 0.4)) }]
    : [{ name: 'Dev', total: String(expense) }]
  return { month, ...periodOf(groupId, month), currency: currencyOf(groupId), income: String(sumOf(ops, 'INCOME')), expense: String(expense), incomeByMember: [], expenseByMember }
}

function categoryReport(groupId: string, categoryId: string): CategoryReport {
  const category = categoriesOf(groupId).find((candidate) => candidate.id === categoryId) ?? categoriesOf(groupId)[0]
  const thisMonth = currentMonth()
  return {
    categoryId: category.id,
    icon: category.icon,
    name: category.name,
    currency: currencyOf(groupId),
    months: [5, 4, 3, 2, 1, 0].map((back) => {
      const month = shiftMonth(thisMonth, -back)
      const ops = operations[groupId].filter((op) => op.categoryId === category.id && op.operationDate.startsWith(month))
      const total = back === 0 ? ops.reduce((sum, op) => sum + Number(op.amount), 0) : 15000 + back * 2500
      return { month, total: String(total), count: ops.length || back + 2 }
    }),
  }
}

function reportRoute(groupId: string, report: string, query: URLSearchParams, scenario: string): MockResponse {
  const month = query.get('month') ?? currentMonth()
  const oldest = report === 'comparison' ? shiftMonth(month, -1) : month
  if (scenario !== 'premium' && report !== 'category' && `${oldest}-01` < daysAgo(FREE_HISTORY_DAYS)) return premiumRequired
  switch (report) {
    case 'analytics':
      return ok(analyticsReport(groupId, month))
    case 'comparison':
      return ok(comparisonReport(groupId, month))
    case 'members':
      return ok(membersReport(groupId, month))
    default:
      return ok(categoryReport(groupId, query.get('categoryId') ?? ''))
  }
}

interface OperationInput {
  type: Operation['type']
  amount: string
  categoryId: string
  operationDate: string
  description: string | null
}

let createdOperations = 0

function toOperation(groupId: string, id: string, input: OperationInput): Operation {
  const category = categoriesOf(groupId).find((candidate) => candidate.id === input.categoryId)
  return { id, ...input, categoryName: category?.name ?? input.categoryId, categoryIcon: category?.icon ?? '📌', creatorId: 1, creatorName: 'Dev' }
}

function mutateOperation(groupId: string, method: string, operationId: string | undefined, body: string): MockResponse {
  const groupOperations = operations[groupId]
  if (method === 'POST') {
    createdOperations += 1
    const created = toOperation(groupId, `new-${createdOperations}`, JSON.parse(body) as OperationInput)
    operations[groupId] = [created, ...groupOperations].sort((a, b) => b.operationDate.localeCompare(a.operationDate))
    return { status: 201, body: created }
  }
  const index = groupOperations.findIndex((op) => op.id === operationId)
  if (index < 0) return { status: 404, body: { code: 'NOT_FOUND', message: 'Операция не найдена. Возможно, её уже удалили.', details: {} } }
  if (method === 'DELETE') {
    operations[groupId] = groupOperations.filter((op) => op.id !== operationId)
    return { status: NO_CONTENT_STATUS, body: null }
  }
  const replaced = toOperation(groupId, groupOperations[index].id, JSON.parse(body) as OperationInput)
  operations[groupId] = groupOperations.map((op) => (op.id === operationId ? replaced : op))
  return ok(replaced)
}

const MEMBERS: Member[] = [
  { userId: 7, name: 'Айгерим', isOwner: true },
  { userId: 1, name: 'Dev', isOwner: false },
  { userId: 8, name: 'Данияр', isOwner: false },
]

let createdGroups = 0

function voiceResult(groupId: string): VoiceResult {
  const known = categoriesOf(groupId).find((category) => category.type === 'EXPENSE')
  return {
    operations: [
      { type: 'EXPENSE', amount: '2000.0', categoryId: known?.id ?? null, categoryName: known?.name ?? null, categoryIcon: known?.icon ?? null, isNewCategory: false, operationDate: daysAgo(0), description: null },
      { type: 'EXPENSE', amount: '1500.0', categoryId: null, categoryName: 'Кафе', categoryIcon: '☕', isNewCategory: true, operationDate: daysAgo(1), description: 'капучино' },
    ],
  }
}

function groupRoute(path: string, query: URLSearchParams, scenario: string, method: string, body: string): MockResponse | null {
  const match = GROUP_ROUTE.exec(path)
  if (!match || !groups.some((group) => group.id === match[1])) return null
  const [, groupId, resource, itemId] = match
  if (resource === undefined) return mutateGroup(groupId, method, body)
  if (resource === 'operations' && method !== 'GET') return mutateOperation(groupId, method, itemId, body)
  if (resource === 'categories' && method !== 'GET') return mutateCategory(groupId, method, itemId && decodeURIComponent(itemId), body, scenario)
  const empty = scenario === EMPTY_SCENARIO
  if (resource.startsWith('reports/')) return reportRoute(groupId, resource.slice('reports/'.length), query, scenario)
  switch (resource) {
    case 'activate':
      activeGroupId = groupId
      return { status: NO_CONTENT_STATUS, body: null }
    case 'balance':
      return ok(empty ? { ...balanceOf(groupId), income: '0', expense: '0', total: groupOf(groupId).initialBalance } : balanceOf(groupId))
    case 'summary':
      return ok(empty ? emptySummary(groupId) : summaryOf(groupId))
    case 'categories':
      return ok(categoriesOf(groupId))
    case 'members':
      return ok(groupOf(groupId).isOwner ? MEMBERS.map((member) => ({ ...member, isOwner: member.userId === 1 })) : MEMBERS)
    case 'invite':
      return ok({ inviteToken: `K7Q2${groupId.slice(-5)}`, url: `${BOT_LINK}K7Q2${groupId.slice(-5)}` })
    case 'voice':
      return ok(voiceResult(groupId), VOICE_DELAY_MS)
    case 'initial-balance':
      return ok(replaceGroup(groupId, { initialBalance: (JSON.parse(body) as { amount: string }).amount }))
    default:
      return empty ? ok({ items: [], page: 0, size: DEFAULT_PAGE_SIZE, totalItems: 0 }) : operationsResponse(groupId, query, scenario)
  }
}

function replaceGroup(groupId: string, changes: Partial<Group>): Group {
  groups = groups.map((group) => (group.id === groupId ? { ...group, ...changes } : group))
  return { ...groupOf(groupId), isActive: groupId === activeGroupId }
}

function mutateGroup(groupId: string, method: string, body: string): MockResponse {
  if (method === 'DELETE') {
    groups = groups.filter((group) => group.id !== groupId)
    if (activeGroupId === groupId) activeGroupId = PERSONAL_GROUP_ID
    return { status: NO_CONTENT_STATUS, body: null }
  }
  return ok(replaceGroup(groupId, JSON.parse(body) as Partial<Group>))
}

function createGroup(body: string, scenario: string): MockResponse {
  const input = JSON.parse(body) as { name: string; currency: Currency }
  const name = input.name.trim()
  if (groups.some((group) => group.isOwner && group.name === name)) return failure(CONFLICT_STATUS, 'GROUP_DUPLICATE', `Группа «${name}» у вас уже есть.`)
  const limit = FREE_LIMITS.ownedSharedGroups ?? 0
  if (freeTier(scenario) && groups.filter((group) => group.isOwner && group.type === 'SHARED').length >= limit) {
    return failure(LIMIT_STATUS, 'GROUP_LIMIT_REACHED', `На Free можно создать до ${limit} совместных групп.`, { limit })
  }
  createdGroups += 1
  const id = `${GROUP_ID_PREFIX}${String(createdGroups).padStart(GROUP_ID_DIGITS, '0')}`
  const created: Group = { id, name, type: 'SHARED', currency: input.currency, isOwner: true, isActive: true, memberCount: 1, initialBalance: '0', monthStartDay: 1 }
  groups = [...groups, created]
  operations[id] = []
  activeGroupId = id
  return { status: CREATED_STATUS, body: created }
}

let reminders: Reminder[] = [
  { id: 'rem-1', name: 'Записать траты за день', icon: '💸', isActive: true, frequencyType: 'DAILY', customInterval: null, hour: 21, minute: 0, dayOfWeek: null, dayOfMonth: null, monthOfYear: null, nextFireTime: new Date().toISOString() },
  { id: 'rem-2', name: 'Оплатить интернет', icon: '📱', isActive: true, frequencyType: 'MONTHLY', customInterval: null, hour: 10, minute: 0, dayOfWeek: null, dayOfMonth: 5, monthOfYear: null, nextFireTime: new Date().toISOString() },
  { id: 'rem-3', name: 'Итоги недели', icon: '📊', isActive: false, frequencyType: 'WEEKLY', customInterval: null, hour: 20, minute: 0, dayOfWeek: 'SUNDAY', dayOfMonth: null, monthOfYear: null, nextFireTime: new Date().toISOString() },
]
let createdReminders = 0

const reminderLimit = (): MockResponse => {
  const limit = FREE_LIMITS.activeNotifications ?? 0
  return failure(LIMIT_STATUS, 'NOTIFICATION_LIMIT_REACHED', `На Free одновременно активны до ${limit} напоминаний.`, { limit })
}

const activeReminders = () => reminders.filter((reminder) => reminder.isActive).length

function reminderRoute(reminderId: string | undefined, method: string, body: string, scenario: string): MockResponse {
  const atLimit = freeTier(scenario) && activeReminders() >= (FREE_LIMITS.activeNotifications ?? 0)
  if (reminderId === undefined) {
    if (method === 'GET') return ok(reminders)
    if (atLimit) return reminderLimit()
    createdReminders += 1
    const input = JSON.parse(body) as ReminderInput
    const created: Reminder = { ...input, id: `rem-new-${createdReminders}`, icon: input.icon ?? DEFAULT_REMINDER_ICON, isActive: true, nextFireTime: new Date().toISOString() }
    reminders = [...reminders, created]
    return { status: CREATED_STATUS, body: created }
  }
  if (method === 'DELETE') {
    reminders = reminders.filter((reminder) => reminder.id !== reminderId)
    return { status: NO_CONTENT_STATUS, body: null }
  }
  const changes = JSON.parse(body) as Partial<Reminder>
  if (changes.isActive === true && atLimit) return reminderLimit()
  reminders = reminders.map((reminder) => (reminder.id === reminderId ? { ...reminder, ...changes, icon: changes.icon ?? reminder.icon } : reminder))
  return ok(reminders.find((reminder) => reminder.id === reminderId) ?? null)
}

function route(path: string, query: URLSearchParams, scenario: string, method: string, body: string): MockResponse | null {
  if (path === '/me') {
    if (method === 'PATCH') Object.assign(profile, JSON.parse(body))
    return meScenarios[scenario]()
  }
  if (path === '/me/hints') {
    const { hint } = JSON.parse(body) as { hint: AppHint }
    if (!profile.seenHints.includes(hint)) profile.seenHints.push(hint)
    return meScenarios[scenario]()
  }
  if (path === '/groups') {
    if (method === 'POST') return createGroup(body, scenario)
    return ok(groups.map((group) => ({ ...group, isActive: group.id === activeGroupId })))
  }
  const reminder = REMINDER_ROUTE.exec(path)
  if (reminder) return reminderRoute(reminder[1], method, body, scenario)
  return groupRoute(path, query, scenario, method, body)
}

function readBody(request: IncomingMessage): Promise<string> {
  return new Promise((resolve) => {
    let body = ''
    request.on('data', (chunk: Buffer) => { body += chunk.toString() })
    request.on('end', () => resolve(body))
  })
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
      server.middlewares.use(API_PREFIX, async (request, response, next) => {
        if (request.headers.authorization?.startsWith(`${AUTH_SCHEME} `) !== true) {
          respond(response, unauthorized)
          return
        }
        const [path, search] = (request.url ?? '').split('?')
        const method = request.method ?? 'GET'
        const body = method === 'GET' ? '' : await readBody(request)
        const mock = route(path, new URLSearchParams(search), scenarioOf(request), method, body)
        if (mock === null) {
          next()
          return
        }
        respond(response, mock)
      })
    },
  }
}
