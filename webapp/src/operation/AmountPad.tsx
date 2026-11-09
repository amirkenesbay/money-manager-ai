import { strings } from '../strings'
import { tapImpact } from '../telegram/haptics'
import { BackspaceIcon } from '../ui/icons'
import { DIVIDE, MINUS, type Operator, PLUS, TIMES } from './calc'

export const BACKSPACE = 'backspace'
export type PadKey = string

const ROWS: { keys: string[]; operator: Operator }[] = [
  { keys: ['1', '2', '3'], operator: PLUS },
  { keys: ['4', '5', '6'], operator: MINUS },
  { keys: ['7', '8', '9'], operator: TIMES },
  { keys: ['000', '0', BACKSPACE], operator: DIVIDE },
]

export function AmountPad({ onPress }: { onPress: (key: PadKey) => void }) {
  const press = (key: PadKey) => {
    tapImpact()
    onPress(key)
  }
  return (
    <div className="amount-pad" role="group" aria-label={strings.amountPadLabel}>
      {ROWS.flatMap(({ keys, operator }) => [
        ...keys.map((key) => (
          <button key={key} type="button" className="pad-key" aria-label={key === BACKSPACE ? strings.amountErase : undefined} onClick={() => press(key)}>
            {key === BACKSPACE ? <BackspaceIcon /> : key}
          </button>
        )),
        <button key={operator} type="button" className="pad-key pad-operator" onClick={() => press(operator)}>{operator}</button>,
      ])}
    </div>
  )
}
