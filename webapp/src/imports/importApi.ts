import { apiRequest } from '../api/client'
import type { CategoryChoice, ColumnRole, ImportPreview, ImportResult } from '../api/types'

const BINARY_CONTENT_TYPE = 'application/octet-stream'
const importsPath = (groupId: string) => `/groups/${groupId}/imports`

export const previewImport = (groupId: string, file: Blob): Promise<ImportPreview> =>
  apiRequest<ImportPreview>(`${importsPath(groupId)}/preview`, { method: 'POST', body: file, headers: { 'Content-Type': BINARY_CONTENT_TYPE } })

export const fetchImport = (groupId: string, token: string, signal?: AbortSignal): Promise<ImportPreview> =>
  apiRequest<ImportPreview>(`${importsPath(groupId)}/${token}`, {}, signal)

export const setImportColumns = (groupId: string, token: string, roles: Partial<Record<ColumnRole, number>>): Promise<ImportPreview> =>
  apiRequest<ImportPreview>(`${importsPath(groupId)}/${token}/columns`, { method: 'PUT', body: JSON.stringify({ roles }) })

export const commitImport = (groupId: string, token: string, categories: Record<string, CategoryChoice>): Promise<ImportResult> =>
  apiRequest<ImportResult>(`${importsPath(groupId)}/${token}`, { method: 'POST', body: JSON.stringify({ categories }) })

export const undoImport = (groupId: string, importId: string): Promise<void> =>
  apiRequest<void>(`${importsPath(groupId)}/batches/${importId}`, { method: 'DELETE' })
