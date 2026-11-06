import type { E2EConfig } from 'e2e'
import { web } from '@e2e-dev/web'
import { claudeCode } from '@e2e-dev/acp'

const DEV_SERVER_URL = 'http://127.0.0.1:5174'
const PHONE_VIEWPORT = { width: 390, height: 844 }
const BACKEND_TIMEOUT_MS = 180_000

export default {
  tests: 'backend/**/*.e2e.ts',
  targets: [{
    name: 'backend',
    engine: web({ viewport: PHONE_VIEWPORT, reducedMotion: 'reduce' }),
    app: {
      url: DEV_SERVER_URL,
      command: { executable: 'npm', args: ['run', 'dev', '--', '--host', '127.0.0.1', '--port', '5174', '--strictPort'], cwd: '..', reuseExisting: false, log: '.e2e/logs/backend-app.log' },
    },
  }],
  agents: {
    default: {
      executor: claudeCode({ model: 'sonnet', system: 'You test a Telegram Mini App for personal finance. Its interface is in Russian. Move only by tapping and typing inside the app: never navigate to a URL, never reload the page, never use browser back.' }),
    },
  },
  workers: 1,
  timeout: BACKEND_TIMEOUT_MS,
  video: 'retain-on-failure',
} satisfies E2EConfig
