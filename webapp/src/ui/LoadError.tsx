import { strings } from '../strings'
import { Button } from './Button'
import { StateView } from './StateView'

export function LoadError({ message, onRetry }: { message: string | null; onRetry: () => void }) {
  return (
    <StateView icon={strings.loadFailedIcon} title={strings.loadFailedTitle} body={message ?? strings.loadFailedFallback} tone="alert">
      <Button onClick={onRetry}>{strings.retry}</Button>
    </StateView>
  )
}
