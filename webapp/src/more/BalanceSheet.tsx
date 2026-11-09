import { useCallback, useState } from 'react'
import type { Balance, Group } from '../api/types'
import { useAsync } from '../api/useAsync'
import { fetchBalance } from '../dashboard/dashboardApi'
import { setInitialBalance } from '../groups/groupsApi'
import { parseBalance } from '../operation/amount'
import { strings } from '../strings'
import { AmountInput } from '../ui/AmountInput'
import { Button } from '../ui/Button'
import { LoadError } from '../ui/LoadError'
import { Sheet } from '../ui/Sheet'
import { SkeletonRows } from '../ui/Skeleton'
import { useSubmit } from '../ui/useSubmit'
import { initialForCurrent } from './balanceMath'

const SKELETON_ROWS = 1

export function BalanceSheet({ group, onSaved, onClose }: { group: Group; onSaved: (group: Group) => void; onClose: () => void }) {
  const load = useCallback((signal: AbortSignal) => fetchBalance(group.id, signal), [group.id])
  const balance = useAsync(load)
  return (
    <Sheet label={strings.balanceNowTitle} onClose={onClose}>
      <h2 className="sheet-title">{strings.balanceNowTitle}</h2>
      {balance.state.status === 'loading' && <SkeletonRows count={SKELETON_ROWS} />}
      {balance.state.status === 'error' && <LoadError message={balance.state.message} onRetry={balance.reload} />}
      {balance.state.status === 'ready' && <BalanceForm group={group} balance={balance.state.data} onSaved={onSaved} />}
    </Sheet>
  )
}

function BalanceForm({ group, balance, onSaved }: { group: Group; balance: Balance; onSaved: (group: Group) => void }) {
  const [text, setText] = useState(balance.total)
  const submit = useSubmit()
  const current = parseBalance(text)
  const invalid = text.trim() !== '' && current === null

  const save = async () => {
    if (current === null) return
    const result = await submit.run(() => setInitialBalance(group.id, initialForCurrent(balance, current)), strings.saveFailed)
    if (result.ok) onSaved(result.value)
  }

  return (
    <>
      <AmountInput label={strings.balanceNowLabel} value={text} currency={group.currency} invalid={invalid} onChange={setText} />
      {submit.error && <p className="error-text" role="alert">{submit.error.message}</p>}
      <Button disabled={current === null || submit.pending} onClick={save}>{submit.pending ? strings.saving : strings.save}</Button>
    </>
  )
}
