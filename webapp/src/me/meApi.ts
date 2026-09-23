import { apiRequest } from '../api/client'
import type { Me } from '../api/types'

export const fetchMe = (signal?: AbortSignal): Promise<Me> => apiRequest<Me>('/me', {}, signal)
