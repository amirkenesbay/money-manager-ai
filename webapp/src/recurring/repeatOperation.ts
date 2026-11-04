import { serverMessage } from '../api/client'
import type { Operation, RecurringPayment, Repeat } from '../api/types'
import { strings } from '../strings'
import { describeRepeat } from './recurrence'
import { createRecurring } from './recurringApi'

export const repeatOperation = (groupId: string, operation: Operation, repeat: Repeat): Promise<RecurringPayment> =>
  createRecurring(groupId, {
    type: operation.type,
    amount: operation.amount,
    categoryId: operation.categoryId,
    description: operation.description,
    unit: repeat.unit,
    interval: repeat.interval,
    startDate: operation.operationDate,
    endDate: null,
    mode: 'AUTO',
    firstRecorded: true,
  })

export async function startRepeating(groupId: string, operation: Operation, repeat: Repeat): Promise<string> {
  try {
    await repeatOperation(groupId, operation, repeat)
    return strings.repeatSavedToast(describeRepeat(repeat))
  } catch (error) {
    return serverMessage(error) ?? strings.saveFailed
  }
}
