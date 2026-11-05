import type { Currency } from '../api/types'
import { useMembers } from '../groups/useMembers'
import { everyoneShare, type Share } from './share'
import { SplitField } from './SplitField'

export function ExpenseSplit({ groupId, userId, amount, currency, language, share, onChange }: {
  groupId: string
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
      share={share ?? everyoneShare(list, userId)}
      onChange={onChange}
    />
  )
}
