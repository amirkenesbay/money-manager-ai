import { useState } from 'react'
import type { DigestKind, Me } from '../api/types'
import { setDigestMuted } from '../me/meApi'
import { strings } from '../strings'
import { Button } from '../ui/Button'
import { Sheet } from '../ui/Sheet'
import { Switch } from '../ui/Switch'
import { DIGEST_KINDS } from './digestKinds'

export function DigestsSheet({ me, onChanged, onFailed, onClose }: {
  me: Me
  onChanged: (me: Me) => void
  onFailed: () => void
  onClose: () => void
}) {
  const [pending, setPending] = useState<DigestKind | null>(null)

  const toggle = async (kind: DigestKind, enabled: boolean) => {
    setPending(kind)
    try {
      onChanged(await setDigestMuted(kind, !enabled))
    } catch {
      onFailed()
    } finally {
      setPending(null)
    }
  }

  return (
    <Sheet label={strings.digestsTitle} onClose={onClose}>
      <h2 className="sheet-title">{strings.digestsTitle}</h2>
      <p className="hint">{strings.digestsHint}</p>
      <ul className="digest-list">
        {DIGEST_KINDS.map((kind) => (
          <li key={kind} className="digest-row">
            <span className="row-main">
              <span className="row-title">{strings.digestLabels[kind]}</span>
              <span className="caption">{strings.digestSchedules[kind]}</span>
            </span>
            <Switch
              checked={!me.mutedDigests.includes(kind)}
              label={strings.digestLabels[kind]}
              disabled={pending === kind}
              onChange={(enabled) => void toggle(kind, enabled)}
            />
          </li>
        ))}
      </ul>
      <Button onClick={onClose}>{strings.done}</Button>
    </Sheet>
  )
}
