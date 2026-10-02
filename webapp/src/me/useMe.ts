import type { Me } from '../api/types'
import { useAsync } from '../api/useAsync'
import { fetchMe } from './meApi'

export const useMe = () => useAsync<Me>(fetchMe)
