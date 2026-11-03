import { apiRequest } from '../api/client'
import type { CategoryType, Operation, OperationsPage } from '../api/types'
import type { DateRange } from './periods'

export const OPERATIONS_PAGE_SIZE = 20

const operationsPath = (groupId: string) => `/groups/${groupId}/operations`

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
  return apiRequest<OperationsPage>(`${operationsPath(groupId)}?${params}`, {}, signal)
}

export interface OperationInput {
  type: CategoryType
  amount: string
  categoryId: string
  operationDate: string
  description: string | null
  categoryCorrected?: boolean
}

export const createOperation = (groupId: string, input: OperationInput): Promise<Operation> =>
  apiRequest<Operation>(operationsPath(groupId), { method: 'POST', body: JSON.stringify(input) })

export const replaceOperation = (groupId: string, operationId: string, input: OperationInput): Promise<Operation> =>
  apiRequest<Operation>(`${operationsPath(groupId)}/${operationId}`, { method: 'PUT', body: JSON.stringify(input) })

export const deleteOperation = (groupId: string, operationId: string): Promise<void> =>
  apiRequest<void>(`${operationsPath(groupId)}/${operationId}`, { method: 'DELETE' })
