import { TAG_PATTERN } from './tags'

export function TaggedText({ text }: { text: string }) {
  return text.split(TAG_PATTERN).map((part, index) => (TAG_PATTERN.test(part) ? <span key={index} className="tag">{part}</span> : part))
}
