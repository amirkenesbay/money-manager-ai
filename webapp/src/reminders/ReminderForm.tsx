import { useState } from 'react'
import type { Reminder } from '../api/types'
import { strings } from '../strings'
import { BottomBar } from '../ui/BottomBar'
import { Button } from '../ui/Button'
import { ConfirmSheet } from '../ui/ConfirmSheet'
import { EmojiPicker } from '../ui/EmojiPicker'
import { NameField } from '../ui/NameField'
import { ScreenHeader } from '../ui/ScreenHeader'
import { useConfirmClosing } from '../ui/useConfirmClosing'
import { useNameInput } from '../ui/useNameInput'
import { useSubmit } from '../ui/useSubmit'
import { createReminder, deleteReminder, replaceReminder } from './remindersApi'
import { draftOf, intervalValid, toReminderInput } from './schedule'
import { ScheduleFields } from './ScheduleFields'

const MAX_REMINDER_NAME_LENGTH = 100
const ICON_PRESETS = ['🔔', '💸', '📝', '💳', '📊', '🧾', '🏠', '📱', '💡', '🎯']

export function ReminderForm({ reminder, language, onBack, onSaved, onDeleted, onLimit }: {
  reminder: Reminder | null
  language: string
  onBack: () => void
  onSaved: (reminder: Reminder, created: boolean) => void
  onDeleted: (reminder: Reminder) => void
  onLimit: () => void
}) {
  const name = useNameInput(reminder?.name ?? '')
  const [icon, setIcon] = useState(reminder?.icon ?? ICON_PRESETS[0])
  const [draft, setDraft] = useState(() => draftOf(reminder))
  const [deleteOpen, setDeleteOpen] = useState(false)
  const submit = useSubmit()
  const intervalInvalid = !intervalValid(draft)

  useConfirmClosing(name.value.trim() !== '' && name.value !== reminder?.name)

  const save = async () => {
    const trimmed = name.require()
    if (trimmed === null || intervalInvalid) return
    const input = toReminderInput(trimmed, icon, draft)
    const result = await submit.run(() => (reminder ? replaceReminder(reminder.id, input) : createReminder(input)), strings.saveFailed)
    if (result.ok) onSaved(result.value, reminder === null)
    else if (result.error.code === 'NOTIFICATION_LIMIT_REACHED') onLimit()
  }

  return (
    <>
      <ScreenHeader title={reminder ? strings.editReminderTitle : strings.newReminderTitle} onBack={onBack} />
      <NameField
        label={strings.nameLabel}
        value={name.value}
        placeholder={strings.reminderNamePlaceholder}
        maxLength={MAX_REMINDER_NAME_LENGTH}
        error={name.missingError}
        onChange={name.change}
      />
      <EmojiPicker value={icon} presets={ICON_PRESETS} onChange={setIcon} />
      <ScheduleFields draft={draft} language={language} intervalInvalid={intervalInvalid} onChange={setDraft} />
      {reminder && <button type="button" className="link-button danger-link" onClick={() => setDeleteOpen(true)}>{strings.deleteReminder}</button>}
      {submit.error && <p className="error-text" role="alert">{submit.error.message}</p>}
      <BottomBar>
        <Button disabled={submit.pending || intervalInvalid} onClick={save}>{submit.pending ? strings.saving : reminder ? strings.saveChanges : strings.save}</Button>
      </BottomBar>
      {deleteOpen && reminder && (
        <ConfirmSheet
          title={strings.deleteReminderQuestion(reminder.name)}
          confirmLabel={strings.delete}
          onConfirm={() => deleteReminder(reminder.id)}
          onDone={() => onDeleted(reminder)}
          onClose={() => setDeleteOpen(false)}
        >
          <p className="hint">{strings.deleteReminderWarning}</p>
        </ConfirmSheet>
      )}
    </>
  )
}
