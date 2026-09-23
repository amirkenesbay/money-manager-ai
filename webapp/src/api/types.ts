export type ApiErrorCode =
  | 'BAD_REQUEST'
  | 'UNAUTHORIZED'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
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
