export type HintKind = 'say' | 'ask'

export interface HintItem {
  kind: HintKind
  text: string
}

const MAX_QUESTIONS = 3

export function hintItems(sayExamples: string[], questions: string[]): HintItem[] {
  const say = sayExamples.map((text): HintItem => ({ kind: 'say', text }))
  const asks = questions.slice(0, MAX_QUESTIONS).map((text): HintItem => ({ kind: 'ask', text }))
  return asks.length === 0 ? say : [...say.slice(0, 1), ...asks]
}
