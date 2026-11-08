import { useMemo, useState } from 'react'
import { strings } from '../strings'
import { EMOJI_GROUPS, searchEmoji } from './emojiCatalog'

const graphemes = new Intl.Segmenter(undefined, { granularity: 'grapheme' })

const firstGrapheme = (text: string): string => graphemes.segment(text.trim())[Symbol.iterator]().next().value?.segment ?? ''

export function EmojiPicker({ value, presets, suggestion = null, onChange }: {
  value: string
  presets: string[]
  suggestion?: string | null
  onChange: (emoji: string) => void
}) {
  const [libraryOpen, setLibraryOpen] = useState(false)
  const offered = [...new Set([suggestion, ...presets].filter((emoji): emoji is string => Boolean(emoji)))]
  const quick = (offered.includes(value) ? offered : [value, ...offered]).slice(0, presets.length)
  return (
    <fieldset className="field-group">
      <legend className="field-label">{strings.iconLabel}</legend>
      <div className="emoji-grid">
        {quick.map((emoji) => (
          <EmojiTile key={emoji} emoji={emoji} selected={emoji === value} onPick={onChange} />
        ))}
        <button type="button" className="emoji-tile emoji-more" aria-expanded={libraryOpen} onClick={() => setLibraryOpen(!libraryOpen)}>
          {libraryOpen ? strings.iconLess : strings.iconMore}
        </button>
      </div>
      {libraryOpen && <EmojiLibrary value={value} onChange={onChange} />}
    </fieldset>
  )
}

function EmojiLibrary({ value, onChange }: { value: string; onChange: (emoji: string) => void }) {
  const [query, setQuery] = useState('')
  const found = useMemo(() => searchEmoji(query), [query])
  const sections = EMOJI_GROUPS
    .map((group) => ({ group, items: found.filter((item) => item.group === group) }))
    .filter((section) => section.items.length > 0)
  return (
    <div className="emoji-library">
      <input
        className="text-field emoji-search"
        type="search"
        aria-label={strings.iconSearchLabel}
        placeholder={strings.iconSearchPlaceholder}
        value={query}
        onChange={(event) => setQuery(event.target.value)}
      />
      {sections.length === 0 && <p className="caption">{strings.iconSearchEmpty}</p>}
      {sections.map(({ group, items }) => (
        <section key={group} className="emoji-section" aria-label={strings.emojiGroupNames[group]}>
          <p className="caption">{strings.emojiGroupNames[group]}</p>
          <div className="emoji-grid">
            {items.map((item) => <EmojiTile key={item.emoji} emoji={item.emoji} selected={item.emoji === value} onPick={onChange} />)}
          </div>
        </section>
      ))}
      <label className="emoji-custom">
        <span className="caption">{strings.customIconLabel}</span>
        <input
          className="emoji-tile emoji-input"
          aria-label={strings.customIconLabel}
          placeholder={strings.customIconPlaceholder}
          onChange={(event) => {
            const emoji = firstGrapheme(event.target.value)
            if (emoji) onChange(emoji)
          }}
        />
      </label>
    </div>
  )
}

function EmojiTile({ emoji, selected, onPick }: { emoji: string; selected: boolean; onPick: (emoji: string) => void }) {
  return (
    <button type="button" className="emoji-tile" aria-label={emoji} aria-pressed={selected} onClick={() => onPick(emoji)}>
      {emoji}
    </button>
  )
}
