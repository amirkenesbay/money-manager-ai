import type { Category } from '../api/types'
import { strings } from '../strings'

export function CategoryTiles({ categories, selectedId, onSelect }: {
  categories: Category[]
  selectedId: string | null
  onSelect: (categoryId: string) => void
}) {
  if (categories.length === 0) return <p className="hint-box caption">{strings.noCategoriesOfType}</p>
  return (
    <div className="category-tiles">
      {categories.map((category) => (
        <button key={category.id} type="button" className="category-tile" aria-pressed={category.id === selectedId} onClick={() => onSelect(category.id)}>
          <span className="category-tile-icon" aria-hidden="true">{category.icon}</span>
          <span className="category-tile-name">{category.name}</span>
        </button>
      ))}
    </div>
  )
}
