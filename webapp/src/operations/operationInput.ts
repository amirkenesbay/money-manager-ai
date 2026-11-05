import type { Operation } from '../api/types'
import type { OperationInput } from './operationsApi'

export const operationInputOf = (operation: Operation, operationDate: string = operation.operationDate): OperationInput => ({
  type: operation.type,
  amount: operation.amount,
  categoryId: operation.categoryId,
  operationDate,
  description: operation.description,
  payerId: operation.payerId,
  participantIds: operation.participantIds,
})
