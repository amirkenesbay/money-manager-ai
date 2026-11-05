import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiRequestError } from '../api/client'
import type { ImportPreview } from '../api/types'
import { fetchCategories } from '../categories/categoriesApi'
import { strings } from '../strings'
import { category, group, me } from '../test/fixtures'
import { commitImport, previewImport, setImportColumns, undoImport } from './importApi'
import { ImportScreen } from './ImportScreen'

vi.mock('./importApi', () => ({ previewImport: vi.fn(), fetchImport: vi.fn(), setImportColumns: vi.fn(), commitImport: vi.fn(), undoImport: vi.fn() }))
vi.mock('../categories/categoriesApi', () => ({ fetchCategories: vi.fn() }))

const groceries = category({ id: 'cat-groceries', name: 'Продукты', icon: '🛒' })
const cafe = category({ id: 'cat-cafe', name: 'Кафе', icon: '🍔' })
const file = new File(['date;categoryName;outcome'], 'zenmoney.csv', { type: 'text/csv' })
const preview = (overrides: Partial<ImportPreview> = {}): ImportPreview => ({
  token: 'token-1',
  source: 'ZENMONEY',
  columns: [{ header: 'date', samples: ['2026-11-03'], role: 'DATE' }, { header: 'outcome', samples: ['1500'], role: 'EXPENSE' }],
  needsColumns: false,
  count: 840,
  from: '2024-01-01',
  to: '2026-11-03',
  skipped: { TRANSFER: 30 },
  categories: [
    { key: 'EXPENSE:продукты', name: 'Продукты', type: 'EXPENSE', count: 620, existingId: groceries.id },
    { key: 'EXPENSE:кофейни', name: 'Кофейни', type: 'EXPENSE', count: 180, existingId: null },
    { key: 'EXPENSE:собака', name: 'Собака', type: 'EXPENSE', count: 40, existingId: null },
  ],
  importsLeft: 2,
  ...overrides,
})
const notify = vi.fn()
const onOpenOperations = vi.fn()
const renderScreen = () => render(<ImportScreen me={me()} group={group()} notify={notify} onBack={vi.fn()} onOpenOperations={onOpenOperations} />)

async function upload() {
  const user = userEvent.setup()
  renderScreen()
  await user.upload(screen.getByLabelText(strings.importPick, { selector: 'input' }), file)
  return user
}

describe('ImportScreen', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(fetchCategories).mockResolvedValue([groceries, cafe])
  })

  it('reads a file, merges one category into an existing one, skips another and imports the rest', async () => {
    vi.mocked(previewImport).mockResolvedValue(preview())
    vi.mocked(commitImport).mockResolvedValue({ importId: 'batch-1', count: 800, createdCategories: 0, from: '2024-01-01', to: '2026-11-03', historyLimited: true })
    const user = await upload()

    expect(await screen.findByText(strings.importLooksLike(strings.importApps.ZENMONEY))).toBeInTheDocument()
    expect(screen.getByText(/переводы 30/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /Кофейни/ }))
    await user.click(await screen.findByRole('radio', { name: strings.importInto('🍔 Кафе') }))
    await user.click(screen.getByRole('button', { name: /Собака/ }))
    await user.click(screen.getByRole('radio', { name: strings.importDoNotMove }))
    await user.click(screen.getByRole('button', { name: strings.importGo(800) }))

    expect(commitImport).toHaveBeenCalledWith(group().id, 'token-1', {
      'EXPENSE:продукты': { action: 'EXISTING', categoryId: groceries.id },
      'EXPENSE:кофейни': { action: 'EXISTING', categoryId: cafe.id },
      'EXPENSE:собака': { action: 'SKIP' },
    })
    expect(await screen.findByText(strings.importDone(800))).toBeInTheDocument()
    expect(screen.getByText(strings.importHistoryNote)).toBeInTheDocument()
  })

  it('undoes a finished import', async () => {
    vi.mocked(previewImport).mockResolvedValue(preview())
    vi.mocked(commitImport).mockResolvedValue({ importId: 'batch-1', count: 840, createdCategories: 2, from: null, to: null, historyLimited: false })
    vi.mocked(undoImport).mockResolvedValue(undefined)
    const user = await upload()

    await user.click(await screen.findByRole('button', { name: strings.importGo(840) }))
    await user.click(await screen.findByRole('button', { name: strings.importUndo }))

    expect(undoImport).toHaveBeenCalledWith(group().id, 'batch-1')
    expect(notify).toHaveBeenCalledWith({ title: strings.importUndone })
  })

  it('asks for columns when the file is unknown', async () => {
    const unknown = preview({ source: null, needsColumns: true, count: 0, categories: [], columns: [{ header: 'when', samples: ['03.11.2026'], role: null }, { header: 'sum', samples: ['-1500'], role: null }] })
    vi.mocked(previewImport).mockResolvedValue(unknown)
    vi.mocked(setImportColumns).mockResolvedValue(preview())
    const user = await upload()

    await user.selectOptions(await screen.findByRole('combobox', { name: 'when' }), 'DATE')
    await user.selectOptions(screen.getByRole('combobox', { name: 'sum' }), 'AMOUNT')
    await user.click(screen.getByRole('button', { name: strings.importNext }))

    expect(setImportColumns).toHaveBeenCalledWith(group().id, 'token-1', { DATE: 0, AMOUNT: 1 })
    expect(await screen.findByRole('button', { name: strings.importGo(840) })).toBeInTheDocument()
  })

  it('shows the free limit when all imports are used', async () => {
    vi.mocked(previewImport).mockResolvedValue(preview())
    vi.mocked(commitImport).mockRejectedValue(new ApiRequestError('limit', 422, { code: 'IMPORT_LIMIT_REACHED', message: 'limit', details: { limit: 3 } }))
    const user = await upload()

    await user.click(await screen.findByRole('button', { name: strings.importGo(840) }))

    expect(await screen.findByText(strings.importLimitBody)).toBeInTheDocument()
  })
})
