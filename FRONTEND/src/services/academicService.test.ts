import { afterEach, describe, expect, it, vi } from 'vitest'
import { api } from './api'
import { attendanceService } from './attendanceService'
import { resultService } from './resultService'
import { studentService } from './studentService'
import { academicService } from './academicService'

afterEach(() => vi.restoreAllMocks())

describe('academic API services', () => {
  it('uses the Gateway API paths for student records and date attendance', async () => {
    const get = vi.spyOn(api, 'get').mockResolvedValue({ data: [] })
    await studentService.getStudents()
    expect(get).toHaveBeenCalledWith('/students')
    get.mockResolvedValueOnce({ data: [] })
    await attendanceService.getAttendanceByDate('2026-10-22')
    expect(get).toHaveBeenCalledWith('/attendance/date/2026-10-22')
  })

  it('sends attendance create, update, and delete through the shared API client', async () => {
    const post = vi.spyOn(api, 'post').mockResolvedValue({ data: { id: 1 } })
    const put = vi.spyOn(api, 'put').mockResolvedValue({ data: { id: 1 } })
    const remove = vi.spyOn(api, 'delete').mockResolvedValue({ data: {} })
    const request = { studentId: 17, courseCode: 'CS301', date: '2026-10-22', status: 'PRESENT' as const }
    await attendanceService.markAttendance(request)
    await attendanceService.updateAttendance(1, request)
    await attendanceService.deleteAttendance(1)
    expect(post).toHaveBeenCalledWith('/attendance', request)
    expect(put).toHaveBeenCalledWith('/attendance/1', request)
    expect(remove).toHaveBeenCalledWith('/attendance/1')
  })

  it('sends result create, update, and delete through the shared API client', async () => {
    const post = vi.spyOn(api, 'post').mockResolvedValue({ data: { id: 2 } })
    const put = vi.spyOn(api, 'put').mockResolvedValue({ data: { id: 2 } })
    const remove = vi.spyOn(api, 'delete').mockResolvedValue({ data: {} })
    const request = { studentId: 17, semester: 3, academicYear: '2025-2026', courseCode: 'CS301', courseName: 'Data Structures', credits: 3, internalMarks: 35, externalMarks: 52 }
    await resultService.recordResult(request)
    await resultService.updateResult(2, request)
    await resultService.deleteResult(2)
    expect(post).toHaveBeenCalledWith('/results', request)
    expect(put).toHaveBeenCalledWith('/results/2', request)
    expect(remove).toHaveBeenCalledWith('/results/2')
  })

  it('uses the academic enrolment API paths for courses, faculty assignment, and student enrollment', async () => {
    const get = vi.spyOn(api, 'get').mockResolvedValue({ data: [] })
    const post = vi.spyOn(api, 'post').mockResolvedValue({ data: { id: 9 } })
    await academicService.getCourses()
    await academicService.getAvailableCourses()
    const course = { courseCode: 'DS1010', courseName: 'Data Science', department: 'CSE', credits: 4, semester: 2, academicYear: '2026-2027', description: 'data science', active: true }
    await academicService.createCourse(course)
    await academicService.getFacultyStudents(12)
    await academicService.enrollStudent({ courseId: 5, facultyAssignmentId: 12 })
    expect(get).toHaveBeenCalledWith('/courses')
    expect(get).toHaveBeenCalledWith('/courses/available')
    expect(post).toHaveBeenCalledWith('/admin/courses', course)
    expect(get).toHaveBeenCalledWith('/faculty/my-students', { params: { courseId: 12 } })
    expect(post).toHaveBeenCalledWith('/enrollments', { courseId: 5, facultyAssignmentId: 12 })
  })
})
