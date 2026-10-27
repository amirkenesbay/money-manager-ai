import type { Balance, Category, Group, Limits, Me, MonthSummary, Operation, OperationsPage, Reminder } from '../api/types'

export const PERSONAL_GROUP_ID = '6650a1b2c3d4e5f6a7b8c9d0'
export const FAMILY_GROUP_ID = '6650a1b2c3d4e5f6a7b8c9d1'
export const SUBSCRIBE_URL = 'https://t.me/tribute/app?startapp=test'

const FREE_LIMITS: Limits = { aiRequestsPerDay: 10, categoriesPerType: 10, ownedSharedGroups: 3, activeNotifications: 3, historyDaysBack: 30 }
const PAID_LIMITS: Limits = { aiRequestsPerDay: 100, categoriesPerType: 1000, ownedSharedGroups: null, activeNotifications: null, historyDaysBack: null }

export const me = (overrides: Partial<Me> = {}): Me => ({
  userId: 42,
  firstName: 'Амир',
  username: 'amir',
  language: 'ru',
  timezone: null,
  activeGroupId: PERSONAL_GROUP_ID,
  subscription: {
    tier: 'FREE',
    expiresAt: null,
    limits: FREE_LIMITS,
    freeLimits: FREE_LIMITS,
    paidLimits: PAID_LIMITS,
    price: { amount: '990', currency: 'KZT' },
    subscribeUrl: SUBSCRIBE_URL,
  },
  ...overrides,
})

export const group = (overrides: Partial<Group> = {}): Group => ({
  id: PERSONAL_GROUP_ID,
  name: 'Личное',
  type: 'PERSONAL',
  currency: 'KZT',
  isOwner: true,
  isActive: true,
  memberCount: 1,
  initialBalance: '0',
  ...overrides,
})

export const balance = (overrides: Partial<Balance> = {}): Balance => ({
  currency: 'KZT',
  initial: '50000',
  income: '300000',
  expense: '180000.5',
  total: '169999.5',
  ...overrides,
})

export const summary = (overrides: Partial<MonthSummary> = {}): MonthSummary => ({
  month: '2026-10',
  currency: 'KZT',
  income: '300000',
  expense: '15000',
  net: '285000',
  operationCount: 3,
  expenseByCategory: [
    { icon: '🛒', name: 'Продукты', total: '12000' },
    { icon: '🚕', name: 'Такси', total: '2000' },
    { icon: '🎬', name: 'Кино', total: '700' },
    { icon: '☕', name: 'Кофе', total: '300' },
  ],
  ...overrides,
})

export const operation = (overrides: Partial<Operation> = {}): Operation => ({
  id: 'op-1',
  type: 'EXPENSE',
  amount: '3500',
  categoryId: 'cat-1',
  categoryName: 'Продукты',
  categoryIcon: '🛒',
  operationDate: '2026-10-12',
  description: 'Магнум',
  creatorId: 42,
  creatorName: 'Амир',
  ...overrides,
})

export const operationsPage = (items: Operation[], totalItems: number = items.length, page = 0): OperationsPage =>
  ({ items, page, size: 20, totalItems })

export const category = (overrides: Partial<Category> = {}): Category => ({
  id: 'cat-1',
  name: 'Продукты',
  icon: '🛒',
  type: 'EXPENSE',
  ...overrides,
})

export const reminder = (overrides: Partial<Reminder> = {}): Reminder => ({
  id: 'rem-1',
  name: 'Записать траты',
  icon: '💸',
  isActive: true,
  frequencyType: 'DAILY',
  customInterval: null,
  hour: 21,
  minute: 0,
  dayOfWeek: null,
  dayOfMonth: null,
  monthOfYear: null,
  nextFireTime: '2026-10-28T16:00:00Z',
  ...overrides,
})
