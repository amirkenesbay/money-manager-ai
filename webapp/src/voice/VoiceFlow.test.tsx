import { StrictMode } from 'react'
import { formatMoney } from '../format'
import { asRendered } from '../test/text'
import { act, render, screen } from '@testing-library/react'
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
import { askQuestion, fetchAskSuggestions } from '../ask/askApi'
import { createLoan } from '../debts/loansApi'
import type { AnswerCard } from '../api/types'

vi.mock('./voiceApi', () => ({ parseVoice: vi.fn() }))
vi.mock('../ask/askApi', () => ({ askQuestion: vi.fn(), fetchAskSuggestions: vi.fn() }))
vi.mock('../debts/loansApi', () => ({ createLoan: vi.fn(), repayLoan: vi.fn() }))
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
const onOpenLink = vi.fn()
const onLoanSaved = vi.fn()
const QUESTION = 'Сколько на такси в этом месяце?'
const answer = (overrides: Partial<AnswerCard> = {}): AnswerCard => ({
  title: '🚕 Такси · Ноябрь', headline: '12 000 ₸', lines: ['Операций: 4'], bars: [], rows: [],
  followUps: ['А в прошлом месяце?'], link: { screen: 'REPORTS', month: '2026-11', search: null }, understood: true, ...overrides,
})
const flow = (start?: Parameters<typeof VoiceFlow>[0]['start']) =>
  <VoiceFlow me={me()} group={group()} start={start} onClose={vi.fn()} onManual={onManual} onSaved={onSaved} onLoanSaved={onLoanSaved} onOpenLink={onOpenLink} />
const renderFlow = (start?: Parameters<typeof VoiceFlow>[0]['start']) => render(flow(start))

