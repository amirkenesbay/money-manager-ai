import type { ReactNode } from 'react'
import type { Me } from './api/types'
import { Dashboard } from './dashboard/Dashboard'
import { useMe } from './me/useMe'
import { strings } from './strings'
import { LoadError } from './ui/LoadError'

export function App({ insideTelegram }: { insideTelegram: boolean }) {
  if (!insideTelegram) {
    return (
      <Screen>
        <h1>{strings.outsideTelegramTitle}</h1>
        <p className="hint">{strings.outsideTelegramHint}</p>
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
        <p className="hint">{strings.loading}</p>
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
  if (me.activeGroupId === null) return <NoGroup me={me} />
  return <Dashboard initialGroupId={me.activeGroupId} language={me.language} />
}

function NoGroup({ me }: { me: Me }) {
  return (
    <Screen>
      <h1>{strings.greeting(me.firstName ?? me.username ?? strings.defaultName)}</h1>
      <p className="hint">{strings.noGroupHint}</p>
    </Screen>
  )
}

function Screen({ children }: { children: ReactNode }) {
  return <main className="screen">{children}</main>
}
