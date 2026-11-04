export const TAG_PATTERN = /(#[\p{L}\p{N}_]+)/u

const TAG_MARK = '#'
const TAGS = /#([\p{L}\p{N}_]+)/gu
const NOT_TAG_CHARACTERS = /[^\p{L}\p{N}_]+/gu
const SPACES = /\s+/g
const SPACE = ' '
const JOINER = '_'

export const MAX_TAG_LENGTH = 30

export function tagsOf(description: string | null): string[] {
  if (!description) return []
  return [...new Set([...description.matchAll(TAGS)].map((match) => match[1].toLowerCase()))]
}

export function withoutTags(description: string | null): string {
  return (description ?? '').replace(TAGS, '').replace(SPACES, SPACE).trim()
}

export function normalizeTag(input: string): string {
  return input.trim().replace(SPACES, JOINER).replace(NOT_TAG_CHARACTERS, '').toLowerCase().slice(0, MAX_TAG_LENGTH)
}

export function withTags(text: string, tags: string[]): string | null {
  const joined = [text.trim(), ...tags.map((tag) => TAG_MARK + tag)].filter(Boolean).join(SPACE)
  return joined || null
}
