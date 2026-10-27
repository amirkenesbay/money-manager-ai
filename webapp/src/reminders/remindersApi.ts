import { apiRequest } from '../api/client'
import type { Reminder, ReminderInput } from '../api/types'

const REMINDERS_PATH = '/notifications'

const reminderPath = (reminderId: string) => `${REMINDERS_PATH}/${reminderId}`

export const fetchReminders = (signal?: AbortSignal): Promise<Reminder[]> => apiRequest<Reminder[]>(REMINDERS_PATH, {}, signal)

export const createReminder = (input: ReminderInput): Promise<Reminder> =>
  apiRequest<Reminder>(REMINDERS_PATH, { method: 'POST', body: JSON.stringify(input) })

export const replaceReminder = (reminderId: string, input: ReminderInput): Promise<Reminder> =>
  apiRequest<Reminder>(reminderPath(reminderId), { method: 'PUT', body: JSON.stringify(input) })

export const setReminderActive = (reminderId: string, isActive: boolean): Promise<Reminder> =>
  apiRequest<Reminder>(reminderPath(reminderId), { method: 'PATCH', body: JSON.stringify({ isActive }) })

export const deleteReminder = (reminderId: string): Promise<void> =>
  apiRequest<void>(reminderPath(reminderId), { method: 'DELETE' })
