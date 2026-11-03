import type { MomentInputs, MomentKind } from '../moments'
import { createCoin } from './coin'
import { createCrystal } from './crystal'
import { createGroup } from './group'
import type { Model, ModelFactory } from './model'
import { createOrb } from './orb'
import { createRing } from './ring'

const FACTORIES: { [K in MomentKind]: ModelFactory<MomentInputs[K]> } = {
  orb: createOrb,
  coin: createCoin,
  ring: createRing,
  crystal: createCrystal,
  group: createGroup,
}

export function createModel<K extends MomentKind>(kind: K, read: () => MomentInputs[K]): Model {
  const factory: ModelFactory<MomentInputs[K]> = FACTORIES[kind]
  return factory(read)
}
