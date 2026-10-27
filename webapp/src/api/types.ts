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
  subscribeUrl: string
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

export interface Category {
  id: string
  name: string
  icon: string
  type: CategoryType
}

export interface MaxExpense {
  amount: string
  icon: string
  categoryName: string
  date: string
}

export interface ExpensiveDay {
  date: string
  total: string
}

export interface AnalyticsReport {
  month: string
  currency: Currency
  income: string
  expense: string
  operationCount: number
  daysInMonth: number
  topExpenses: CategoryAmount[]
  maxExpense: MaxExpense | null
  mostExpensiveDay: ExpensiveDay | null
}

export interface MonthPair {
  previous: string
  current: string
}

export interface CategoryComparison {
  icon: string
  name: string
  previous: string
  current: string
}

export interface ComparisonReport {
  month: string
  previousMonth: string
  currency: Currency
  income: MonthPair
  expense: MonthPair
  categories: CategoryComparison[]
}

export interface MemberAmount {
  name: string
  total: string
}

export interface MembersReport {
  month: string
  currency: Currency
  income: string
  expense: string
  incomeByMember: MemberAmount[]
  expenseByMember: MemberAmount[]
}

export interface CategoryMonth {
  month: string
  total: string
  count: number
}

export interface CategoryReport {
  categoryId: string
  icon: string
  name: string
  currency: Currency
  months: CategoryMonth[]
}
