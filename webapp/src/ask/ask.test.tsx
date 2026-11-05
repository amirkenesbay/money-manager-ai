import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { strings } from '../strings'
import { fetchAskSuggestions } from './askApi'
import { AskChip } from './AskChip'
import { AskTopicsSheet } from './AskTopicsSheet'
import { hintItems } from './hintItems'

vi.mock('./askApi', () => ({ askQuestion: vi.fn(), fetchAskSuggestions: vi.fn() }))

const QUESTION = 'Сколько на кафе в этом месяце?'

describe('ask', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    localStorage.clear()
  })

  it('alternates one recording example with personal questions', () => {
    expect(hintItems(['«кофе 1 500»', '«такси 2 000»'], ['a', 'b', 'c', 'd'])).toEqual([
      { kind: 'say', text: '«кофе 1 500»' }, { kind: 'ask', text: 'a' }, { kind: 'ask', text: 'b' }, { kind: 'ask', text: 'c' },
    ])
    expect(hintItems(['«кофе 1 500»'], [])).toEqual([{ kind: 'say', text: '«кофе 1 500»' }])
  })

  it('groups personal questions by topic and asks the tapped one', async () => {
    vi.mocked(fetchAskSuggestions).mockResolvedValue({
      questions: [QUESTION],
      topics: [{ topic: 'SUM', title: 'Сколько', questions: [QUESTION] }],
    })
    const onAsk = vi.fn()
    render(<AskTopicsSheet groupId="group-1" onAsk={onAsk} onRecord={vi.fn()} onClose={vi.fn()} />)

    expect(await screen.findByRole('region', { name: 'Сколько' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: QUESTION }))

    expect(onAsk).toHaveBeenCalledWith(QUESTION)
  })

  it('hides a context question for good once closed', async () => {
    const view = render(<AskChip place="reports" question={strings.askReportsChip} onAsk={vi.fn()} />)

    await userEvent.click(screen.getByRole('button', { name: strings.askChipHide }))
    view.unmount()
    render(<AskChip place="reports" question={strings.askReportsChip} onAsk={vi.fn()} />)

    expect(screen.queryByRole('button', { name: strings.askReportsChip })).not.toBeInTheDocument()
  })
})
