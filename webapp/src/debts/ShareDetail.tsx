import type { Currency } from '../api/types'
import { useMembers } from '../groups/useMembers'
import { strings } from '../strings'
import { memberName, moneyOf, partsOf, type Share } from './share'
import './debts.css'

export function ShareDetail({ groupId, userId, amount, share, currency, language }: {
  groupId: string
  userId: number
  amount: number
  share: Share
  currency: Currency
  language: string
}) {
  const members = useMembers(groupId)
  if (members.state.status !== 'ready') return null
  const list = members.state.data
  const nameOf = (id: number) => memberName(list, userId, id)
  return (
    <dl className="detail-fields">
      <div className="detail-field">
        <dt className="caption">{strings.paidByLabel}</dt>
        <dd>{nameOf(share.payerId)}</dd>
      </div>
      <div className="detail-field">
        <dt className="caption">{strings.paidForLabel}</dt>
        <dd>
          <ul className="debt-items">
            {[...partsOf(amount, share)].map(([id, part]) => (
              <li key={id} className="debt-item"><span>{nameOf(id)}</span><span className="debt-item-amount">{moneyOf(part, currency, language)}</span></li>
            ))}
          </ul>
        </dd>
      </div>
    </dl>
  )
}
