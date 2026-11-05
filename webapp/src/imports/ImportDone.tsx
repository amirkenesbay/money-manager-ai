import { useState } from 'react'
import type { Group, ImportResult } from '../api/types'
import { formatPeriod } from '../format'
import { strings } from '../strings'
import { BottomBar } from '../ui/BottomBar'
import { Button } from '../ui/Button'
import { ScreenHeader } from '../ui/ScreenHeader'
import { StateView } from '../ui/StateView'
import { undoImport } from './importApi'

const DONE_ICON = '🎉'
const PART_SEPARATOR = ' · '

export function ImportDone({ group, language, result, onBack, onOpenOperations, onUndone, onFailed }: {
  group: Group
  language: string
  result: ImportResult
  onBack: () => void
  onOpenOperations: () => void
  onUndone: () => void
  onFailed: (error: unknown) => void
}) {
  const [undoing, setUndoing] = useState(false)
  const details = [
    result.from && result.to ? formatPeriod(result.from, result.to, language) : null,
    result.createdCategories > 0 ? strings.importCreated(result.createdCategories) : null,
  ].filter(Boolean).join(PART_SEPARATOR)

  const undo = async () => {
    setUndoing(true)
    try {
      await undoImport(group.id, result.importId)
      onUndone()
    } catch (error) {
      setUndoing(false)
      onFailed(error)
    }
  }

  return (
    <>
      <ScreenHeader title={strings.importTitle} onBack={onBack} />
      <StateView icon={DONE_ICON} title={strings.importDone(result.count)} body={details || undefined}>
        {result.historyLimited && <p className="hint">{strings.importHistoryNote}</p>}
      </StateView>
      <BottomBar>
        <Button variant="secondary" disabled={undoing} onClick={undo}>{strings.importUndo}</Button>
        <Button onClick={onOpenOperations}>{strings.importOpenOperations}</Button>
      </BottomBar>
    </>
  )
}
