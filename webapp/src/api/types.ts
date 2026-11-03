export type ApiErrorCode =
  | 'BAD_REQUEST'
  | 'UNAUTHORIZED'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'PREMIUM_REQUIRED'
  | 'CATEGORY_DUPLICATE'
  | 'CATEGORY_LIMIT_REACHED'
  | 'GROUP_DUPLICATE'
  | 'GROUP_LIMIT_REACHED'
  | 'NOTIFICATION_LIMIT_REACHED'
  | 'AI_LIMIT_REACHED'
  | 'AI_BUSY'
  | 'AI_UNAVAILABLE'
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

export interface Price {
  amount: string
  currency: Currency
}

export interface Subscription {
  tier: SubscriptionTier
  expiresAt: string | null
  limits: Limits
  freeLimits: Limits
  paidLimits: Limits
  price: Price
  subscribeUrl: string
}

export type AppHint = 'WELCOME' | 'OPERATIONS' | 'REPORTS' | 'MORE' | 'OPERATION_FORM' | 'GROUP' | 'MANUAL_ENTRY'

export interface Me {
  userId: number
  firstName: string | null
  username: string | null
  language: string
  timezone: string | null
  activeGroupId: string | null
  seenHints: AppHint[]
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
  monthStartDay: number
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
  from: string
  to: string
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
  from: string
  to: string
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
  from: string
  to: string
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
  from: string
  to: string
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

export interface Member {
  userId: number
  name: string
  isOwner: boolean
}

export interface Invite {
  inviteToken: string
  url: string
}

export type FrequencyType =
  | 'DAILY'
  | 'WEEKLY'
  | 'BIWEEKLY'
  | 'MONTHLY'
  | 'BIMONTHLY'
  | 'YEARLY'
  | 'CUSTOM_DAYS'
  | 'CUSTOM_WEEKS'
  | 'CUSTOM_MONTHS'
  | 'CUSTOM_YEARS'

export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'

export interface ReminderSchedule {
  frequencyType: FrequencyType
  customInterval: number | null
  hour: number
  minute: number
  dayOfWeek: DayOfWeek | null
  dayOfMonth: number | null
  monthOfYear: number | null
}

export interface ReminderInput extends ReminderSchedule {
  name: string
  icon: string | null
}

export interface Reminder extends ReminderSchedule {
  id: string
  name: string
  icon: string
  isActive: boolean
  nextFireTime: string
}

export interface VoiceOperation {
  type: CategoryType
  amount: string
  categoryId: string | null
  categoryName: string | null
  categoryIcon: string | null
  isNewCategory: boolean
  operationDate: string
  description: string | null
}

export interface VoiceResult {
  operations: VoiceOperation[]
}
