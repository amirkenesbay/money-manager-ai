import type { Operation } from '../api/types'

export interface OperationActions {
  edit: (operation: Operation) => void
  remove: (operation: Operation) => void
  menu: (operation: Operation) => void
}
