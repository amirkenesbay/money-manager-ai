import { useState } from 'react'
import type { Group, GroupNoticeLevel } from '../api/types'
import { strings } from '../strings'
import { Button } from '../ui/Button'
import { Segmented } from '../ui/Segmented'
import { Sheet } from '../ui/Sheet'
import { useSubmit } from '../ui/useSubmit'
import { setGroupNotices } from './groupsApi'

const NOTICE_LEVELS: GroupNoticeLevel[] = ['ALL', 'IMPORTANT', 'OFF']

export function NoticesSheet({ group, onSaved, onClose }: { group: Group; onSaved: (group: Group) => void; onClose: () => void }) {
  const [level, setLevel] = useState(group.noticeLevel)
  const submit = useSubmit()

  const done = async () => {
    if (level === group.noticeLevel) {
      onClose()
      return
    }
    const result = await submit.run(() => setGroupNotices(group.id, level), strings.saveFailed)
    if (result.ok) onSaved(result.value)
  }

  return (
    <Sheet label={strings.groupNoticesTitle} onClose={onClose}>
      <h2 className="sheet-title">{strings.groupNoticesTitle}</h2>
      <Segmented
        variant="choices"
        label={strings.groupNoticesTitle}
        options={NOTICE_LEVELS}
        value={level}
        optionKey={(option) => option}
        optionLabel={(option) => strings.groupNoticeLevels[option]}
        onChange={setLevel}
      />
      <p className="hint">{strings.groupNoticeDetails[level]}</p>
      <p className="caption">{strings.groupNoticesHint}</p>
      {submit.error && <p className="error-text" role="alert">{submit.error.message}</p>}
      <Button disabled={submit.pending} onClick={done}>{strings.done}</Button>
    </Sheet>
  )
}
