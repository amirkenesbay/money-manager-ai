import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiRequestError } from '../api/client'
import type { VoiceOperation } from '../api/types'
import { createCategory, fetchCategories } from '../categories/categoriesApi'
import { createOperation } from '../operations/operationsApi'
import { createRecurring } from '../recurring/recurringApi'
import { strings } from '../strings'
import { category, group, me, operation, PERSONAL_GROUP_ID } from '../test/fixtures'
import { type Recorder, startRecording } from './recorder'
import { parseVoice } from './voiceApi'
import { VoiceFlow } from './VoiceFlow'

vi.mock('./voiceApi', () => ({ parseVoice: vi.fn() }))
vi.mock('./recorder', () => ({
  startRecording: vi.fn(),
  microphoneSupported: () => true,
  microphoneErrorOf: () => 'denied',
}))
vi.mock('../categories/categoriesApi', () => ({ fetchCategories: vi.fn(), createCategory: vi.fn() }))
vi.mock('../operations/operationsApi', () => ({ createOperation: vi.fn() }))
vi.mock('../recurring/recurringApi', () => ({ createRecurring: vi.fn() }))
vi.mock('../telegram/telegramEnv', () => ({ openExternalLink: vi.fn(), rawInitData: () => undefined }))

const wav = new Blob(['RIFF'], { type: 'audio/wav' })
const onSaved = vi.fn()
const onManual = vi.fn()
const taxi = category({ id: 'cat-taxi', name: 'Такси', icon: '🚕' })
const voiceOperation = (overrides: Partial<VoiceOperation> = {}): VoiceOperation => ({
  type: 'EXPENSE',
  amount: '2000.0',
  categoryId: taxi.id,
  categoryName: taxi.name,
  categoryIcon: taxi.icon,
  isNewCategory: false,
  operationDate: '2026-10-28',
  description: null,
  ...overrides,
})
const recorderReturning = (recording: ReturnType<Recorder['stop']>): Recorder => ({ stop: () => recording, level: () => 0, cancel: vi.fn() })
const renderFlow = () => render(<VoiceFlow me={me()} group={group()} onClose={vi.fn()} onManual={onManual} onSaved={onSaved} />)

async function recordPhrase() {
  const user = userEvent.setup()
  renderFlow()
  await user.click(screen.getByRole('button', { name: strings.voiceAskAllow }))
  await user.click(await screen.findByRole('button', { name: strings.stopRecording }))
  return user
}

