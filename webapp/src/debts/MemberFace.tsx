export function MemberFace({ name, size = 'small' }: { name: string; size?: 'small' | 'large' }) {
  return <span className={`member-face ${size}`} aria-hidden="true">{name.slice(0, 1).toUpperCase()}</span>
}
