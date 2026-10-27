import { apiRequest } from '../api/client'
import type { AnalyticsReport, CategoryReport, ComparisonReport, MembersReport } from '../api/types'

const reportsPath = (groupId: string) => `/groups/${groupId}/reports`

export const fetchAnalytics = (groupId: string, month: string, signal?: AbortSignal): Promise<AnalyticsReport> =>
  apiRequest<AnalyticsReport>(`${reportsPath(groupId)}/analytics?month=${month}`, {}, signal)

export const fetchComparison = (groupId: string, month: string, signal?: AbortSignal): Promise<ComparisonReport> =>
  apiRequest<ComparisonReport>(`${reportsPath(groupId)}/comparison?month=${month}`, {}, signal)

export const fetchMembers = (groupId: string, month: string, signal?: AbortSignal): Promise<MembersReport> =>
  apiRequest<MembersReport>(`${reportsPath(groupId)}/members?month=${month}`, {}, signal)

export const fetchCategoryReport = (groupId: string, categoryId: string, signal?: AbortSignal): Promise<CategoryReport> =>
  apiRequest<CategoryReport>(`${reportsPath(groupId)}/category?categoryId=${categoryId}`, {}, signal)
