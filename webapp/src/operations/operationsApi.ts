import { apiRequest } from '../api/client'
import type { Category, CategoryType, OperationsPage } from '../api/types'
import type { DateRange } from './periods'

export const OPERATIONS_PAGE_SIZE = 20

export interface OperationsQuery extends DateRange {
  type: CategoryType | null
  categoryId: string | null
}

export const NO_FILTERS: OperationsQuery = { from: null, to: null, type: null, categoryId: null }

export function fetchOperations(
  groupId: string,
  query: OperationsQuery,
  page: number,
  signal?: AbortSignal,
  size: number = OPERATIONS_PAGE_SIZE,
): Promise<OperationsPage> {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (query.from) params.set('from', query.from)
  if (query.to) params.set('to', query.to)
  if (query.type) params.set('type', query.type)
  if (query.categoryId) params.set('categoryId', query.categoryId)
  return apiRequest<OperationsPage>(`/groups/${groupId}/operations?${params}`, {}, signal)
}

export const fetchCategories = (groupId: string, signal?: AbortSignal): Promise<Category[]> =>
  apiRequest<Category[]>(`/groups/${groupId}/categories`, {}, signal)
