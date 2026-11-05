import { apiRequest } from '../api/client'
import type { Loan, LoanDirection } from '../api/types'

const loansPath = (groupId: string) => `/groups/${groupId}/loans`

export interface LoanInput {
  direction: LoanDirection
  person: string
  amount: string
  dueDate: string | null
  note: string | null
}

export const fetchLoanPeople = (groupId: string, signal?: AbortSignal): Promise<string[]> =>
  apiRequest<string[]>(`${loansPath(groupId)}/people`, {}, signal)

export const createLoan = (groupId: string, input: LoanInput): Promise<Loan> =>
  apiRequest<Loan>(loansPath(groupId), { method: 'POST', body: JSON.stringify(input) })

export const repayLoan = (groupId: string, loanId: string, amount: string | null): Promise<Loan> =>
  apiRequest<Loan>(`${loansPath(groupId)}/${loanId}/repayments`, { method: 'POST', body: JSON.stringify({ amount }) })

export const undoRepayment = (groupId: string, loanId: string, repaymentId: string): Promise<Loan> =>
  apiRequest<Loan>(`${loansPath(groupId)}/${loanId}/repayments/${repaymentId}`, { method: 'DELETE' })

export const deleteLoan = (groupId: string, loanId: string): Promise<void> =>
  apiRequest<void>(`${loansPath(groupId)}/${loanId}`, { method: 'DELETE' })
