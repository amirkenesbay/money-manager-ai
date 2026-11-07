export type ApiErrorCode =
  | 'BAD_REQUEST'
  | 'UNAUTHORIZED'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'PREMIUM_REQUIRED'
  | 'CATEGORY_DUPLICATE'
  | 'CATEGORY_LIMIT_REACHED'
  | 'BUDGET_LIMIT_REACHED'
  | 'RECURRING_LIMIT_REACHED'
  | 'GROUP_DUPLICATE'
  | 'GROUP_LIMIT_REACHED'
  | 'NOTIFICATION_LIMIT_REACHED'
  | 'AI_LIMIT_REACHED'
  | 'AI_BUSY'
  | 'AI_UNAVAILABLE'
  | 'REMINDER_TOO_SOON'
  | 'IMPORT_LIMIT_REACHED'
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
  budgets: number | null
  recurringPayments: number | null
  imports: number | null
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

export type DigestKind = 'DAY' | 'WEEK' | 'MONTH'

export const APP_HINTS = [
  'WELCOME', 'OPERATIONS', 'REPORTS', 'MORE', 'OPERATION_FORM', 'GROUP', 'MANUAL_ENTRY', 'ASK',
  'CATEGORIES', 'GROUPS', 'RECURRING', 'REMINDERS', 'DEBTS', 'IMPORT',
] as const

export type AppHint = typeof APP_HINTS[number]

export interface Me {
  userId: number
  firstName: string | null
  username: string | null
  language: string
  timezone: string | null
  activeGroupId: string | null
  seenHints: AppHint[]
  mutedDigests: DigestKind[]
  recurringHeadsUp: boolean
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
  noticeLevel: GroupNoticeLevel
  splitExpenses: boolean
}

export type GroupNoticeLevel = 'ALL' | 'IMPORTANT' | 'OFF'

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
  payerId?: number | null
  participantIds?: number[] | null
}

export interface OperationsPage {
  items: Operation[]
  page: number
  size: number
  totalItems: number
  totals?: OperationTotals | null
}

export interface OperationTotals {
  income: string
  expense: string
  count: number
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
  dailyExpense: DailyAmount[]
}

export interface DailyAmount {
  date: string
  total: string
}

export interface Category {
  id: string
  name: string
  icon: string
  type: CategoryType
  monthlyLimit: string | null
  rollover: boolean
}

export interface Budget {
  categoryId: string
  icon: string
  name: string
  limit: string
  carriedOver: string
  spent: string
  remaining: string
  percent: number
  from: string
  to: string
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
  repeat?: Repeat | null
  payerId?: number | null
  participantIds?: number[] | null
}

export type DebtItemKind = 'EXPENSE' | 'SETTLEMENT' | 'OFFSET' | 'EARLIER'

export interface DebtItem {
  kind: DebtItemKind
  amount: string
  date: string | null
  operationId: string | null
  icon: string | null
  title: string | null
}

export interface PersonalDebt {
  userId: number
  name: string
  amount: string
  items: DebtItem[]
}

export interface DebtTransfer {
  fromId: number
  fromName: string
  toId: number
  toName: string
  amount: string
}

export interface Settlement {
  id: string
  fromId: number
  fromName: string
  toId: number
  toName: string
  amount: string
  date: string
  canUndo: boolean
}

export type LoanDirection = 'LENT' | 'BORROWED'

export interface Repayment {
  id: string
  amount: string
  date: string
}

export interface Loan {
  id: string
  direction: LoanDirection
  person: string
  amount: string
  repaid: string
  remaining: string
  lentOn: string
  dueDate: string | null
  note: string | null
  repayments: Repayment[]
  open: boolean
}

export interface VoiceLoan {
  kind: 'NEW' | 'REPAYMENT'
  direction: LoanDirection
  person: string
  amount: string | null
  dueDate: string | null
  note: string | null
  loanId: string | null
  remaining: string | null
}

export interface Debts {
  balance: string
  owedToMe: string
  iOwe: string
  mine: PersonalDebt[]
  others: DebtTransfer[]
  history: Settlement[]
  loans: Loan[]
}

export interface VoiceResult {
  operations: VoiceOperation[]
  answer?: AnswerCard | null
  loan?: VoiceLoan | null
}

export type AnswerScreen = 'REPORTS' | 'OPERATIONS' | 'CATEGORIES' | 'RECURRING'

export interface AnswerLink {
  screen: AnswerScreen
  month: string | null
  search: string | null
}

export interface AnswerBar {
  label: string
  value: string
  current: boolean
}

export interface AnswerRow {
  icon: string
  label: string
  value: string
  note: string | null
}

export interface AnswerCard {
  title: string
  headline: string | null
  lines: string[]
  bars: AnswerBar[]
  rows: AnswerRow[]
  followUps: string[]
  link: AnswerLink | null
  understood: boolean
}

export type AskTopic = 'SUM' | 'COMPARE' | 'WHERE' | 'BUDGETS' | 'RECURRING'

export interface AskTopicSuggestions {
  topic: AskTopic
  title: string
  questions: string[]
}

export interface AskSuggestions {
  questions: string[]
  topics: AskTopicSuggestions[]
}

export interface CategoryRule {
  id: string
  place: string
  categoryId: string
  categoryName: string
  categoryIcon: string
}

export type RecurrenceUnit = 'DAY' | 'WEEK' | 'MONTH' | 'YEAR'

export type RecurringMode = 'AUTO' | 'REMIND'

export interface Repeat {
  unit: RecurrenceUnit
  interval: number
}

export interface RecurringPayment extends Repeat {
  id: string
  type: CategoryType
  amount: string
  categoryId: string
  categoryName: string
  categoryIcon: string
  description: string | null
  startDate: string
  endDate: string | null
  nextDate: string
  mode: RecurringMode
  active: boolean
  monthlyAmount: string
}

export interface RecurringList {
  items: RecurringPayment[]
  monthlyExpense: string
  monthlyIncome: string
  activeByMe: number
}

export type ColumnRole = 'DATE' | 'AMOUNT' | 'EXPENSE' | 'INCOME' | 'TYPE' | 'CATEGORY' | 'INCOME_CATEGORY' | 'COMMENT' | 'CURRENCY'

export type ImportSource = 'ZENMONEY' | 'COINKEEPER' | 'MONEY_MANAGER' | 'DETECTED' | 'AI' | 'MANUAL'

export type SkipReason = 'TRANSFER' | 'CURRENCY' | 'INVALID' | 'DUPLICATE'

export interface ImportColumn {
  header: string
  samples: string[]
  role: ColumnRole | null
}

export interface ImportCategory {
  key: string
  name: string
  type: CategoryType
  count: number
  existingId: string | null
}

export interface ImportPreview {
  token: string
  source: ImportSource | null
  columns: ImportColumn[]
  needsColumns: boolean
  count: number
  from: string | null
  to: string | null
  skipped: Partial<Record<SkipReason, number>>
  categories: ImportCategory[]
  importsLeft: number | null
}

export type CategoryChoice = { action: 'CREATE' } | { action: 'SKIP' } | { action: 'EXISTING'; categoryId: string }

export interface ImportResult {
  importId: string
  count: number
  createdCategories: number
  from: string | null
  to: string | null
  historyLimited: boolean
}
