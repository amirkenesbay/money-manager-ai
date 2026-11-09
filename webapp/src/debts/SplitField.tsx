import { useState } from 'react'
import type { Currency, Member } from '../api/types'
import { strings } from '../strings'
import { Button } from '../ui/Button'
import { Sheet } from '../ui/Sheet'
import { impactOf, impactText, memberName, type Share } from './share'
import './debts.css'

const PAIR = 2

export function SplitField({ members, userId, amount, currency, language, share, onChange }: {
  members: Member[]
  userId: number
  amount: number | null
  currency: Currency
  language: string
  share: Share
  onChange: (share: Share) => void
}) {
  const [open, setOpen] = useState(false)
  const label = shareLabel(members, userId, share)
  return (
    <>
      <button type="button" className="chip split-chip pressable" aria-label={`${strings.paidForLabel}: ${label}`} onClick={() => setOpen(true)}>
        <MemberFaces members={members} userId={userId} ids={share.participantIds} />
        <span>{label}</span>
      </button>
      {open && (
        <SplitSheet members={members} userId={userId} amount={amount} currency={currency} language={language} share={share} onChange={onChange} onClose={() => setOpen(false)} />
      )}
    </>
  )
}

function SplitSheet({ members, userId, amount, currency, language, share, onChange, onClose }: {
  members: Member[]
  userId: number
  amount: number | null
  currency: Currency
  language: string
  share: Share
  onChange: (share: Share) => void
  onClose: () => void
}) {
  const [draft, setDraft] = useState(share)
  const nameOf = (id: number) => memberName(members, userId, id)
  const toggle = (id: number) => {
    const people = draft.participantIds.includes(id) ? draft.participantIds.filter((other) => other !== id) : [...draft.participantIds, id]
    setDraft({ ...draft, participantIds: people })
  }
  const nextPayer = () => {
    const index = members.findIndex((member) => member.userId === draft.payerId)
    setDraft({ ...draft, payerId: members[(index + 1) % members.length].userId })
  }
  const empty = draft.participantIds.length === 0
  return (
    <Sheet label={strings.paidForLabel} onClose={onClose}>
      <h2 className="sheet-title">{strings.paidForLabel}</h2>
      <div className="split-people">
        {members.map((member) => (
          <button
            key={member.userId}
            type="button"
            className="split-person"
            aria-pressed={draft.participantIds.includes(member.userId)}
            onClick={() => toggle(member.userId)}
          >
            <MemberFace name={nameOf(member.userId)} />
            <span>{nameOf(member.userId)}</span>
          </button>
        ))}
      </div>
      <p className="split-sentence">
        {strings.paidByLabel}{' '}
        <button type="button" className="split-word" aria-label={`${strings.paidByLabel}: ${nameOf(draft.payerId)}`} onClick={nextPayer}>{nameOf(draft.payerId)}</button>
      </p>
      {empty
        ? <p className="field-message negative" role="alert">{strings.pickSomeone}</p>
        : amount !== null && <p className="caption split-outcome" role="status">{outcomeOf(amount, draft, userId, currency, language)}</p>}
      <Button disabled={empty} onClick={() => { onChange(draft); onClose() }}>{strings.done}</Button>
    </Sheet>
  )
}

function MemberFaces({ members, userId, ids }: { members: Member[]; userId: number; ids: number[] }) {
  return (
    <span className="member-faces" aria-hidden="true">
      {ids.map((id) => <MemberFace key={id} name={memberName(members, userId, id)} />)}
    </span>
  )
}

function MemberFace({ name }: { name: string }) {
  return <span className="member-face" aria-hidden="true">{name.slice(0, 1).toUpperCase()}</span>
}

function shareLabel(members: Member[], userId: number, share: Share): string {
  const people = share.participantIds
  if (people.length === members.length) return members.length === PAIR ? strings.splitHalf : strings.splitEveryone
  if (people.length === 1 && people[0] === userId) return strings.forMyself
  if (people.length === 1) return strings.splitFor(memberName(members, userId, people[0]))
  return people.map((id) => memberName(members, userId, id)).join(', ')
}

function outcomeOf(amount: number, share: Share, userId: number, currency: Currency, language: string): string {
  const impact = impactOf(amount, share, userId)
  const onlyPayer = share.participantIds.length === 1 && share.participantIds[0] === share.payerId
  return impact.kind === 'none' && onlyPayer ? strings.shareSelf : impactText(impact, currency, language)
}
