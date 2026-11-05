import { apiRequest } from '../api/client'
import type { Debts, Settlement } from '../api/types'

const debtsPath = (groupId: string) => `/groups/${groupId}/debts`

export const fetchDebts = (groupId: string, signal?: AbortSignal): Promise<Debts> =>
  apiRequest<Debts>(debtsPath(groupId), {}, signal)

export const recordSettlement = (groupId: string, fromId: number, toId: number, amount: string): Promise<Settlement> =>
  apiRequest<Settlement>(`${debtsPath(groupId)}/settlements`, { method: 'POST', body: JSON.stringify({ fromId, toId, amount }) })

export const undoSettlement = (groupId: string, settlementId: string): Promise<void> =>
  apiRequest<void>(`${debtsPath(groupId)}/settlements/${settlementId}`, { method: 'DELETE' })

export const remindDebtor = (groupId: string, userId: number): Promise<void> =>
  apiRequest<void>(`${debtsPath(groupId)}/reminders`, { method: 'POST', body: JSON.stringify({ userId }) })
