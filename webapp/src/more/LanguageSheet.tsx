import { useState } from 'react'
import type { Me } from '../api/types'
import { isLanguage, LANGUAGE_NAMES, LANGUAGES, type Language } from '../i18n/languages'
import { updateMe } from '../me/meApi'
import { strings } from '../strings'
import { Button } from '../ui/Button'
import { Segmented } from '../ui/Segmented'
import { Sheet } from '../ui/Sheet'
import { useSubmit } from '../ui/useSubmit'

export function LanguageSheet({ me, onChanged, onClose }: { me: Me; onChanged: (me: Me) => void; onClose: () => void }) {
  const [language, setLanguage] = useState<Language>(isLanguage(me.language) ? me.language : LANGUAGES[0])
  const submit = useSubmit()

  const done = async () => {
    if (language === me.language) {
      onClose()
      return
    }
    const result = await submit.run(() => updateMe({ language }), strings.saveFailed)
    if (result.ok) onChanged(result.value)
  }

  return (
    <Sheet label={strings.languageSheetTitle} onClose={onClose}>
      <h2 className="sheet-title">{strings.languageSheetTitle}</h2>
      <Segmented variant="choices" label={strings.languageSheetTitle} options={[...LANGUAGES]} value={language} optionKey={(code) => code} optionLabel={(code) => LANGUAGE_NAMES[code]} onChange={setLanguage} />
      <p className="hint">{strings.languageSheetHint}</p>
      {submit.error && <p className="error-text" role="alert">{submit.error.message}</p>}
      <Button disabled={submit.pending} onClick={done}>{strings.done}</Button>
    </Sheet>
  )
}
