export type ApiErrorCode =
  | 'BAD_REQUEST'
  | 'UNAUTHORIZED'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'PREMIUM_REQUIRED'
  | 'INTERNAL_ERROR'

export interface ApiErrorBody {
  code: ApiErrorCode
  message: string
  details: Record<string, unknown>
}

export type SubscriptionTier = 'FREE' | 'PAID'

export interface Limits {
  aiRequestsPerDay: number
  categoriesPerType: number
  ownedSharedGroups: number | null
  activeNotifications: number | null
  historyDaysBack: number | null
}

export interface Subscription {
  tier: SubscriptionTier
  expiresAt: string | null
  limits: Limits
}

export interface Me {
  userId: number
  firstName: string | null
  username: string | null
  language: string
  timezone: string | null
  activeGroupId: string | null
  subscription: Subscription
}

export type Currency = 'KZT' | 'USD' | 'RUB' | 'EUR' | 'KGS'

export type CategoryType = 'EXPENSE' | 'INCOME'

export type GroupType = 'PERSONAL' | 'SHARED'

export interface Group {
  id: string
  name: string
  type: GroupType
  currency: Currency
  isOwner: boolean
  isActive: boolean
  memberCount: number
  initialBalance: string
}

export interface Balance {
  currency: Currency
  initial: string
  income: string
  expense: string
  total: string
}

export interface Operation {
  id: string
  type: CategoryType
  amount: string
  categoryId: string
  categoryName: string
  categoryIcon: string
  operationDate: string
  description: string | null
  creatorId: number
  creatorName: string
}

export interface OperationsPage {
  items: Operation[]
  page: number
  size: number
  totalItems: number
}

export interface CategoryAmount {
  icon: string
  name: string
  total: string
}

export interface MonthSummary {
  month: string
  currency: Currency
  income: string
  expense: string
  net: string
  operationCount: number
  expenseByCategory: CategoryAmount[]
}
