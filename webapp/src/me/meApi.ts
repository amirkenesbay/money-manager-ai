import { apiRequest } from '../api/client'
import type { AppHint, Me } from '../api/types'

export const fetchMe = (signal?: AbortSignal): Promise<Me> => apiRequest<Me>('/me', {}, signal)

export const updateMe = (changes: { language?: string; timezone?: string }): Promise<Me> =>
  apiRequest<Me>('/me', { method: 'PATCH', body: JSON.stringify(changes) })

export const markHintSeen = (hint: AppHint): Promise<Me> =>
  apiRequest<Me>('/me/hints', { method: 'POST', body: JSON.stringify({ hint }) })
