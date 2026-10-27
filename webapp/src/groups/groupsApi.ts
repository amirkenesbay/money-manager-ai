import { apiRequest } from '../api/client'
import type { Currency, Group, Invite, Member } from '../api/types'

const groupPath = (groupId: string) => `/groups/${groupId}`

export const fetchGroups = (signal?: AbortSignal): Promise<Group[]> => apiRequest<Group[]>('/groups', {}, signal)

export const activateGroup = (groupId: string): Promise<void> =>
  apiRequest<void>(`${groupPath(groupId)}/activate`, { method: 'POST' })

export const createGroup = (name: string, currency: Currency): Promise<Group> =>
  apiRequest<Group>('/groups', { method: 'POST', body: JSON.stringify({ name, currency }) })

export const updateGroup = (groupId: string, changes: { name?: string; currency?: Currency }): Promise<Group> =>
  apiRequest<Group>(groupPath(groupId), { method: 'PATCH', body: JSON.stringify(changes) })

export const deleteGroup = (groupId: string): Promise<void> =>
  apiRequest<void>(groupPath(groupId), { method: 'DELETE' })

export const fetchMembers = (groupId: string, signal?: AbortSignal): Promise<Member[]> =>
  apiRequest<Member[]>(`${groupPath(groupId)}/members`, {}, signal)

export const fetchInvite = (groupId: string, signal?: AbortSignal): Promise<Invite> =>
  apiRequest<Invite>(`${groupPath(groupId)}/invite`, {}, signal)

export const setInitialBalance = (groupId: string, amount: string): Promise<Group> =>
  apiRequest<Group>(`${groupPath(groupId)}/initial-balance`, { method: 'PUT', body: JSON.stringify({ amount }) })
