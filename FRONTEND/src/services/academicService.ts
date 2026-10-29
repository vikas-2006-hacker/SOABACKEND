import { api } from './api'
import type { AcademicCourse, AcademicCourseEnrollmentInput, CourseOffering, CourseOfferingFaculty, FacultyCourseAssignment, StudentCourseEnrollment, StudentCourseEnrollmentView } from '../types/academic'
import type { Student } from '../types/student'

export const academicService = {
  async getCourses(): Promise<AcademicCourse[]> {
    const response = await api.get<AcademicCourse[]>('/courses')
    return response.data
  },

  async getCourse(courseId: number): Promise<AcademicCourse> {
    const response = await api.get<AcademicCourse>(`/courses/${courseId}`)
    return response.data
  },

  async getAvailableCourses(): Promise<CourseOffering[]> {
    const response = await api.get<CourseOffering[]>('/courses/available')
    return response.data
  },

  async getStudentCourseDetails(courseId: number): Promise<CourseOffering> {
    const response = await api.get<CourseOffering>(`/courses/available/${courseId}`)
    return response.data
  },

  async getCourseFaculty(courseId: number): Promise<FacultyCourseAssignment[]> {
    const response = await api.get<FacultyCourseAssignment[]>(`/courses/${courseId}/faculty`)
    return response.data
  },

  async createCourse(course: Omit<AcademicCourse, 'id' | 'createdAt' | 'updatedAt'>): Promise<AcademicCourse> {
    const response = await api.post<AcademicCourse>('/admin/courses', course)
    return response.data
  },

  async updateCourse(courseId: number, course: Partial<Omit<AcademicCourse, 'id' | 'createdAt' | 'updatedAt'>>): Promise<AcademicCourse> {
    const response = await api.put<AcademicCourse>(`/admin/courses/${courseId}`, course)
    return response.data
  },

  async assignFacultyToCourse(payload: {
    facultyEmail: string
    courseId: number
    academicYear?: string
    semester?: number
  }): Promise<FacultyCourseAssignment> {
    const response = await api.post<FacultyCourseAssignment>('/admin/faculty-courses', payload)
    return response.data
  },

  async getFacultyCourses(): Promise<AcademicCourse[]> {
    const response = await api.get<AcademicCourse[]>('/faculty/my-courses')
    return response.data
  },

  async getFacultyStudents(courseId?: number): Promise<Student[]> {
    const response = await api.get<Student[]>('/faculty/my-students', {
      params: courseId ? { courseId } : undefined,
    })
    return response.data
  },

  async enrollStudent(payload: AcademicCourseEnrollmentInput): Promise<StudentCourseEnrollmentView> {
    const response = await api.post<StudentCourseEnrollmentView>('/enrollments', payload)
    return response.data
  },

  async getMyEnrollments(): Promise<StudentCourseEnrollmentView[]> {
    const response = await api.get<StudentCourseEnrollmentView[]>('/enrollments/my')
    return response.data
  },

  async getStudentEnrollments(studentId: number): Promise<StudentCourseEnrollmentView[]> {
    const response = await api.get<StudentCourseEnrollmentView[]>(`/enrollments/my/${studentId}`)
    return response.data
  },

  async listAllEnrollments(): Promise<StudentCourseEnrollment[]> {
    const response = await api.get<StudentCourseEnrollment[]>('/admin/enrollments')
    return response.data
  },

  async removeEnrollment(enrollmentId: number): Promise<void> {
    await api.delete(`/enrollments/${enrollmentId}`)
  },
}

export type { AcademicCourse, CourseOffering, CourseOfferingFaculty, FacultyCourseAssignment, StudentCourseEnrollment, StudentCourseEnrollmentView, AcademicCourseEnrollmentInput }
