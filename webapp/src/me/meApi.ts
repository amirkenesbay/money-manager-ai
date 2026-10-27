import { apiRequest } from '../api/client'
import type { Me } from '../api/types'

export const fetchMe = (signal?: AbortSignal): Promise<Me> => apiRequest<Me>('/me', {}, signal)

export const updateMe = (changes: { language?: string; timezone?: string }): Promise<Me> =>
  apiRequest<Me>('/me', { method: 'PATCH', body: JSON.stringify(changes) })
