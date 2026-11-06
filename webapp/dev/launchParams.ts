import { createHmac } from 'node:crypto'

const SECRET_KEY_SEED = 'WebAppData'
const HMAC_ALGORITHM = 'sha256'
const PLACEHOLDER_SIGNATURE = 'dev'
const DEFAULT_USER_ID = '1000001'
const DEFAULT_FIRST_NAME = 'Dev'
const DEFAULT_LANGUAGE_CODE = 'ru'
const WEB_APP_VERSION = '8.0'
const WEB_APP_PLATFORM = 'tdesktop'

function sign(fields: Record<string, string>, botToken: string): string {
  const dataCheckString = Object.keys(fields).sort().map((key) => `${key}=${fields[key]}`).join('\n')
  const secretKey = createHmac(HMAC_ALGORITHM, SECRET_KEY_SEED).update(botToken).digest()
  return createHmac(HMAC_ALGORITHM, secretKey).update(dataCheckString).digest('hex')
}

function signedInitData(botToken: string): string {
  const user = {
    id: Number(process.env.DEV_USER_ID ?? DEFAULT_USER_ID),
    first_name: process.env.DEV_FIRST_NAME ?? DEFAULT_FIRST_NAME,
    language_code: DEFAULT_LANGUAGE_CODE,
  }
  const fields = {
    auth_date: String(Math.floor(Date.now() / 1000)),
    signature: PLACEHOLDER_SIGNATURE,
    user: JSON.stringify(user),
  }
  return new URLSearchParams({ ...fields, hash: sign(fields, botToken) }).toString()
}

export function launchParams(botToken: string): string {
  return new URLSearchParams({
    tgWebAppData: signedInitData(botToken),
    tgWebAppVersion: WEB_APP_VERSION,
    tgWebAppPlatform: WEB_APP_PLATFORM,
  }).toString()
}
