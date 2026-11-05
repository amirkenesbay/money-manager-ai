const WORD_DELAY_MS = 120
const WORD_SEPARATOR = ' '

export function TypedText({ text, className }: { text: string; className?: string }) {
  return (
    <span className={className}>
      <span className="visually-hidden">{text}</span>
      {text.split(WORD_SEPARATOR).map((word, index) => (
        <span key={`${index}-${word}`} className="typed-word" style={{ animationDelay: `${index * WORD_DELAY_MS}ms` }} aria-hidden="true">{word}</span>
      ))}
    </span>
  )
}
