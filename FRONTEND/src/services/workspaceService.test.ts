import { afterEach, describe, expect, it, vi } from 'vitest'
import { api } from './api'
import { assistantEnabled, assistantService, notificationService, notificationsEnabled } from './workspaceService'

vi.mock('./api', () => ({ api: { get: vi.fn(), post: vi.fn(), patch: vi.fn() } }))
afterEach(() => vi.clearAllMocks())

describe('workspace service boundaries', () => {
  it('does not call a notification endpoint unless notifications are explicitly enabled', async () => {
    if (notificationsEnabled) return
    await expect(notificationService.list()).rejects.toThrow('Notification service is not configured.')
    expect(api.get).not.toHaveBeenCalled()
  })

  it('does not produce assistant answers when an assistant backend is not configured', async () => {
    if (assistantEnabled) return
    await expect(assistantService.ask('What is my CGPA?')).rejects.toThrow('CampusCore Assistant is not configured')
    expect(api.post).not.toHaveBeenCalled()
  })

  it('uses authenticated notification endpoints when notifications are enabled', async () => {
    if (!notificationsEnabled) return
    vi.mocked(api.get).mockResolvedValueOnce({ data: [] } as never).mockResolvedValueOnce({ data: { count: 2 } } as never)
    await expect(notificationService.list()).resolves.toEqual([])
    await expect(notificationService.unreadCount()).resolves.toBe(2)
    expect(api.get).toHaveBeenNthCalledWith(1, '/notifications')
    expect(api.get).toHaveBeenNthCalledWith(2, '/notifications/unread-count')
  })

  it('posts assistant questions and preserves the source labels returned by the backend', async () => {
    if (!assistantEnabled) return
    const reply = { answer: 'Recorded attendance is 92%.', sources: ['attendance summary'] }
    vi.mocked(api.post).mockResolvedValueOnce({ data: reply } as never)
    await expect(assistantService.ask('How is my attendance?')).resolves.toEqual(reply)
    expect(api.post).toHaveBeenCalledWith('/assistant/chat', { message: 'How is my attendance?' })
  })
})