describe('VoiceFlow', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    localStorage.clear()
    vi.mocked(startRecording).mockResolvedValue(recorderReturning({ kind: 'audio', wav }))
    vi.mocked(fetchCategories).mockResolvedValue([taxi])
  })

  it('records a phrase, shows the parsed operation and saves it', async () => {
    vi.mocked(parseVoice).mockResolvedValue({ operations: [voiceOperation()] })
    const saved = operation({ id: 'voice-op', amount: '2000', categoryName: 'Такси' })
    vi.mocked(createOperation).mockResolvedValue(saved)

    const user = await recordPhrase()

    expect(await screen.findByRole('heading', { name: strings.confirmOneTitle })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: strings.save }))

    expect(parseVoice).toHaveBeenCalledWith(PERSONAL_GROUP_ID, wav, expect.anything())
    expect(createOperation).toHaveBeenCalledWith(PERSONAL_GROUP_ID, expect.objectContaining({ amount: '2000.0', categoryId: taxi.id, categoryCorrected: false }))
    expect(onSaved).toHaveBeenCalledWith([saved], undefined)
  })

  it('keeps a phrase that repeats as a recurring payment', async () => {
    vi.mocked(parseVoice).mockResolvedValue({ operations: [voiceOperation({ repeat: { unit: 'MONTH', interval: 1 } })] })
    const saved = operation({ id: 'voice-op', amount: '2000', categoryName: 'Такси' })
    vi.mocked(createOperation).mockResolvedValue(saved)
    vi.mocked(createRecurring).mockResolvedValue({} as Awaited<ReturnType<typeof createRecurring>>)

    const user = await recordPhrase()

    expect(await screen.findByRole('button', { name: `${strings.repeatLabel}: ${strings.scheduleEveryMonth}` })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: strings.save }))

    expect(createRecurring).toHaveBeenCalledWith(PERSONAL_GROUP_ID, expect.objectContaining({ unit: 'MONTH', firstRecorded: true }))
    expect(onSaved).toHaveBeenCalledWith([saved], strings.repeatSavedToast(strings.scheduleEveryMonth))
  })

  it('creates a suggested category once for several operations', async () => {
    const cafe = voiceOperation({ categoryId: null, categoryName: 'Кафе', categoryIcon: '☕', isNewCategory: true })
    vi.mocked(parseVoice).mockResolvedValue({ operations: [cafe, { ...cafe, amount: '700' }] })
    vi.mocked(createCategory).mockResolvedValue(category({ id: 'cat-cafe', name: 'Кафе', icon: '☕' }))
    vi.mocked(createOperation).mockResolvedValue(operation())

    const user = await recordPhrase()

    expect(await screen.findByRole('heading', { name: strings.confirmManyTitle })).toBeInTheDocument()
    expect(screen.getAllByText(strings.newCategoryNote('Кафе'))).toHaveLength(2)
    await user.click(screen.getByRole('button', { name: strings.saveCount(2) }))

    expect(createCategory).toHaveBeenCalledTimes(1)
    expect(createOperation).toHaveBeenCalledTimes(2)
    expect(createOperation).toHaveBeenLastCalledWith(PERSONAL_GROUP_ID, expect.objectContaining({ amount: '700', categoryId: 'cat-cafe' }))
  })

  it('asks to pick a category the phrase did not name and remembers the pick', async () => {
    vi.mocked(parseVoice).mockResolvedValue({ operations: [voiceOperation({ categoryId: null, categoryName: null, categoryIcon: null })] })
    vi.mocked(createOperation).mockResolvedValue(operation())

    const user = await recordPhrase()

    expect(await screen.findByRole('button', { name: strings.save })).toBeDisabled()
    await user.click(screen.getByRole('button', { name: `${strings.categoryLabel}: ${strings.pickCategory}` }))
    await user.click(await screen.findByRole('button', { name: /Такси/ }))

    expect(screen.getByRole('button', { name: strings.save })).toBeEnabled()
    await user.click(screen.getByRole('button', { name: strings.save }))
    expect(createOperation).toHaveBeenCalledWith(PERSONAL_GROUP_ID, expect.objectContaining({ categoryId: taxi.id, categoryCorrected: true }))
  })

  it('says it heard nothing without spending an AI request', async () => {
    vi.mocked(startRecording).mockResolvedValue(recorderReturning({ kind: 'silence' }))

    await recordPhrase()

    expect(await screen.findByText(strings.silenceTitle)).toBeInTheDocument()
    expect(parseVoice).not.toHaveBeenCalled()
  })

  it('explains a phrase that is not about money', async () => {
    vi.mocked(parseVoice).mockResolvedValue({ operations: [] })

    await recordPhrase()

    expect(await screen.findByText(strings.notUnderstoodTitle)).toBeInTheDocument()
  })

  it('shows the daily AI limit from the server and offers manual entry', async () => {
    const message = 'AI-запросы на сегодня закончились'
    vi.mocked(parseVoice).mockRejectedValue(new ApiRequestError(message, 429, { code: 'AI_LIMIT_REACHED', message, details: { limit: 10 } }))

    const user = await recordPhrase()

    expect(await screen.findByText(message)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: strings.manualEntryLabel }))
    expect(onManual).toHaveBeenCalled()
  })

  it('keeps the recording and resends it after a network failure', async () => {
    vi.mocked(parseVoice)
      .mockRejectedValueOnce(new ApiRequestError('Failed to fetch', null, null))
      .mockResolvedValueOnce({ operations: [voiceOperation()] })

    const user = await recordPhrase()

    await user.click(await screen.findByRole('button', { name: strings.sendAgain }))

    expect(await screen.findByRole('heading', { name: strings.confirmOneTitle })).toBeInTheDocument()
    expect(parseVoice).toHaveBeenCalledTimes(2)
  })

  it('explains how to allow the microphone when access is denied', async () => {
    vi.mocked(startRecording).mockRejectedValue(new DOMException('denied', 'NotAllowedError'))
    const user = userEvent.setup()
    renderFlow()

    await user.click(screen.getByRole('button', { name: strings.voiceAskAllow }))

    expect(await screen.findByText(strings.micDeniedTitle)).toBeInTheDocument()
    expect(screen.getByText(strings.micDeniedSteps[1])).toBeInTheDocument()
  })
})
