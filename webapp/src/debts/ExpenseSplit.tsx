import type { Currency, SplitDefault } from '../api/types'
import { useMembers } from '../groups/useMembers'
import { everyoneShare, onlyMeShare, type Share } from './share'
import { SplitField } from './SplitField'

export function ExpenseSplit({ groupId, splitDefault, userId, amount, currency, language, share, onChange }: {
  groupId: string
  splitDefault: SplitDefault
  userId: number
  amount: number | null
  currency: Currency
  language: string
  share: Share | null
  onChange: (share: Share) => void
}) {
  const members = useMembers(groupId)
  if (members.state.status !== 'ready' || members.state.data.length < 2) return null
  const list = members.state.data
  return (
    <SplitField
      members={list}
      userId={userId}
      amount={amount}
      currency={currency}
      language={language}
      share={share ?? (splitDefault === 'EVERYONE' ? everyoneShare(list, userId) : onlyMeShare(userId))}
      onChange={onChange}
    />
  )
}
