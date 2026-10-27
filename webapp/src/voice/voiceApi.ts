import { apiRequest } from '../api/client'
import type { VoiceResult } from '../api/types'
import { WAV_MIME_TYPE } from './wav'

export const parseVoice = (groupId: string, wav: Blob, signal?: AbortSignal): Promise<VoiceResult> =>
  apiRequest<VoiceResult>(`/groups/${groupId}/voice`, { method: 'POST', body: wav, headers: { 'Content-Type': WAV_MIME_TYPE } }, signal)
