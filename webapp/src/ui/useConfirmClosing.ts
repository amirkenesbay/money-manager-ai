import { useEffect } from 'react'
import { confirmClosing } from '../telegram/haptics'

export function useConfirmClosing(dirty: boolean): void {
  useEffect(() => {
    confirmClosing(dirty)
    return () => confirmClosing(false)
  }, [dirty])
}
