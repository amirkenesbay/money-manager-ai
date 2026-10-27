import type { ButtonHTMLAttributes } from 'react'

type ButtonVariant = 'main' | 'secondary'
type ButtonSize = 'large' | 'small'

export function Button({ variant = 'main', size = 'large', className, type = 'button', ...props }: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant
  size?: ButtonSize
}) {
  const classes = ['button', `button-${variant}`, size === 'small' && 'button-small', 'pressable', className]
  return <button type={type} className={classes.filter(Boolean).join(' ')} {...props} />
}
