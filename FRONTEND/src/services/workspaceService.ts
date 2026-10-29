import { api } from './api'

export type NotificationCategory = 'ATTENDANCE' | 'RESULTS' | 'ACADEMIC' | 'FACULTY' | 'SYSTEM' | 'SECURITY'
export interface WorkspaceNotification {
  id: number
  category: NotificationCategory
  title: string
  message: string
  createdAt: string
  readAt: string | null
  actionUrl?: string | null
}
export interface AssistantReply {
  answer: string
  sources: string[]
}

export const notificationsEnabled = import.meta.env.VITE_NOTIFICATIONS_ENABLED !== 'false'
export const assistantEnabled = import.meta.env.VITE_EDUPULSE_ASSISTANT_ENABLED !== 'false'

export const notificationService = {
  async list(): Promise<WorkspaceNotification[]> {
    if (!notificationsEnabled) throw new Error('Notification service is not configured.')
    const response = await api.get<WorkspaceNotification[]>('/notifications')
    return response.data
  },
  async unreadCount(): Promise<number> {
    if (!notificationsEnabled) throw new Error('Notification service is not configured.')
    const response = await api.get<{ count: number }>('/notifications/unread-count')
    return response.data.count
  },
  async markRead(id: number): Promise<void> {
    if (!notificationsEnabled) throw new Error('Notification service is not configured.')
    await api.patch(`/notifications/${id}/read`)
  },
  async markAllRead(): Promise<void> {
    if (!notificationsEnabled) throw new Error('Notification service is not configured.')
    await api.patch('/notifications/read-all')
  },
}

export const assistantService = {
  async ask(question: string): Promise<AssistantReply> {
    if (!assistantEnabled) throw new Error('CampusCore Assistant is not configured for this deployment.')
    const response = await api.post<AssistantReply>('/assistant/chat', { message: question })
    return response.data
  },
}
