import { strings } from '../strings'
import { Button } from '../ui/Button'
import { MicIcon } from '../ui/icons'
import { Sheet } from '../ui/Sheet'
import { SkeletonRows } from '../ui/Skeleton'
import { useAskSuggestions } from './useAskSuggestions'
import './ask.css'

const SKELETON_ROWS = 3

export function AskTopicsSheet({ groupId, onAsk, onRecord, onClose }: {
  groupId: string
  onAsk: (question: string) => void
  onRecord: () => void
  onClose: () => void
}) {
  const suggestions = useAskSuggestions(groupId)
  return (
    <Sheet label={strings.askTopicsTitle} onClose={onClose}>
      <h2 className="sheet-title">{strings.askTopicsTitle}</h2>
      <p className="hint">{strings.askTopicsBody}</p>
      {suggestions === null && <SkeletonRows count={SKELETON_ROWS} />}
      {suggestions?.topics.map((topic) => (
        <section key={topic.topic} className="ask-topic" aria-label={topic.title}>
          <h3 className="section-title">{topic.title}</h3>
          <div className="ask-topic-questions">
            {topic.questions.map((question) => (
              <button key={question} type="button" className="chip ask-chip pressable" onClick={() => onAsk(question)}>{question}</button>
            ))}
          </div>
        </section>
      ))}
      <Button onClick={onRecord}><MicIcon />{strings.askByVoice}</Button>
    </Sheet>
  )
}
