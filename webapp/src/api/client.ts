import { rawInitData } from '../telegram/telegramEnv'
import type { ApiErrorBody } from './types'

const API_BASE = '/api/v1'
const AUTH_SCHEME = 'tma'
const JSON_CONTENT_TYPE = 'application/json'

export class ApiRequestError extends Error {
  readonly status: number | null
  readonly body: ApiErrorBody | null

  constructor(message: string, status: number | null, body: ApiErrorBody | null) {
    super(message)
    this.status = status
    this.body = body
  }
}

export async function apiRequest<T>(path: string, init: RequestInit = {}, signal?: AbortSignal): Promise<T> {
  const response = await send(path, init, signal)
  if (!response.ok) {
    const body = await readErrorBody(response)
    throw new ApiRequestError(body?.message ?? response.statusText, response.status, body)
  }
  return (await response.json()) as T
}

async function send(path: string, init: RequestInit, signal?: AbortSignal): Promise<Response> {
  const headers = new Headers(init.headers)
  headers.set('Authorization', `${AUTH_SCHEME} ${rawInitData() ?? ''}`)
  if (init.body !== undefined) headers.set('Content-Type', JSON_CONTENT_TYPE)
  try {
    return await fetch(`${API_BASE}${path}`, { ...init, headers, signal })
  } catch (error) {
    if (signal?.aborted) throw error
    throw new ApiRequestError(String(error), null, null)
  }
}

async function readErrorBody(response: Response): Promise<ApiErrorBody | null> {
  if (!response.headers.get('Content-Type')?.includes(JSON_CONTENT_TYPE)) return null
  try {
    return (await response.json()) as ApiErrorBody
  } catch {
    return null
  }
}
