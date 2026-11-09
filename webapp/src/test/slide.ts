import { within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

export async function confirmBySlide(dialog: HTMLElement, label: string): Promise<void> {
  within(dialog).getByRole('button', { name: label }).focus()
  await userEvent.keyboard('{Enter}')
}
