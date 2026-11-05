import { useState } from 'react'
import type { Currency, Member } from '../api/types'
import { strings } from '../strings'
import { Button } from '../ui/Button'
import { ChevronDownIcon } from '../ui/icons'
import { Sheet } from '../ui/Sheet'
import { impactOf, impactText, memberName, moneyOf, partsOf, type Share } from './share'
import './debts.css'

type Picker = 'payer' | 'people' | null

export function SplitField({ members, userId, amount, currency, language, share, onChange }: {
  members: Member[]
  userId: number
  amount: number | null
  currency: Currency
  language: string
  share: Share
  onChange: (share: Share) => void
}) {
  const [picker, setPicker] = useState<Picker>(null)
  const nameOf = (id: number) => memberName(members, userId, id)
  const everyone = share.participantIds.length === members.length
  const onlyMe = share.participantIds.length === 1 && share.participantIds[0] === userId
  const peopleLabel = everyone ? strings.forEveryone : onlyMe ? strings.forMyself : share.participantIds.map(nameOf).join(', ')

  return (
    <>
      <div className="split-field">
        <button type="button" className="split-pick pressable" onClick={() => setPicker('payer')}>
          <span className="caption">{strings.paidByLabel}</span>
          <span className="split-value">{nameOf(share.payerId)}<ChevronDownIcon /></span>
        </button>
        <button type="button" className="split-pick pressable" onClick={() => setPicker('people')}>
          <span className="caption">{strings.paidForLabel}</span>
          <span className="split-value">{peopleLabel}<ChevronDownIcon /></span>
        </button>
      </div>
      {amount !== null && <p className="caption split-outcome" role="status">{outcomeOf(amount, share, userId, currency, language)}</p>}
      {picker === 'payer' && (
        <Sheet label={strings.paidByLabel} onClose={() => setPicker(null)}>
          <h2 className="sheet-title">{strings.paidByLabel}</h2>
          <div className="split-options" role="radiogroup" aria-label={strings.paidByLabel}>
            {members.map((member) => (
              <button
                key={member.userId}
                type="button"
                role="radio"
                className="split-option"
                aria-checked={member.userId === share.payerId}
                onClick={() => {
                  onChange({ ...share, payerId: member.userId })
                  setPicker(null)
                }}
              >
                {nameOf(member.userId)}
              </button>
            ))}
          </div>
        </Sheet>
      )}
      {picker === 'people' && (
        <PeopleSheet members={members} userId={userId} amount={amount} currency={currency} language={language} share={share} nameOf={nameOf} onChange={onChange} onClose={() => setPicker(null)} />
      )}
    </>
  )
}

function PeopleSheet({ members, userId, amount, currency, language, share, nameOf, onChange, onClose }: {
  members: Member[]
  userId: number
  amount: number | null
  currency: Currency
  language: string
  share: Share
  nameOf: (id: number) => string
  onChange: (share: Share) => void
  onClose: () => void
}) {
  const [people, setPeople] = useState(share.participantIds)
  const parts = amount !== null && people.length > 0 ? partsOf(amount, { ...share, participantIds: people }) : null
  const toggle = (id: number) => setPeople((list) => (list.includes(id) ? list.filter((other) => other !== id) : [...list, id]))
  return (
    <Sheet label={strings.paidForLabel} onClose={onClose}>
      <h2 className="sheet-title">{strings.paidForLabel}</h2>
      <div className="split-quick">
        <button type="button" className="chip pressable" onClick={() => setPeople(members.map((member) => member.userId))}>{strings.forEveryone}</button>
        <button type="button" className="chip pressable" onClick={() => setPeople([userId])}>{strings.forMyself}</button>
      </div>
      <div className="split-options">
        {members.map((member) => {
          const part = parts?.get(member.userId)
          return (
            <button key={member.userId} type="button" role="checkbox" className="split-option" aria-checked={people.includes(member.userId)} onClick={() => toggle(member.userId)}>
              <span>{nameOf(member.userId)}</span>
              {part !== undefined && <span className="caption">{moneyOf(part, currency, language)}</span>}
            </button>
          )
        })}
      </div>
      {people.length === 0 && <p className="field-message negative" role="alert">{strings.pickSomeone}</p>}
      <Button disabled={people.length === 0} onClick={() => { onChange({ ...share, participantIds: people }); onClose() }}>{strings.done}</Button>
    </Sheet>
  )
}

function outcomeOf(amount: number, share: Share, userId: number, currency: Currency, language: string): string {
  const impact = impactOf(amount, share, userId)
  const onlyPayer = share.participantIds.length === 1 && share.participantIds[0] === share.payerId
  return impact.kind === 'none' && onlyPayer ? strings.shareSelf : impactText(impact, currency, language)
}
