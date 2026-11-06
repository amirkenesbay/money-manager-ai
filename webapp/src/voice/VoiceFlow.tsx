import { type ReactNode, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { ApiRequestError, serverErrorCode, serverMessage } from '../api/client'
import type { AnswerCard, AnswerLink, Group, Me, Operation, VoiceLoan, VoiceResult } from '../api/types'
import { LoanConfirm } from '../debts/LoanConfirm'
import { askQuestion } from '../ask/askApi'
import { AnswerView } from '../ask/AnswerView'
import { hintItems } from '../ask/hintItems'
import { RotatingHint } from '../ask/RotatingHint'
import { useAskSuggestions } from '../ask/useAskSuggestions'
import { strings } from '../strings'
import { notifyOutcome } from '../telegram/haptics'
import { openExternalLink } from '../telegram/telegramEnv'
import { Button } from '../ui/Button'
import { ThinkingDots } from '../ui/ThinkingDots'
import { TypedText } from '../ui/TypedText'
import { ScreenHeader } from '../ui/ScreenHeader'
import { Sheet } from '../ui/Sheet'
import { StateView } from '../ui/StateView'
import { prefersLessMotion } from '../ui/motion'
import { isFlagStored, storeFlag } from '../ui/storedFlag'
import { useConfirmClosing } from '../ui/useConfirmClosing'
import { type Draft, toDrafts } from './drafts'
import { microphoneErrorOf, microphoneSupported, type Recorder, startRecording } from './recorder'
import { VoiceConfirm } from './VoiceConfirm'
import { parseVoice } from './voiceApi'
import './voice.css'

const MAX_RECORDING_SECONDS = 60
const WARNING_SECONDS = 10
const SECOND_MS = 1000
const EXPLAINED_KEY = 'voice.microphoneExplained'
const PROBLEM_ICON = '🎙️'
const LEVEL_PROPERTY = '--voice-level'


type Problem = 'denied' | 'unsupported' | 'silence' | 'notUnderstood' | 'aiLimit' | 'aiBusy' | 'network'

type Request = { kind: 'processing'; wav: Blob } | { kind: 'asking'; text: string }

type Phase =
  | { kind: 'ask' }
  | { kind: 'recording' }
  | { kind: 'paused'; wav: Blob }
  | Request
  | { kind: 'confirm'; drafts: Draft[] }
  | { kind: 'answer'; card: AnswerCard }
  | { kind: 'loan'; loan: VoiceLoan }
  | { kind: 'problem'; problem: Problem; retry: Request | null; message: string | null }

export type VoiceStart = { kind: 'record' } | { kind: 'question'; text: string }

export function VoiceFlow({ me, group, start = { kind: 'record' }, onClose, onManual, onSaved, onLoanSaved, onOpenLink }: {
  me: Me
  group: Group
  start?: VoiceStart
  onClose: () => void
  onManual: () => void
  onSaved: (operations: Operation[], note?: string) => void
  onLoanSaved: (title: string, detail: string) => void
  onOpenLink: (link: AnswerLink) => void
}) {
  const [phase, setPhase] = useState<Phase>(() => initialPhase(start))
  const problem = (kind: Problem, retry: Request | null = null, message: string | null = null) => setPhase({ kind: 'problem', problem: kind, retry, message })
  const record = () => setPhase({ kind: 'recording' })
  const ask = (text: string) => setPhase({ kind: 'asking', text })

  switch (phase.kind) {
    case 'ask':
      return (
        <Sheet label={strings.voiceAskTitle} onClose={onClose}>
          <h2 className="sheet-title">{strings.voiceAskTitle}</h2>
          <p className="hint">{strings.voiceAskBody}</p>
          <Button onClick={() => { storeFlag(EXPLAINED_KEY); record() }}>{strings.voiceAskAllow}</Button>
          <Button variant="secondary" onClick={onClose}>{strings.voiceAskLater}</Button>
        </Sheet>
      )
    case 'recording':
      return (
        <RecordingScreen
          groupId={group.id}
          onAsk={ask}
          onRecorded={(wav) => setPhase({ kind: 'processing', wav })}
          onPaused={(wav) => setPhase({ kind: 'paused', wav })}
          onSilence={() => problem('silence')}
          onMicrophoneError={(error) => problem(error)}
          onCancel={onClose}
        />
      )
    case 'paused':
      return (
        <ProblemScreen icon={PROBLEM_ICON} title={strings.pausedTitle} body={strings.pausedBody} onBack={onClose}>
          <Button onClick={() => setPhase({ kind: 'processing', wav: phase.wav })}>{strings.sendRecording}</Button>
          <Button variant="secondary" onClick={onClose}>{strings.discardRecording}</Button>
        </ProblemScreen>
      )
    case 'processing':
    case 'asking':
      return (
        <ThinkingScreen
          groupId={group.id}
          request={phase}
          onDone={(next) => (next === null ? problem('notUnderstood') : setPhase(next))}
          onFailed={(kind, message) => problem(kind, phase, message)}
          onCancel={onClose}
        />
      )
    case 'loan':
      return <LoanConfirm group={group} language={me.language} loan={phase.loan} onSayAgain={record} onClose={onClose} onSaved={onLoanSaved} />
    case 'answer':
      return (
        <AnswerView
          card={phase.card}
          currency={group.currency}
          language={me.language}
          onAsk={ask}
          onRecord={record}
          onOpenLink={onOpenLink}
          onClose={onClose}
        />
      )
    case 'confirm':
      return <VoiceConfirm group={group} userId={me.userId} language={me.language} initialDrafts={phase.drafts} onSayAgain={record} onClose={onClose} onSaved={onSaved} />
    default:
      return (
        <VoiceProblem
          problem={phase.problem}
          message={phase.message}
          subscribeUrl={me.subscription.subscribeUrl}
          onRecord={record}
          onResend={phase.retry ? () => setPhase(phase.retry as Request) : undefined}
          onManual={onManual}
          onClose={onClose}
        />
      )
  }
}

function initialPhase(start: VoiceStart): Phase {
  if (start.kind === 'question') return { kind: 'asking', text: start.text }
  return isFlagStored(EXPLAINED_KEY) ? { kind: 'recording' } : { kind: 'ask' }
}

function RecordingScreen({ groupId, onAsk, onRecorded, onPaused, onSilence, onMicrophoneError, onCancel }: {
  groupId: string
  onAsk: (question: string) => void
  onRecorded: (wav: Blob) => void
  onPaused: (wav: Blob) => void
  onSilence: () => void
  onMicrophoneError: (error: 'denied' | 'unsupported') => void
  onCancel: () => void
}) {
  const recorder = useRef<Recorder | null>(null)
  const [seconds, setSeconds] = useState(0)
  const [ready, setReady] = useState(false)
  const level = useCallback(() => recorder.current?.level() ?? 0, [])
  const suggestions = useAskSuggestions(groupId)
  const hints = useMemo(() => hintItems(strings.voiceExamples, suggestions?.questions ?? []), [suggestions])
  useConfirmClosing(true)

  const finish = useCallback((deliver: (wav: Blob) => void) => {
    const active = recorder.current
    if (!active) return
    recorder.current = null
    const recording = active.stop()
    if (recording.kind === 'silence') onSilence()
    else deliver(recording.wav)
  }, [onSilence])

  useEffect(() => {
    let cancelled = false
    if (!microphoneSupported()) {
      onMicrophoneError('unsupported')
      return undefined
    }
    startRecording()
      .then((started) => {
        if (cancelled) {
          started.cancel()
          return
        }
        recorder.current = started
        setReady(true)
        notifyOutcome('success')
      })
      .catch((error: unknown) => {
        if (!cancelled) onMicrophoneError(microphoneErrorOf(error))
      })
    return () => {
      cancelled = true
      recorder.current?.cancel()
      recorder.current = null
    }
  }, [onMicrophoneError])

  useEffect(() => {
    if (!ready) return undefined
    const timer = setInterval(() => setSeconds((value) => value + 1), SECOND_MS)
    return () => clearInterval(timer)
  }, [ready])

  useEffect(() => {
    if (seconds >= MAX_RECORDING_SECONDS) finish(onRecorded)
  }, [seconds, finish, onRecorded])

  useEffect(() => {
    const pauseWhenHidden = () => {
      if (document.visibilityState === 'hidden') finish(onPaused)
    }
    document.addEventListener('visibilitychange', pauseWhenHidden)
    return () => document.removeEventListener('visibilitychange', pauseWhenHidden)
  }, [finish, onPaused])

  const left = MAX_RECORDING_SECONDS - seconds
  return (
    <>
      <ScreenHeader title={strings.recordVoice} onBack={onCancel} />
      <section className="voice-stage">
        <BreathingOrb level={level} />
        <p className="voice-listening">{strings.recordingTitle}</p>
        <RotatingHint items={hints} onAsk={onAsk} />
        {left <= WARNING_SECONDS && <p className="caption negative" role="timer">{strings.recordingEnds(left)}</p>}
        <button type="button" className="voice-stop-round pressable" aria-label={strings.stopRecording} disabled={!ready} onClick={() => finish(onRecorded)}>
          <span className="voice-stop-square" aria-hidden="true" />
        </button>
      </section>
      <Button variant="secondary" onClick={onCancel}>{strings.cancel}</Button>
    </>
  )
}

function BreathingOrb({ level }: { level: () => number }) {
  const orb = useRef<HTMLSpanElement>(null)

  useEffect(() => {
    if (prefersLessMotion()) return undefined
    let frame = 0
    const follow = () => {
      orb.current?.style.setProperty(LEVEL_PROPERTY, String(level()))
      frame = requestAnimationFrame(follow)
    }
    frame = requestAnimationFrame(follow)
    return () => cancelAnimationFrame(frame)
  }, [level])

  return <span ref={orb} className="voice-breath" aria-hidden="true" />
}

function ThinkingScreen({ groupId, request, onDone, onFailed, onCancel }: {
  groupId: string
  request: Request
  onDone: (next: Phase | null) => void
  onFailed: (problem: Problem, message: string | null) => void
  onCancel: () => void
}) {
  const callbacks = useRef({ onDone, onFailed })

  useEffect(() => {
    callbacks.current = { onDone, onFailed }
  })

  const inFlight = useRef<InFlight | null>(null)

  useEffect(() => {
    const flight = inFlight.current?.request === request ? inFlight.current : send(groupId, request)
    inFlight.current = flight
    clearTimeout(flight.abortTimer)
    let attached = true
    flight.pending
      .then((next) => attached && callbacks.current.onDone(next))
      .catch((error: unknown) => {
        if (!attached || flight.controller.signal.aborted) return
        notifyOutcome('error')
        callbacks.current.onFailed(problemOf(error), serverMessage(error))
      })
    return () => {
      attached = false
      flight.abortTimer = setTimeout(() => flight.controller.abort())
    }
  }, [groupId, request])

  const asking = request.kind === 'asking'
  return (
    <>
      <ScreenHeader title={asking ? strings.askingTitle : strings.processingTitle} onBack={onCancel} />
      <section className="voice-stage" aria-busy="true">
        {asking && <p className="hint centered"><TypedText text={strings.quoted(request.text)} /></p>}
        <ThinkingDots stages={asking ? strings.askingStages : strings.processingStages} />
      </section>
      <Button variant="secondary" onClick={onCancel}>{strings.cancel}</Button>
    </>
  )
}

interface InFlight {
  request: Request
  controller: AbortController
  pending: Promise<Phase | null>
  abortTimer?: ReturnType<typeof setTimeout>
}

function send(groupId: string, request: Request): InFlight {
  const controller = new AbortController()
  const pending = request.kind === 'processing'
    ? parseVoice(groupId, request.wav, controller.signal).then(voiceOutcome)
    : askQuestion(groupId, request.text, controller.signal).then((card): Phase => ({ kind: 'answer', card }))
  return { request, controller, pending }
}

function voiceOutcome(result: VoiceResult): Phase | null {
  const drafts = toDrafts(result.operations)
  if (drafts.length > 0) return { kind: 'confirm', drafts }
  if (result.loan) return { kind: 'loan', loan: result.loan }
  return result.answer ? { kind: 'answer', card: result.answer } : null
}

function problemOf(error: unknown): Problem {
  const code = serverErrorCode(error)
  if (code === 'AI_LIMIT_REACHED') return 'aiLimit'
  if (code === 'AI_BUSY') return 'aiBusy'
  return error instanceof ApiRequestError && error.status !== null && code !== 'AI_UNAVAILABLE' && code !== 'INTERNAL_ERROR' ? 'notUnderstood' : 'network'
}

function VoiceProblem({ problem, message, subscribeUrl, onRecord, onResend, onManual, onClose }: {
  problem: Problem
  message: string | null
  subscribeUrl: string
  onRecord: () => void
  onResend?: () => void
  onManual: () => void
  onClose: () => void
}) {
  const manual = <Button variant="secondary" onClick={onManual}>{strings.manualEntryLabel}</Button>
  switch (problem) {
    case 'denied':
    case 'unsupported':
      return (
        <ProblemScreen icon={PROBLEM_ICON} title={strings.micDeniedTitle} body={problem === 'denied' ? strings.micDeniedBody : strings.micUnsupportedBody} onBack={onClose}>
          {problem === 'denied' && <ol className="voice-steps">{strings.micDeniedSteps.map((step) => <li key={step}>{step}</li>)}</ol>}
          {problem === 'denied' && <Button onClick={onRecord}>{strings.retry}</Button>}
          {manual}
        </ProblemScreen>
      )
    case 'silence':
      return (
        <ProblemScreen icon={PROBLEM_ICON} title={strings.silenceTitle} body={strings.silenceBody} onBack={onClose}>
          <Button onClick={onRecord}>{strings.recordAgain}</Button>
          <Button variant="secondary" onClick={onClose}>{strings.cancel}</Button>
        </ProblemScreen>
      )
    case 'notUnderstood':
      return (
        <ProblemScreen icon={PROBLEM_ICON} title={strings.notUnderstoodTitle} body={strings.notUnderstoodBody} onBack={onClose}>
          <ul className="voice-examples">{strings.voiceExamples.map((example) => <li key={example} className="chip">{example}</li>)}</ul>
          <Button onClick={onRecord}>{strings.sayAgain}</Button>
          {manual}
        </ProblemScreen>
      )
    case 'aiLimit':
      return (
        <ProblemScreen icon={PROBLEM_ICON} title={strings.aiLimitTitle} body={message ?? strings.aiBusyBody} onBack={onClose}>
          <Button onClick={onManual}>{strings.manualEntryLabel}</Button>
          <Button variant="secondary" onClick={() => openExternalLink(subscribeUrl)}>{strings.learnPremium}</Button>
        </ProblemScreen>
      )
    case 'aiBusy':
      return (
        <ProblemScreen icon={PROBLEM_ICON} title={strings.aiBusyTitle} body={strings.aiBusyBody} onBack={onClose}>
          <Button onClick={onManual}>{strings.manualEntryLabel}</Button>
          {onResend && <Button variant="secondary" onClick={onResend}>{strings.sendAgain}</Button>}
        </ProblemScreen>
      )
    default:
      return (
        <ProblemScreen icon={PROBLEM_ICON} title={strings.voiceFailedTitle} body={message ?? strings.voiceFailedBody} onBack={onClose}>
          {onResend && <Button onClick={onResend}>{strings.sendAgain}</Button>}
          <Button variant="secondary" onClick={onRecord}>{strings.recordAgain}</Button>
        </ProblemScreen>
      )
  }
}

function ProblemScreen({ icon, title, body, onBack, children }: {
  icon: string
  title: string
  body: string
  onBack: () => void
  children: ReactNode
}) {
  return (
    <>
      <ScreenHeader title={strings.recordVoice} onBack={onBack} />
      <StateView icon={icon} title={title} body={body} tone="alert">{children}</StateView>
    </>
  )
}
