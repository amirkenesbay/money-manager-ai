import type { Operation } from '../api/types'

export interface OperationDay {
  date: string
  operations: Operation[]
  net: number
}

export function groupByDay(operations: Operation[]): OperationDay[] {
  const days = new Map<string, Operation[]>()
  operations.forEach((operation) => days.set(operation.operationDate, [...(days.get(operation.operationDate) ?? []), operation]))
  return [...days].map(([date, dayOperations]) => ({ date, operations: dayOperations, net: dayNet(dayOperations) }))
}

function dayNet(operations: Operation[]): number {
  return operations.reduce((sum, operation) => sum + (operation.type === 'INCOME' ? 1 : -1) * Number(operation.amount), 0)
}
