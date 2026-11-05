import { useState } from 'react'
import type { Group, Operation } from '../api/types'
import { formatDayLabel, formatSignedMoney } from '../format'
import { strings } from '../strings'
import { BottomBar } from '../ui/BottomBar'
import { Button } from '../ui/Button'
import { ScreenHeader } from '../ui/ScreenHeader'
import { DeleteSheet } from './DeleteSheet'
import { ShareDetail } from '../debts/ShareDetail'
import { shareOf } from '../debts/share'
import './operation.css'

export function OperationDetail({ group, userId, language, operation, onBack, onEdit, onDeleted }: {
  group: Group
  userId: number
  language: string
  operation: Operation
  onBack: () => void
  onEdit: () => void
  onDeleted: (operation: Operation) => void
}) {
  const [deleteOpen, setDeleteOpen] = useState(false)
  const income = operation.type === 'INCOME'
  const share = shareOf(operation)
  const fields = [
    [strings.dateLabel, formatDayLabel(operation.operationDate, new Date(), language)],
    [strings.commentLabel, operation.description ?? strings.noComment],
    [strings.detailGroup, group.name],
    [strings.detailAuthor, operation.creatorName],
  ]
  return (
    <>
      <ScreenHeader title={strings.operationTitle} onBack={onBack} />
      <section className="card detail-card">
        <span className="detail-icon" aria-hidden="true">{operation.categoryIcon}</span>
        <p className={`amount detail-amount ${income ? 'income' : ''}`}>{formatSignedMoney(operation.type, operation.amount, group.currency, language)}</p>
        <span className={`detail-badge ${income ? 'income' : 'negative'}`}>{`${strings.operationType(operation.type)} · ${operation.categoryName}`}</span>
      </section>
      <dl className="detail-fields">
        {fields.map(([label, value]) => (
          <div key={label} className="detail-field">
            <dt className="caption">{label}</dt>
            <dd>{value}</dd>
          </div>
        ))}
      </dl>
      {share && <ShareDetail groupId={group.id} userId={userId} amount={Number(operation.amount)} share={share} currency={group.currency} language={language} />}
      <BottomBar>
        <Button variant="secondary" className="button-danger" onClick={() => setDeleteOpen(true)}>{strings.delete}</Button>
        <Button onClick={onEdit}>{strings.edit}</Button>
      </BottomBar>
      {deleteOpen && <DeleteSheet group={group} language={language} operation={operation} onDeleted={onDeleted} onClose={() => setDeleteOpen(false)} />}
    </>
  )
}
