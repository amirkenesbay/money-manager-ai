import { apiRequest } from '../api/client'
import type { CategoryType, RecurrenceUnit, RecurringList, RecurringMode, RecurringPayment } from '../api/types'

const recurringPath = (groupId: string) => `/groups/${groupId}/recurring`

export interface RecurringInput {
  type: CategoryType
  amount: string
  categoryId: string
  description: string | null
  unit: RecurrenceUnit
  interval: number
  startDate: string
  endDate: string | null
  mode: RecurringMode
  firstRecorded?: boolean
}

export const fetchRecurring = (groupId: string, signal?: AbortSignal): Promise<RecurringList> =>
  apiRequest<RecurringList>(recurringPath(groupId), {}, signal)

export const createRecurring = (groupId: string, input: RecurringInput): Promise<RecurringPayment> =>
  apiRequest<RecurringPayment>(recurringPath(groupId), { method: 'POST', body: JSON.stringify(input) })

export const updateRecurring = (groupId: string, paymentId: string, input: RecurringInput): Promise<RecurringPayment> =>
  apiRequest<RecurringPayment>(`${recurringPath(groupId)}/${paymentId}`, { method: 'PUT', body: JSON.stringify(input) })

export const setRecurringActive = (groupId: string, paymentId: string, active: boolean): Promise<RecurringPayment> =>
  apiRequest<RecurringPayment>(`${recurringPath(groupId)}/${paymentId}`, { method: 'PATCH', body: JSON.stringify({ active }) })

export const deleteRecurring = (groupId: string, paymentId: string): Promise<void> =>
  apiRequest<void>(`${recurringPath(groupId)}/${paymentId}`, { method: 'DELETE' })
