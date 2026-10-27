import type { ReactNode } from 'react'
import { MainScreen } from './main/MainScreen'
import { useMe } from './me/useMe'
import { strings } from './strings'
import { LoadError } from './ui/LoadError'
import { SkeletonCard } from './ui/Skeleton'
import { StateView } from './ui/StateView'

export function App({ insideTelegram }: { insideTelegram: boolean }) {
  if (!insideTelegram) {
    return (
      <Screen>
        <StateView icon={strings.outsideTelegramIcon} title={strings.outsideTelegramTitle} body={strings.outsideTelegramHint} />
      </Screen>
    )
  }
  return <MeScreen />
}

function MeScreen() {
  const { state, reload } = useMe()

  if (state.status === 'loading') {
    return (
      <Screen>
        <div aria-busy="true" aria-label={strings.loading}><SkeletonCard /></div>
      </Screen>
    )
  }
  if (state.status === 'error') {
    return (
      <Screen>
        <LoadError message={state.message} onRetry={reload} />
      </Screen>
    )
  }
  const me = state.data
  if (me.activeGroupId === null) {
    return (
      <Screen>
        <StateView icon={strings.noGroupIcon} title={strings.noGroupTitle} body={strings.noGroupHint} />
      </Screen>
    )
  }
  return <MainScreen me={me} activeGroupId={me.activeGroupId} />
}

function Screen({ children }: { children: ReactNode }) {
  return <main className="screen">{children}</main>
}
