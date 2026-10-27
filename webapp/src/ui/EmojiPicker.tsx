import { strings } from '../strings'

const graphemes = new Intl.Segmenter(undefined, { granularity: 'grapheme' })

const firstGrapheme = (text: string): string => graphemes.segment(text.trim())[Symbol.iterator]().next().value?.segment ?? ''

export function EmojiPicker({ value, presets, onChange }: { value: string; presets: string[]; onChange: (emoji: string) => void }) {
  const custom = presets.includes(value) ? '' : value
  return (
    <fieldset className="field-group">
      <legend className="field-label">{strings.iconLabel}</legend>
      <div className="emoji-grid">
        {presets.map((emoji) => (
          <button key={emoji} type="button" className="emoji-tile" aria-label={emoji} aria-pressed={emoji === value} onClick={() => onChange(emoji)}>
            {emoji}
          </button>
        ))}
        <input
          className="emoji-tile emoji-input"
          aria-label={strings.customIconLabel}
          placeholder="+"
          value={custom}
          onChange={(event) => {
            const emoji = firstGrapheme(event.target.value)
            if (emoji) onChange(emoji)
          }}
        />
      </div>
    </fieldset>
  )
}
