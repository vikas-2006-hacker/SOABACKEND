import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { FacultyPage } from './FacultyPage'
import { facultyService } from '../../services/facultyService'

vi.mock('../../services/facultyService', () => ({ facultyService: { list: vi.fn(), create: vi.fn(), update: vi.fn(), setStatus: vi.fn() } }))

const list = vi.mocked(facultyService.list)
const create = vi.mocked(facultyService.create)

afterEach(() => { cleanup(); vi.clearAllMocks() })

describe('faculty management', () => {
  it('shows existing staff and MFA enrollment status', async () => {
    list.mockResolvedValue([{ id: 12, name: 'Asha Rao', email: 'asha@school.edu', employeeId: 'FAC012', department: 'Science', status: 'ACTIVE', mfaEnabled: false, createdAt: '', updatedAt: '' }])
    render(<FacultyPage />)
    expect(await screen.findByText('asha@school.edu')).toBeInTheDocument()
    expect(screen.getByText('Setup pending')).toBeInTheDocument()
  })

  it('creates a faculty account with matching temporary password fields', async () => {
    list.mockResolvedValue([])
    create.mockResolvedValue({ id: 4, name: 'Asha Rao', email: 'asha@school.edu', employeeId: 'FAC004', department: 'Science', status: 'ACTIVE', mfaEnabled: false, createdAt: '', updatedAt: '' })
    render(<FacultyPage />)
    fireEvent.click(await screen.findByRole('button', { name: /add faculty/i }))
    fireEvent.change(screen.getByLabelText('Full name'), { target: { value: 'Asha Rao' } })
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'asha@school.edu' } })
    fireEvent.change(screen.getByLabelText('Employee ID'), { target: { value: 'FAC004' } })
    fireEvent.change(screen.getByLabelText('Department'), { target: { value: 'Science' } })
    fireEvent.change(screen.getByLabelText('Temporary password'), { target: { value: 'temporary-123' } })
    fireEvent.change(screen.getByLabelText('Confirm password'), { target: { value: 'temporary-123' } })
    fireEvent.click(screen.getByRole('button', { name: /create faculty/i }))
    await waitFor(() => expect(create).toHaveBeenCalledWith({ name: 'Asha Rao', email: 'asha@school.edu', employeeId: 'FAC004', department: 'Science', password: 'temporary-123' }))
  })
})
