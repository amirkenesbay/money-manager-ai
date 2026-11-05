import { useState } from 'react'
import type { Category, Group, ImportCategory, ImportPreview, ImportResult } from '../api/types'
import { useCategories } from '../categories/useCategories'
import { formatPeriod } from '../format'
import { strings } from '../strings'
import { BottomBar } from '../ui/BottomBar'
import { Button } from '../ui/Button'
import { ScreenHeader } from '../ui/ScreenHeader'
import { Sheet } from '../ui/Sheet'
import { commitImport } from './importApi'
import { type Choices, choiceLabel, defaultChoices, importedCount, recognisedLabel, skippedLabel } from './importChoices'

export function ImportSummary({ group, language, importLimit, preview, onBack, onColumns, onDone, onFailed }: {
  group: Group
  language: string
  importLimit: number | null
  preview: ImportPreview
  onBack: () => void
  onColumns: () => void
  onDone: (result: ImportResult) => void
  onFailed: (error: unknown) => void
}) {
  const [choices, setChoices] = useState<Choices>(() => defaultChoices(preview))
  const [editing, setEditing] = useState<ImportCategory | null>(null)
  const [saving, setSaving] = useState(false)
  const categories = useCategories(group.id)
  const known: Category[] = categories.state.status === 'ready' ? categories.state.data : []
  const count = importedCount(preview, choices)
  const skipped = skippedLabel(preview.skipped)

  const save = async () => {
    setSaving(true)
    try {
      onDone(await commitImport(group.id, preview.token, choices))
    } catch (error) {
      setSaving(false)
      onFailed(error)
    }
  }

  return (
    <>
      <ScreenHeader title={strings.importSummary} onBack={onBack} />
      <span className="import-recognised">
        <span className="caption">{recognisedLabel(preview)}</span>
        <button type="button" className="link-button" onClick={onColumns}>{strings.importColumnsLink}</button>
      </span>
      <section className="card import-total">
        <span className="amount">{preview.count}</span>
        {preview.from && preview.to && <span className="caption">{`${strings.importCount(preview.count)} · ${formatPeriod(preview.from, preview.to, language)}`}</span>}
        {skipped && <span className="caption">{skipped}</span>}
      </section>
      {preview.categories.length > 0 && (
        <>
          <h2 className="section-title">{strings.importCategories(preview.categories.length)}</h2>
          <ul className="import-categories">
            {preview.categories.map((category) => {
              const choice = choices[category.key]
              return (
                <li key={category.key}>
                  <button type="button" className="import-category pressable" onClick={() => setEditing(category)}>
                    <span className="row-title">{category.name}</span>
                    <span className="caption">{category.count}</span>
                    <span className={`import-tag ${choice?.action === 'SKIP' ? 'off' : choice?.action === 'CREATE' ? 'new' : ''}`}>{choiceLabel(category, choice, known)}</span>
                  </button>
                </li>
              )
            })}
          </ul>
        </>
      )}
      {preview.importsLeft !== null && importLimit !== null && <p className="caption centered">{strings.importLeft(preview.importsLeft, importLimit)}</p>}
      <BottomBar>
        <Button disabled={count === 0 || saving} onClick={save}>{saving ? strings.saving : count === 0 ? strings.importNothing : strings.importGo(count)}</Button>
      </BottomBar>
      {editing && (
        <Sheet label={editing.name} onClose={() => setEditing(null)}>
          <h2 className="sheet-title">{editing.name}</h2>
          <div className="split-options">
            {!editing.existingId && (
              <button type="button" role="radio" className="split-option" aria-checked={choices[editing.key]?.action === 'CREATE'} onClick={() => pick({ action: 'CREATE' })}>
                {strings.importCreate(editing.name)}
              </button>
            )}
            {known.filter((category) => category.type === editing.type).map((category) => {
              const current = choices[editing.key]
              return (
                <button key={category.id} type="button" role="radio" className="split-option" aria-checked={current?.action === 'EXISTING' && current.categoryId === category.id} onClick={() => pick({ action: 'EXISTING', categoryId: category.id })}>
                  {strings.importInto(`${category.icon} ${category.name}`)}
                </button>
              )
            })}
            <button type="button" role="radio" className="split-option" aria-checked={choices[editing.key]?.action === 'SKIP'} onClick={() => pick({ action: 'SKIP' })}>
              {strings.importDoNotMove}
            </button>
          </div>
        </Sheet>
      )}
    </>
  )

  function pick(choice: Choices[string]) {
    if (!editing) return
    setChoices((current) => ({ ...current, [editing.key]: choice }))
    setEditing(null)
  }
}
