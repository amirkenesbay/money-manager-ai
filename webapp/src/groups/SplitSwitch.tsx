import { useState } from 'react'
import { serverMessage } from '../api/client'
import type { Group } from '../api/types'
import { strings } from '../strings'
import { notifyOutcome } from '../telegram/haptics'
import { Switch } from '../ui/Switch'
import type { Notify } from '../ui/useToast'
import { updateGroup } from './groupsApi'

export function SplitSwitch({ group, notify, onUpdated }: { group: Group; notify: Notify; onUpdated: (group: Group) => void }) {
  const [saving, setSaving] = useState(false)
  const toggle = async (enabled: boolean) => {
    setSaving(true)
    try {
      onUpdated(await updateGroup(group.id, { splitExpenses: enabled }))
      notifyOutcome('success')
    } catch (error) {
      notifyOutcome('error')
      notify({ title: serverMessage(error) ?? strings.saveFailed })
    } finally {
      setSaving(false)
    }
  }
  return (
    <div className="switch-row">
      <span className="row-title">{strings.splitExpenses}</span>
      <Switch checked={group.splitExpenses} label={strings.splitExpenses} disabled={saving} onChange={(enabled) => void toggle(enabled)} />
    </div>
  )
}
