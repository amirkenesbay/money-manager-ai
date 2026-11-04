import { type KeyboardEvent, useCallback, useState } from 'react'
import { useAsync } from '../api/useAsync'
import { fetchRecentTags } from '../operations/operationsApi'
import { MAX_TAG_LENGTH, normalizeTag } from '../operations/tags'
import { strings } from '../strings'
import { CloseIcon } from '../ui/icons'

const MAX_SUGGESTIONS = 5
const SUBMIT_KEY = 'Enter'
const CANCEL_KEY = 'Escape'

export function TagsField({ groupId, tags, onChange }: { groupId: string; tags: string[]; onChange: (tags: string[]) => void }) {
  const [draft, setDraft] = useState<string | null>(null)
  const load = useCallback((signal: AbortSignal) => fetchRecentTags(groupId, signal), [groupId])
  const recent = useAsync(load)
  const suggestions = recent.state.status === 'ready'
    ? recent.state.data.filter((tag) => !tags.includes(tag)).slice(0, MAX_SUGGESTIONS)
    : []

  const add = (input: string) => {
    const tag = normalizeTag(input)
    if (tag && !tags.includes(tag)) onChange([...tags, tag])
  }
  const commit = () => {
    if (draft !== null) add(draft)
    setDraft(null)
  }
  const onKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === SUBMIT_KEY) {
      event.preventDefault()
      commit()
    }
    if (event.key === CANCEL_KEY) setDraft(null)
  }

  return (
    <fieldset className="field-group">
      <legend className="field-label">{strings.tagsLabel}</legend>
      <div className="tag-chips">
        {tags.map((tag) => (
          <button key={tag} type="button" className="tag-chip selected" aria-label={strings.removeTag(tag)} onClick={() => onChange(tags.filter((item) => item !== tag))}>
            #{tag}<CloseIcon />
          </button>
        ))}
        {draft === null
          ? <button type="button" className="tag-chip add" onClick={() => setDraft('')}>{strings.addTag}</button>
          : (
            <input
              className="tag-input"
              aria-label={strings.newTagLabel}
              placeholder={strings.newTagPlaceholder}
              maxLength={MAX_TAG_LENGTH}
              autoFocus
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              onKeyDown={onKeyDown}
              onBlur={commit}
            />
          )}
      </div>
      {suggestions.length > 0 && (
        <div className="tag-chips">
          {suggestions.map((tag) => (
            <button key={tag} type="button" className="tag-chip" aria-label={strings.suggestTag(tag)} onClick={() => add(tag)}>#{tag}</button>
          ))}
        </div>
      )}
    </fieldset>
  )
}
