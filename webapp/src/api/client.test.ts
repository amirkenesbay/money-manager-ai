import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiRequestError, apiRequest } from './client'
import type { ApiErrorBody } from './types'

const RAW_INIT_DATA = 'user=%7B%22id%22%3A42%7D&auth_date=1&hash=abc'

vi.mock('../telegram/telegramEnv', () => ({ rawInitData: () => RAW_INIT_DATA }))

const fetchMock = vi.fn<typeof fetch>()

const jsonResponse = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

describe('apiRequest', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
  })
  afterEach(() => {
    vi.unstubAllGlobals()
    fetchMock.mockReset()
  })

  it('sends Telegram initData with the tma scheme to the versioned API', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, { ok: true }))

    await apiRequest('/me')

    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/v1/me')
    expect(new Headers(init?.headers).get('Authorization')).toBe(`tma ${RAW_INIT_DATA}`)
  })

  it('returns parsed JSON on success', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, { firstName: 'Амир' }))

    await expect(apiRequest('/me')).resolves.toEqual({ firstName: 'Амир' })
  })

  it('exposes the server error format on failure', async () => {
    const body: ApiErrorBody = { code: 'FORBIDDEN', message: 'Вы не состоите в этой группе.', details: {} }
    fetchMock.mockResolvedValue(jsonResponse(403, body))

    const error = await apiRequest('/groups/1').catch((caught: unknown) => caught)

    expect(error).toBeInstanceOf(ApiRequestError)
    expect((error as ApiRequestError).status).toBe(403)
    expect((error as ApiRequestError).body).toEqual(body)
    expect((error as ApiRequestError).message).toBe(body.message)
  })

  it('reports a network failure without a status', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))

    const error = await apiRequest('/me').catch((caught: unknown) => caught)

    expect(error).toBeInstanceOf(ApiRequestError)
    expect((error as ApiRequestError).status).toBeNull()
    expect((error as ApiRequestError).body).toBeNull()
  })
})
