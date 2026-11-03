export const MAX_GROUP_BALLS = 6

export type OrbMode = 'listening' | 'thinking'

export interface MomentInputs {
  orb: { mode: OrbMode; level: () => number }
  coin: { onLanded: () => void }
  ring: { values: number[]; selected: number | null; onSelect: (index: number) => void }
  crystal: Record<string, never>
  group: { initials: string[]; you: number }
}

export type MomentKind = keyof MomentInputs
