import { useState } from 'react'
import { suggestEmoji } from '../ui/emojiCatalog'

export function useCategoryIcon(saved: string | null, name: string, fallback: string) {
  const [picked, setPicked] = useState<string | null>(saved)
  const suggestion = suggestEmoji(name)
  return {
    icon: picked ?? suggestion ?? fallback,
    suggestion,
    pick: setPicked,
  }
}
