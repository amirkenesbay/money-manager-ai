const UTC_LABEL = 'UTC'
const GMT_LABEL = 'GMT'
const PATH_SEPARATOR = '/'
const WORD_SEPARATOR = /_/g

export const deviceTimezone = (): string => Intl.DateTimeFormat().resolvedOptions().timeZone

function utcOffset(timezone: string): string {
  const offset = new Intl.DateTimeFormat('en', { timeZone: timezone, timeZoneName: 'shortOffset' })
    .formatToParts(new Date())
    .find((part) => part.type === 'timeZoneName')?.value ?? GMT_LABEL
  return offset.replace(GMT_LABEL, UTC_LABEL)
}

export function describeTimezone(timezone: string): string {
  const city = timezone.split(PATH_SEPARATOR).pop()?.replace(WORD_SEPARATOR, ' ') ?? timezone
  return `${city} (${utcOffset(timezone)})`
}
