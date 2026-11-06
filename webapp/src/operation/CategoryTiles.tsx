import { useState } from 'react'
import { selectionTick } from '../telegram/haptics'
import type { Category, CategoryType } from '../api/types'
import { strings } from '../strings'
import { PlusIcon } from '../ui/icons'
import { NewCategorySheet } from './NewCategorySheet'

export interface CategoryCreation {
  groupId: string
  type: CategoryType
  onCreated: (category: Category) => void
}

export function CategoryTiles({ categories, selectedId, onSelect, creation }: {
  categories: Category[]
  selectedId: string | null
  onSelect: (categoryId: string) => void
  creation?: CategoryCreation
}) {
  const [creating, setCreating] = useState(false)
  if (categories.length === 0 && !creation) return <p className="hint-box caption">{strings.noCategoriesOfType}</p>

  const created = (category: Category) => {
    setCreating(false)
    creation?.onCreated(category)
    onSelect(category.id)
  }

  return (
    <div className="category-tiles">
      {categories.map((category) => (
        <button key={category.id} type="button" className="category-tile" aria-pressed={category.id === selectedId} onClick={() => { selectionTick(); onSelect(category.id) }}>
          <span className="category-tile-icon" aria-hidden="true">{category.icon}</span>
          <span className="category-tile-name">{category.name}</span>
        </button>
      ))}
      {creation && (
        <button type="button" className="category-tile category-tile-add" aria-label={strings.addCategory} onClick={() => setCreating(true)}>
          <span className="category-tile-icon" aria-hidden="true"><PlusIcon /></span>
          <span className="category-tile-name">{strings.newCategoryTile}</span>
        </button>
      )}
      {creating && creation && (
        <NewCategorySheet groupId={creation.groupId} type={creation.type} onCreated={created} onClose={() => setCreating(false)} />
      )}
    </div>
  )
}
