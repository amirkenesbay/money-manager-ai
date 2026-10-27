import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiRequestError } from '../api/client'
import { deleteOperation } from '../operations/operationsApi'
import { strings } from '../strings'
import { group, operation, PERSONAL_GROUP_ID } from '../test/fixtures'
import { OperationDetail } from './OperationDetail'

vi.mock('../operations/operationsApi', () => ({ deleteOperation: vi.fn() }))

const onDeleted = vi.fn()
const onEdit = vi.fn()
const renderDetail = () => render(<OperationDetail group={group()} language="ru" operation={operation()} onBack={vi.fn()} onEdit={onEdit} onDeleted={onDeleted} />)

describe('OperationDetail', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('shows who added the operation and its comment', () => {
    renderDetail()

    expect(screen.getByText('Магнум')).toBeInTheDocument()
    expect(screen.getByText('Амир')).toBeInTheDocument()
    expect(screen.getByText(`${strings.operationType('EXPENSE')} · Продукты`)).toBeInTheDocument()
  })

  it('opens editing', async () => {
    renderDetail()

    await userEvent.click(screen.getByRole('button', { name: strings.edit }))

    expect(onEdit).toHaveBeenCalled()
  })

  it('deletes only after confirmation', async () => {
    vi.mocked(deleteOperation).mockResolvedValue(undefined)
    renderDetail()

    await userEvent.click(screen.getByRole('button', { name: strings.delete }))
    const sheet = screen.getByRole('dialog', { name: strings.deleteQuestion })
    expect(deleteOperation).not.toHaveBeenCalled()

    await userEvent.click(sheet.querySelector('.button-destructive')!)

    expect(deleteOperation).toHaveBeenCalledWith(PERSONAL_GROUP_ID, 'op-1')
    expect(onDeleted).toHaveBeenCalledWith(operation())
  })

  it('keeps the operation when the user changes their mind', async () => {
    renderDetail()

    await userEvent.click(screen.getByRole('button', { name: strings.delete }))
    await userEvent.click(screen.getByRole('button', { name: strings.keep }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(deleteOperation).not.toHaveBeenCalled()
  })

  it('explains a failed deletion', async () => {
    vi.mocked(deleteOperation).mockRejectedValue(new ApiRequestError('Failed to fetch', null, null))
    renderDetail()

    await userEvent.click(screen.getByRole('button', { name: strings.delete }))
    await userEvent.click(screen.getByRole('dialog').querySelector('.button-destructive')!)

    expect(await screen.findByText(strings.deleteFailed)).toBeInTheDocument()
    expect(onDeleted).not.toHaveBeenCalled()
  })
})
