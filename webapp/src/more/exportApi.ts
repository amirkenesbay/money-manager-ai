import { apiRequest } from '../api/client'

export type ExportFormat = 'XLSX' | 'CSV'

export const exportOperations = (groupId: string, range: { from: string | null; to: string | null }, format: ExportFormat): Promise<{ count: number }> =>
  apiRequest<{ count: number }>(`/groups/${groupId}/export`, { method: 'POST', body: JSON.stringify({ ...range, format }) })
