import { apiRequest } from '../api/client'
import type { AnswerCard, AskSuggestions } from '../api/types'

export const askQuestion = (groupId: string, text: string, signal?: AbortSignal): Promise<AnswerCard> =>
  apiRequest<AnswerCard>(`/groups/${groupId}/ask`, { method: 'POST', body: JSON.stringify({ text }) }, signal)

export const fetchAskSuggestions = (groupId: string, signal?: AbortSignal): Promise<AskSuggestions> =>
  apiRequest<AskSuggestions>(`/groups/${groupId}/ask/suggestions`, {}, signal)
