export const OPERATION_CARD = 'operation-card'

let named: HTMLElement | null = null

export function shareElement(element: HTMLElement, name: string): void {
  if (named && named !== element) named.style.viewTransitionName = ''
  element.style.viewTransitionName = name
  named = element
}
