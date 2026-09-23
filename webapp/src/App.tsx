import type { ReactNode } from 'react'
import type { Me } from './api/types'
import { useMe } from './me/useMe'
import { strings } from './strings'

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
        <h1>{strings.loadFailedTitle}</h1>
        <p className="hint">{state.message ?? strings.loadFailedFallback}</p>
        <button type="button" onClick={reload}>{strings.retry}</button>
      </Screen>
    )
  }
  return <Welcome me={state.me} />
}

function Welcome({ me }: { me: Me }) {
  return (
    <Screen>
      <h1>{strings.greeting(me.firstName ?? me.username ?? strings.defaultName)}</h1>
      {me.activeGroupId === null
        ? <p className="hint">{strings.noGroupHint}</p>
        : <p className="hint">{strings.tier(me.subscription.tier)}</p>}
    </Screen>
  )
}

function Screen({ children }: { children: ReactNode }) {
  return <main className="screen">{children}</main>
}
