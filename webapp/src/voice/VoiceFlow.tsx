import { type ReactNode, useCallback, useEffect, useRef, useState } from 'react'
import { ApiRequestError, serverErrorCode, serverMessage } from '../api/client'
import type { Group, Me, Operation } from '../api/types'
import { strings } from '../strings'
import { notifyOutcome } from '../telegram/haptics'
import { openExternalLink } from '../telegram/telegramEnv'
import { Button } from '../ui/Button'
import { Moment } from '../three/Moment'
import type { MomentInputs } from '../three/moments'
import { MicIcon } from '../ui/icons'
import { ScreenHeader } from '../ui/ScreenHeader'
import { Sheet } from '../ui/Sheet'
import { StateView } from '../ui/StateView'
import { useConfirmClosing } from '../ui/useConfirmClosing'
import { type Draft, toDrafts } from './drafts'
import { microphoneErrorOf, microphoneSupported, type Recorder, startRecording } from './recorder'
import { VoiceConfirm } from './VoiceConfirm'
import { parseVoice } from './voiceApi'
import './voice.css'

const MAX_RECORDING_SECONDS = 60
const WARNING_SECONDS = 10
const SECOND_MS = 1000
const SECONDS_IN_MINUTE = 60
const SECONDS_DIGITS = 2
const EXPLAINED_KEY = 'voice.microphoneExplained'
const PROBLEM_ICON = '🎙️'

const THINKING: MomentInputs['orb'] = { mode: 'thinking', level: () => 0 }

type Problem = 'denied' | 'unsupported' | 'silence' | 'notUnderstood' | 'aiLimit' | 'aiBusy' | 'network'

type Phase =
  | { kind: 'ask' }
  | { kind: 'recording' }
  | { kind: 'paused'; wav: Blob }
  | { kind: 'processing'; wav: Blob }
  | { kind: 'confirm'; drafts: Draft[] }
  | { kind: 'problem'; problem: Problem; wav: Blob | null; message: string | null }

const readExplained = (): boolean => {
  try {
    return localStorage.getItem(EXPLAINED_KEY) !== null
  } catch {
    return false
  }
}

const rememberExplained = (): void => {
  try {
    localStorage.setItem(EXPLAINED_KEY, '1')
  } catch {
    return
  }
}

export function VoiceFlow({ me, group, onClose, onManual, onSaved }: {
  me: Me
  group: Group
  onClose: () => void
  onManual: () => void
  onSaved: (operations: Operation[]) => void
}) {
  const [phase, setPhase] = useState<Phase>(() => (readExplained() ? { kind: 'recording' } : { kind: 'ask' }))
  const problem = (kind: Problem, wav: Blob | null = null, message: string | null = null) => setPhase({ kind: 'problem', problem: kind, wav, message })
  const record = () => setPhase({ kind: 'recording' })

  switch (phase.kind) {
    case 'ask':
      return (
        <Sheet label={strings.voiceAskTitle} onClose={onClose}>
          <h2 className="sheet-title">{strings.voiceAskTitle}</h2>
          <p className="hint">{strings.voiceAskBody}</p>
          <Button onClick={() => { rememberExplained(); record() }}>{strings.voiceAskAllow}</Button>
          <Button variant="secondary" onClick={onClose}>{strings.voiceAskLater}</Button>
        </Sheet>
      )
    case 'recording':
      return (
        <RecordingScreen
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
      return (
        <ProcessingScreen
          group={group}
          wav={phase.wav}
          onParsed={(drafts) => (drafts.length === 0 ? problem('notUnderstood') : setPhase({ kind: 'confirm', drafts }))}
          onFailed={(kind, message) => problem(kind, phase.wav, message)}
          onCancel={onClose}
        />
      )
    case 'confirm':
      return <VoiceConfirm group={group} language={me.language} initialDrafts={phase.drafts} onSayAgain={record} onClose={onClose} onSaved={onSaved} />
    default:
      return (
        <VoiceProblem
          problem={phase.problem}
          message={phase.message}
          subscribeUrl={me.subscription.subscribeUrl}
          onRecord={record}
          onResend={phase.wav ? () => setPhase({ kind: 'processing', wav: phase.wav as Blob }) : undefined}
          onManual={onManual}
          onClose={onClose}
        />
      )
  }
}

function RecordingScreen({ onRecorded, onPaused, onSilence, onMicrophoneError, onCancel }: {
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

  const ending = MAX_RECORDING_SECONDS - seconds <= WARNING_SECONDS
  return (
    <>
      <ScreenHeader title={strings.recordingTitle} onBack={onCancel} />
      <section className="voice-stage">
        <p className="hint centered">{strings.recordingHint}</p>
        <p className={`amount voice-timer ${ending ? 'negative' : ''}`} role="timer">{`${formatSeconds(seconds)} / ${formatSeconds(MAX_RECORDING_SECONDS)}`}</p>
        <button type="button" className="voice-orb-button pressable" aria-label={strings.stopRecording} disabled={!ready} onClick={() => finish(onRecorded)}>
          <Moment
            kind="orb"
            input={{ mode: 'listening', level }}
            className="voice-orb"
            fallback={<span className="voice-stop"><span className="voice-stop-square" aria-hidden="true" /></span>}
            overlay={<span className="voice-stop-square voice-orb-stop" aria-hidden="true" />}
          />
        </button>
        <ul className="voice-examples">
          {strings.voiceExamples.map((example) => <li key={example} className="caption">{example}</li>)}
        </ul>
      </section>
      <Button variant="secondary" onClick={onCancel}>{strings.cancel}</Button>
    </>
  )
}

function ProcessingScreen({ group, wav, onParsed, onFailed, onCancel }: {
  group: Group
  wav: Blob
  onParsed: (drafts: Draft[]) => void
  onFailed: (problem: Problem, message: string | null) => void
  onCancel: () => void
}) {
  useEffect(() => {
    const controller = new AbortController()
    parseVoice(group.id, wav, controller.signal)
      .then((result) => onParsed(toDrafts(result.operations)))
      .catch((error: unknown) => {
        if (controller.signal.aborted) return
        notifyOutcome('error')
        onFailed(problemOf(error), serverMessage(error))
      })
    return () => controller.abort()
  }, [group.id, wav, onParsed, onFailed])

  return (
    <>
      <ScreenHeader title={strings.processingTitle} onBack={onCancel} />
      <section className="voice-stage" aria-busy="true">
        <Moment kind="orb" input={THINKING} className="voice-orb" fallback={<span className="voice-processing" aria-hidden="true"><MicIcon /></span>} />
        <p className="hint centered">{strings.processingHint}</p>
      </section>
      <Button variant="secondary" onClick={onCancel}>{strings.cancel}</Button>
    </>
  )
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

function formatSeconds(total: number): string {
  const minutes = Math.floor(total / SECONDS_IN_MINUTE)
  return `${minutes}:${String(total % SECONDS_IN_MINUTE).padStart(SECONDS_DIGITS, '0')}`
}
