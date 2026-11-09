import { useState } from 'react'
import { serverMessage } from '../api/client'
import type { Group, SplitDefault } from '../api/types'
import { strings } from '../strings'
import { notifyOutcome } from '../telegram/haptics'
import { Segmented } from '../ui/Segmented'
import { Switch } from '../ui/Switch'
import type { Notify } from '../ui/useToast'
import { updateGroup } from './groupsApi'

const SPLIT_DEFAULTS: SplitDefault[] = ['MINE', 'EVERYONE']

export function SplitSwitch({ group, notify, onUpdated }: { group: Group; notify: Notify; onUpdated: (group: Group) => void }) {
  const [saving, setSaving] = useState(false)
  const save = async (changes: { splitExpenses?: boolean; splitDefault?: SplitDefault }) => {
    setSaving(true)
    try {
      onUpdated(await updateGroup(group.id, changes))
      notifyOutcome('success')
    } catch (error) {
      notifyOutcome('error')
      notify({ title: serverMessage(error) ?? strings.saveFailed })
    } finally {
      setSaving(false)
    }
  }
  return (
    <>
      <div className="switch-row">
        <span className="row-title">{strings.splitExpenses}</span>
        <Switch checked={group.splitExpenses} label={strings.splitExpenses} disabled={saving} onChange={(enabled) => void save({ splitExpenses: enabled })} />
      </div>
      {group.splitExpenses && (
        <Segmented
          label={strings.splitDefaultLabel}
          options={SPLIT_DEFAULTS}
          value={group.splitDefault}
          optionKey={(option) => option}
          optionLabel={(option) => (option === 'MINE' ? strings.forMyself : strings.splitEveryone)}
          onChange={(splitDefault) => void save({ splitDefault })}
        />
      )}
    </>
  )
}