async function recordPhrase(strict = false) {
  const user = userEvent.setup()
  render(strict ? <StrictMode>{flow()}</StrictMode> : flow())
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
    vi.mocked(fetchAskSuggestions).mockResolvedValue({ questions: [QUESTION], topics: [] })
  })

  it('answers a spoken question with a card and asks a follow-up by text', async () => {
    vi.mocked(parseVoice).mockResolvedValue({ operations: [], answer: answer() })
    vi.mocked(askQuestion).mockResolvedValue(answer({ title: '🚕 Такси · Октябрь', followUps: [] }))
    const user = await recordPhrase()

    expect(await screen.findByRole('heading', { name: '🚕 Такси · Ноябрь' })).toBeInTheDocument()
    expect(screen.getByText('12 000 ₸')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'А в прошлом месяце?' }))

    expect(await screen.findByRole('heading', { name: '🚕 Такси · Октябрь' })).toBeInTheDocument()
    expect(askQuestion).toHaveBeenCalledWith(group().id, 'А в прошлом месяце?', expect.anything())
  })

  it('saves a spoken loan with its due date', async () => {
    vi.mocked(parseVoice).mockResolvedValue({
      operations: [],
      loan: { kind: 'NEW', direction: 'LENT', person: 'Ерлан', amount: '20000', dueDate: '2026-11-30', note: 'на ремонт', loanId: null, remaining: null },
    })
    vi.mocked(createLoan).mockResolvedValue({
      id: 'loan-1', direction: 'LENT', person: 'Ерлан', amount: '20000', repaid: '0', remaining: '20000', lentOn: '2026-11-05', dueDate: '2026-11-30', note: 'на ремонт', repayments: [], open: true,
    })
    const user = await recordPhrase()

    expect(await screen.findByText(strings.loanKind(true))).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: strings.save }))

    expect(createLoan).toHaveBeenCalledWith(group().id, { direction: 'LENT', person: 'Ерлан', amount: '20000', dueDate: '2026-11-30', note: 'на ремонт' })
    expect(onLoanSaved).toHaveBeenCalledWith(strings.loanSaved, expect.stringContaining('Ерлан'))
  })

  it('opens the screen the answer points to', async () => {
    vi.mocked(askQuestion).mockResolvedValue(answer())
    const user = userEvent.setup()
    renderFlow({ kind: 'question', text: QUESTION })

    await user.click(await screen.findByRole('button', { name: strings.answerLinks.REPORTS }))

    expect(onOpenLink).toHaveBeenCalledWith({ screen: 'REPORTS', month: '2026-11', search: null })
  })

  it('asks a suggested question straight from the recording screen', async () => {
    vi.mocked(askQuestion).mockResolvedValue(answer())
    localStorage.setItem('voice.microphoneExplained', '1')
    const user = userEvent.setup()
    renderFlow()
    await screen.findByRole('button', { name: strings.stopRecording })

    await user.click(await screen.findByRole('button', { name: strings.quoted(QUESTION) }, { timeout: 4000 }))

    expect(await screen.findByRole('heading', { name: '🚕 Такси · Ноябрь' })).toBeInTheDocument()
    expect(askQuestion).toHaveBeenCalledWith(group().id, QUESTION, expect.anything())
  }, 8000)

  it('asks to wait until the microphone really records, then says it is listening', async () => {
    let ready: (recorder: Recorder) => void = () => undefined
    vi.mocked(startRecording).mockReturnValue(new Promise((resolve) => { ready = resolve }))
    const user = userEvent.setup()
    renderFlow()

    await user.click(screen.getByRole('button', { name: strings.voiceAskAllow }))

    expect(screen.getByRole('status')).toHaveTextContent(strings.microphoneStarting)
    expect(screen.getByRole('button', { name: strings.stopRecording })).toBeDisabled()
    await act(async () => ready(recorderReturning({ kind: 'audio', wav })))
    expect(screen.getByRole('status')).toHaveTextContent(strings.recordingTitle)
  })

  it('sends the phrase on its own once the user stops talking', async () => {
    let speechEnded: () => void = () => undefined
    vi.mocked(startRecording).mockImplementation(async (onSpeechEnd) => {
      speechEnded = onSpeechEnd ?? speechEnded
      return recorderReturning({ kind: 'audio', wav })
    })
    vi.mocked(parseVoice).mockResolvedValue({ operations: [voiceOperation()] })
    const user = userEvent.setup()
    renderFlow()

    await user.click(screen.getByRole('button', { name: strings.voiceAskAllow }))
    expect(await screen.findByText(strings.recordingAutoStop)).toBeInTheDocument()
    await act(async () => speechEnded())

    expect(await screen.findByRole('heading', { name: strings.confirmOneTitle })).toBeInTheDocument()
    expect(parseVoice).toHaveBeenCalledTimes(1)
  })

  it('sends a recording once even when React mounts the screen twice', async () => {
    vi.mocked(parseVoice).mockResolvedValue({ operations: [voiceOperation()] })

    await recordPhrase(true)

    expect(await screen.findByRole('heading', { name: strings.confirmOneTitle })).toBeInTheDocument()
    expect(parseVoice).toHaveBeenCalledTimes(1)
  })

  it('flags an operation the recognizer returned twice so it can be removed', async () => {
    vi.mocked(parseVoice).mockResolvedValue({ operations: [voiceOperation(), voiceOperation()] })

    const user = await recordPhrase()

    expect(await screen.findByText(strings.repeatedDraft)).toBeInTheDocument()
    await user.click(screen.getAllByRole('button', { name: strings.removeOperation })[1])
    expect(screen.queryByText(strings.repeatedDraft)).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: strings.save })).toBeInTheDocument()
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

  it('keeps the dollars a phrase was said in', async () => {
    vi.mocked(parseVoice).mockResolvedValue({ operations: [voiceOperation({ amount: '2421.5', original: { amount: '5', currency: 'USD' } })] })
    vi.mocked(createOperation).mockResolvedValue(operation({ id: 'voice-op', amount: '2421.5' }))

    const user = await recordPhrase()

    expect(await screen.findByText(asRendered(formatMoney('5', 'USD', 'ru')))).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: strings.save }))

    expect(createOperation).toHaveBeenCalledWith(PERSONAL_GROUP_ID, expect.objectContaining({ amount: '2421.5', original: { amount: '5', currency: 'USD' } }))
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
