import { api } from './api'
import type { AttendanceRecord, AttendanceStatus, AttendanceWrite } from '../types/academic'

export interface AttendanceSummary {
  totalClasses: number
  classesAttended: number
  classesAbsent: number
  classesExcused: number
  attendancePercentage: number
  preciseAttendancePercentage: number
  examEligible: boolean
  attendanceStatus: 'EXCELLENT' | 'GOOD' | 'WARNING' | 'CRITICAL' | 'INSUFFICIENT_DATA' | string
  attendanceRisk: 'LOW' | 'MODERATE' | 'HIGH' | 'UNKNOWN' | string
  requiredAttendanceThreshold: number
}

export interface BulkAttendanceWrite {
  courseCode: string
  date: string
  attendanceRecords: Array<{ studentId: number; status: AttendanceStatus; remarks?: string }>
}

export const attendanceService = {
  async getAttendance(): Promise<AttendanceRecord[]> {
    const response = await api.get<AttendanceRecord[]>('/attendance')
    return response.data
  },
  async getStudentAttendance(studentId: number, courseCode?: string): Promise<AttendanceRecord[]> {
    const response = await api.get<AttendanceRecord[]>(`/attendance/student/${studentId}`, {
      params: courseCode ? { courseCode } : undefined,
    })
    return response.data
  },
  async getStudentSummary(studentId: number): Promise<AttendanceSummary> {
    const response = await api.get<AttendanceSummary>(`/attendance/student/${studentId}/summary`)
    return response.data
  },
  async getAttendanceByDate(date: string): Promise<AttendanceRecord[]> {
    const response = await api.get<AttendanceRecord[]>(`/attendance/date/${date}`)
    return response.data
  },
  async getAttendanceByCourse(courseCode: string): Promise<AttendanceRecord[]> {
    const response = await api.get<AttendanceRecord[]>(`/attendance/course/${encodeURIComponent(courseCode)}`)
    return response.data
  },
  async markAttendance(record: AttendanceWrite): Promise<AttendanceRecord> {
    const response = await api.post<AttendanceRecord>('/attendance', record)
    return response.data
  },
  async markBulkAttendance(records: BulkAttendanceWrite): Promise<AttendanceRecord[]> {
    const response = await api.post<AttendanceRecord[]>('/attendance/bulk', records)
    return response.data
  },
  async updateAttendance(id: number, record: AttendanceWrite): Promise<AttendanceRecord> {
    const response = await api.put<AttendanceRecord>(`/attendance/${id}`, record)
    return response.data
  },
  async deleteAttendance(id: number): Promise<void> {
    await api.delete(`/attendance/${id}`)
  },
}
