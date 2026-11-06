import { launchParams } from './launchParams.ts'

const DEFAULT_APP_URL = 'http://localhost:5173/'

function requireEnv(name: string): string {
  const value = process.env[name]
  if (!value) {
    console.error(`${name} is not set. Use the token of your TEST bot, never the production one.`)
    process.exit(1)
  }
  return value
}

console.log(`${process.env.DEV_APP_URL ?? DEFAULT_APP_URL}#${launchParams(requireEnv('BOT_TOKEN'))}`)
