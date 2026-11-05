import { useEffect, useRef, useState } from 'react'
import { serverErrorCode, serverMessage } from '../api/client'
import type { Group, ImportPreview, ImportResult, Me } from '../api/types'
import { strings } from '../strings'
import { notifyOutcome } from '../telegram/haptics'
import { openExternalLink } from '../telegram/telegramEnv'
import { Button } from '../ui/Button'
import { ScreenHeader } from '../ui/ScreenHeader'
import { SkeletonRows } from '../ui/Skeleton'
import { StateView } from '../ui/StateView'
import type { Notify } from '../ui/useToast'
import { ImportColumns } from './ImportColumns'
import { ImportDone } from './ImportDone'
import { ImportPick } from './ImportPick'
import { ImportSummary } from './ImportSummary'
import { fetchImport, previewImport } from './importApi'
import './imports.css'

const SKELETON_ROWS = 3
const LIMIT_ICON = '📦'

type Step =
  | { kind: 'pick' }
  | { kind: 'reading' }
  | { kind: 'summary'; preview: ImportPreview }
  | { kind: 'columns'; preview: ImportPreview }
  | { kind: 'done'; result: ImportResult }
  | { kind: 'limit' }

const stepOf = (preview: ImportPreview): Step => {
  if (preview.importsLeft === 0) return { kind: 'limit' }
  return preview.needsColumns ? { kind: 'columns', preview } : { kind: 'summary', preview }
}

export function ImportScreen({ me, group, initialToken = null, notify, onBack, onOpenOperations }: {
  me: Me
  group: Group
  initialToken?: string | null
  notify: Notify
  onBack: () => void
  onOpenOperations: () => void
}) {
  const [step, setStep] = useState<Step>(initialToken ? { kind: 'reading' } : { kind: 'pick' })
  const tokenLoaded = useRef(false)

  const fail = (error: unknown) => {
    notifyOutcome('error')
    if (serverErrorCode(error) === 'IMPORT_LIMIT_REACHED') {
      setStep({ kind: 'limit' })
      return
    }
    notify({ title: serverMessage(error) ?? strings.saveFailed })
    setStep({ kind: 'pick' })
  }

  useEffect(() => {
    if (!initialToken || tokenLoaded.current) return
    tokenLoaded.current = true
    fetchImport(group.id, initialToken).then((preview) => setStep(stepOf(preview))).catch(fail)
  })

  const read = async (file: File) => {
    setStep({ kind: 'reading' })
    try {
      setStep(stepOf(await previewImport(group.id, file)))
    } catch (error) {
      fail(error)
    }
  }

  switch (step.kind) {
    case 'pick':
      return <ImportPick onBack={onBack} onFile={(file) => void read(file)} />
    case 'reading':
      return (
        <>
          <ScreenHeader title={strings.importTitle} onBack={onBack} />
          <p className="hint centered">{strings.importReading}</p>
          <SkeletonRows count={SKELETON_ROWS} />
        </>
      )
    case 'columns':
      return <ImportColumns group={group} preview={step.preview} onBack={() => setStep({ kind: 'pick' })} onReady={(preview) => setStep(stepOf(preview))} onFailed={fail} />
    case 'summary':
      return (
        <ImportSummary
          group={group}
          language={me.language}
          importLimit={me.subscription.limits.imports}
          preview={step.preview}
          onBack={() => setStep({ kind: 'pick' })}
          onColumns={() => setStep({ kind: 'columns', preview: step.preview })}
          onDone={(result) => {
            notifyOutcome('success')
            setStep({ kind: 'done', result })
          }}
          onFailed={fail}
        />
      )
    case 'done':
      return (
        <ImportDone
          group={group}
          language={me.language}
          result={step.result}
          onBack={onBack}
          onOpenOperations={onOpenOperations}
          onUndone={() => {
            notify({ title: strings.importUndone })
            setStep({ kind: 'pick' })
          }}
          onFailed={fail}
        />
      )
    default:
      return (
        <>
          <ScreenHeader title={strings.importTitle} onBack={onBack} />
          <StateView icon={LIMIT_ICON} title={strings.importLimitTitle(me.subscription.limits.imports ?? 0)} body={strings.importLimitBody}>
            <Button onClick={() => openExternalLink(me.subscription.subscribeUrl)}>{strings.learnPremium}</Button>
          </StateView>
        </>
      )
  }
}
