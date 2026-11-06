import { launchParams } from '../../dev/launchParams.ts'
import { authorizationHeader } from '../../src/api/auth.ts'

const FAKE_BOT_TOKEN = '123456:e2e'
const INIT_DATA_PARAM = 'tgWebAppData'

export type MockScenario = 'ok' | 'premium' | 'nogroup' | 'error' | 'flaky' | 'slow' | 'empty'

export const launchPath = (scenario: MockScenario = 'ok'): string => `/?mock=${scenario}#${launchParams(FAKE_BOT_TOKEN)}`

export const apiAuthorization = (): string =>
  authorizationHeader(new URLSearchParams(launchParams(FAKE_BOT_TOKEN)).get(INIT_DATA_PARAM) ?? '')
