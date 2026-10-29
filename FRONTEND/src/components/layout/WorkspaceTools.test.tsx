import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { WorkspaceTools } from './WorkspaceTools'
import { assistantService } from '../../services/workspaceService'

vi.mock('../../services/workspaceService', () => ({
  notificationsEnabled: true,
  assistantEnabled: true,
  notificationService: {
    list: vi.fn().mockResolvedValue([]),
    unreadCount: vi.fn().mockResolvedValue(0),
    markRead: vi.fn(),
    markAllRead: vi.fn(),
  },
  assistantService: { ask: vi.fn().mockResolvedValue({ answer: 'Recorded attendance is 92%.', sources: ['attendance summary'] }) },
}))

afterEach(cleanup)

describe('role-aware workspace tools', () => {
  it('loads the real notification panel without inventing an unread badge', async () => {
    render(<WorkspaceTools role="STUDENT" />)
    fireEvent.click(screen.getByRole('button', { name: 'Notifications' }))
    expect(await screen.findByText('You’re all caught up')).toBeInTheDocument()
    expect(screen.getByText(/There are no notifications for your account/)).toBeInTheDocument()
  })

  it('shows source labels with authorized assistant answers', async () => {
    render(<WorkspaceTools role="STUDENT" />)
    fireEvent.click(screen.getByRole('button', { name: 'CampusCore Assistant' }))
    fireEvent.click(screen.getByRole('button', { name: 'How is my attendance?' }))
    expect(await screen.findByText('Recorded attendance is 92%.')).toBeInTheDocument()
    expect(screen.getByText('Based on: attendance summary')).toBeInTheDocument()
    expect(assistantService.ask).toHaveBeenCalledWith('How is my attendance?')
  })
})
