import type { Currency, Operation } from '../api/types'
import { impactOf, impactText, shareOf } from './share'
import './debts.css'

export function ImpactNote({ operation, userId, currency, language }: { operation: Operation; userId: number; currency: Currency; language: string }) {
  const share = shareOf(operation)
  if (!share) return null
  const impact = impactOf(Number(operation.amount), share, userId)
  return <span className={`row-impact ${impact.kind}`}>{impactText(impact, currency, language)}</span>
}
