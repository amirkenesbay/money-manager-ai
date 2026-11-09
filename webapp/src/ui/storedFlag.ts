const FLAG_VALUE = '1'

export function isFlagStored(key: string): boolean {
  try {
    return localStorage.getItem(key) !== null
  } catch {
    return false
  }
}

export function clearFlag(key: string): void {
  try {
    localStorage.removeItem(key)
  } catch {
    return
  }
}

export function storeFlag(key: string): void {
  try {
    localStorage.setItem(key, FLAG_VALUE)
  } catch {
    return
  }
}
