import { api } from './api'
import type { CurrentStudent, Student, StudentProfile, StudentWrite } from '../types/student'

export const studentService = {
  async getCurrentStudent(): Promise<CurrentStudent> {
    const response = await api.get<CurrentStudent>('/students/me')
    return response.data
  },
  async getStudents(): Promise<Student[]> {
    const response = await api.get<Student[]>('/students')
    return response.data
  },

  async getStudent(id: number): Promise<Student> {
    const response = await api.get<Student>(`/students/${id}`)
    return response.data
  },

  async getStudentProfile(id: number): Promise<StudentProfile> {
    const response = await api.get<StudentProfile>(`/students/${id}/profile`)
    return response.data
  },

  async createStudent(data: StudentWrite): Promise<Student> {
    const response = await api.post<Student>('/students', data)
    return response.data
  },

  async updateStudent(id: number, data: StudentWrite): Promise<Student> {
    const response = await api.put<Student>(`/students/${id}`, data)
    return response.data
  },

  async deleteStudent(id: number): Promise<void> {
    await api.delete(`/students/${id}`)
  },
}
