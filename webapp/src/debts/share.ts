import type { Currency, Member, Operation } from '../api/types'
import { formatMoney } from '../format'
import { strings } from '../strings'

const CENTS = 100

export interface Share {
  payerId: number
  participantIds: number[]
}

export type Impact = { kind: 'owed'; amount: number } | { kind: 'mine'; amount: number } | { kind: 'none' }

export function partsOf(amount: number, share: Share): Map<number, number> {
  const people = [...share.participantIds].sort((left, right) => left - right)
  const cents = Math.round(amount * CENTS)
  const base = Math.floor(cents / people.length)
  const restTaker = people.includes(share.payerId) ? share.payerId : people[0]
  return new Map(people.map((id) => [id, (id === restTaker ? base + cents - base * people.length : base) / CENTS]))
}

export function shareOf(operation: Pick<Operation, 'payerId' | 'participantIds'>): Share | null {
  const { payerId, participantIds } = operation
  return payerId != null && participantIds && participantIds.length > 0 ? { payerId, participantIds } : null
}

export function impactOf(amount: number, share: Share, userId: number): Impact {
  const parts = partsOf(amount, share)
  if (share.payerId === userId) {
    const owed = [...parts].filter(([id]) => id !== userId).reduce((total, [, part]) => total + part, 0)
    return owed > 0 ? { kind: 'owed', amount: owed } : { kind: 'none' }
  }
  const mine = parts.get(userId)
  return mine === undefined ? { kind: 'none' } : { kind: 'mine', amount: mine }
}

export const onlyMeShare = (userId: number): Share => ({ payerId: userId, participantIds: [userId] })

export const everyoneShare = (members: Member[], payerId: number): Share => ({ payerId, participantIds: members.map((member) => member.userId) })

export const moneyOf = (value: number, currency: Currency, language: string): string => formatMoney(String(value), currency, language)

export const memberName = (members: Member[], userId: number, id: number): string =>
  id === userId ? strings.memberMe : members.find((member) => member.userId === id)?.name ?? ''

export function impactText(impact: Impact, currency: Currency, language: string): string {
  if (impact.kind === 'owed') return strings.shareOwed(moneyOf(impact.amount, currency, language))
  if (impact.kind === 'mine') return strings.shareMine(moneyOf(impact.amount, currency, language))
  return strings.shareNone
}
