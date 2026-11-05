import { useRef, useState } from 'react'
import { strings } from '../strings'
import { Button } from '../ui/Button'
import { ScreenHeader } from '../ui/ScreenHeader'
import { StateView } from '../ui/StateView'

const ACCEPTED_FILES = '.csv,.xlsx,.txt,text/csv'
const HERO_ICON = '📥'
const APPS = ['ZENMONEY', 'COINKEEPER', 'MONEY_MANAGER', 'OTHER'] as const

type App = (typeof APPS)[number]

export function ImportPick({ onBack, onFile }: { onBack: () => void; onFile: (file: File) => void }) {
  const input = useRef<HTMLInputElement>(null)
  const [app, setApp] = useState<App>('ZENMONEY')
  return (
    <>
      <ScreenHeader title={strings.importTitle} onBack={onBack} />
      <StateView icon={HERO_ICON} title={strings.importHero} body={strings.importFormats}>
        <Button onClick={() => input.current?.click()}>{strings.importPick}</Button>
        <p className="caption centered">{strings.importViaBot}</p>
      </StateView>
      <input
        ref={input}
        type="file"
        accept={ACCEPTED_FILES}
        hidden
        aria-label={strings.importPick}
        onChange={(event) => {
          const file = event.target.files?.[0]
          event.target.value = ''
          if (file) onFile(file)
        }}
      />
      <details className="import-help">
        <summary className="link-button">{strings.importHowTo}</summary>
        <div className="date-chips">
          {APPS.map((candidate) => (
            <button key={candidate} type="button" className="date-chip" aria-pressed={app === candidate} onClick={() => setApp(candidate)}>
              {strings.importApps[candidate]}
            </button>
          ))}
        </div>
        <ol className="import-steps">
          {strings.importSteps[app].map((line) => <li key={line}>{line}</li>)}
        </ol>
      </details>
    </>
  )
}
