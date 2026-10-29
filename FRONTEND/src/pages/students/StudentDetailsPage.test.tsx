import { cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { studentService } from '../../services/studentService'
import type { StudentProfile } from '../../types/student'
import { StudentDetailsPage } from './StudentDetailsPage'

vi.mock('../../services/studentService', () => ({
  studentService: {
    getStudents: vi.fn(),
    getStudent: vi.fn(),
    getStudentProfile: vi.fn(),
    createStudent: vi.fn(),
    updateStudent: vi.fn(),
    deleteStudent: vi.fn(),
  },
}))

afterEach(() => {
  cleanup()
})

describe('student profile', () => {
  it('shows student fields and only backend-provided attendance and result records', async () => {
    const profile: StudentProfile = {
      studentDetails: {
        id: 6,
        name: 'Ira Das',
        email: 'ira@example.edu',
        age: 18,
        department: 'Physics',
        courses: 'PHY101',
        contact: null,
        role: 'STUDENT',
        semester: 1,
        academicYear: '2025-2026',
        status: 'ACTIVE',
      },
      attendanceRecords: [{
        id: 2,
        studentId: 6,
        courseCode: 'PHY101',
        date: '2025-09-01',
        status: 'PRESENT',
        remarks: null,
        markedBy: 'Faculty',
      }],
      resultRecords: [{
        id: 3,
        studentId: 6,
        semester: 1,
        academicYear: '2025-2026',
        courseCode: 'PHY101',
        courseName: 'Physics I',
        credits: 3,
        internalMarks: 35,
        externalMarks: 55,
        totalMarks: 90,
        grade: 'S',
        gradePoints: 10,
        resultStatus: 'PASS',
      }],
      totalAttendanceRecords: 1,
      totalResultRecords: 1,
    }
    vi.mocked(studentService.getStudentProfile).mockResolvedValue(profile)
    render(
      <MemoryRouter initialEntries={['/students/6']}>
        <Routes><Route path="/students/:id" element={<StudentDetailsPage />} /></Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByRole('heading', { name: 'Ira Das' })).toBeInTheDocument()
    expect(screen.getByText('2025-09-01')).toBeInTheDocument()
    expect(screen.getByText('Physics I (PHY101)')).toBeInTheDocument()
    expect(screen.getByText('S · 90')).toBeInTheDocument()
  })
})
