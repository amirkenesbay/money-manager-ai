import { useState } from 'react'
import type { ColumnRole, Group, ImportPreview } from '../api/types'
import { strings } from '../strings'
import { BottomBar } from '../ui/BottomBar'
import { Button } from '../ui/Button'
import { ScreenHeader } from '../ui/ScreenHeader'
import { setImportColumns } from './importApi'

const ROLES: ColumnRole[] = ['DATE', 'AMOUNT', 'EXPENSE', 'INCOME', 'TYPE', 'CATEGORY', 'COMMENT', 'CURRENCY']
const NO_ROLE = ''
const SAMPLE_SEPARATOR = ' · '

export function ImportColumns({ group, preview, onBack, onReady, onFailed }: {
  group: Group
  preview: ImportPreview
  onBack: () => void
  onReady: (preview: ImportPreview) => void
  onFailed: (error: unknown) => void
}) {
  const [roles, setRoles] = useState<(ColumnRole | null)[]>(() => preview.columns.map((column) => column.role))
  const [saving, setSaving] = useState(false)
  const complete = roles.includes('DATE') && (roles.includes('AMOUNT') || roles.includes('EXPENSE') || roles.includes('INCOME'))

  const assign = (index: number, role: ColumnRole | null) =>
    setRoles((current) => current.map((existing, position) => (position === index ? role : existing === role && role !== null ? null : existing)))

  const save = async () => {
    setSaving(true)
    try {
      const mapping = Object.fromEntries(roles.flatMap((role, index) => (role ? [[role, index]] : [])))
      onReady(await setImportColumns(group.id, preview.token, mapping))
    } catch (error) {
      setSaving(false)
      onFailed(error)
    }
  }

  return (
    <>
      <ScreenHeader title={strings.importColumnsTitle} onBack={onBack} />
      <ul className="import-columns">
        {preview.columns.map((column, index) => (
          <li key={`${column.header}-${index}`} className="card import-column">
            <span className="row-title">{column.header}</span>
            <span className="caption import-samples">{column.samples.filter(Boolean).join(SAMPLE_SEPARATOR)}</span>
            <select
              className="text-field"
              aria-label={column.header}
              value={roles[index] ?? NO_ROLE}
              onChange={(event) => assign(index, (event.target.value || null) as ColumnRole | null)}
            >
              <option value={NO_ROLE}>{strings.importRoleNone}</option>
              {ROLES.map((role) => <option key={role} value={role}>{strings.importRoles[role]}</option>)}
            </select>
          </li>
        ))}
      </ul>
      {!complete && <p className="field-message warning" role="status">{strings.importNeedColumns}</p>}
      <BottomBar>
        <Button disabled={!complete || saving} onClick={save}>{saving ? strings.saving : strings.importNext}</Button>
      </BottomBar>
    </>
  )
}
