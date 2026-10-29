import { api } from './api'
import type { ResultRecord, ResultWrite } from '../types/academic'

export const resultService = {
  async getResults(): Promise<ResultRecord[]> {
    const response = await api.get<ResultRecord[]>('/results')
    return response.data
  },
  async getStudentResults(studentId: number): Promise<ResultRecord[]> {
    const response = await api.get<ResultRecord[]>(`/results/student/${studentId}`)
    return response.data
  },
  async getStudentCgpa(studentId: number): Promise<{ cgpa: number }> {
    const response = await api.get<{ cgpa: number }>(`/results/student/${studentId}/cgpa`)
    return response.data
  },
  async getStudentTranscript(studentId: number): Promise<AcademicTranscript> {
    const response = await api.get<AcademicTranscript>(`/results/student/${studentId}/transcript`)
    return response.data
  },
  async recordResult(result: ResultWrite): Promise<ResultRecord> {
    const response = await api.post<ResultRecord>('/results', result)
    return response.data
  },
  async updateResult(id: number, result: ResultWrite): Promise<ResultRecord> {
    const response = await api.put<ResultRecord>(`/results/${id}`, result)
    return response.data
  },
  async deleteResult(id: number): Promise<void> {
    await api.delete(`/results/${id}`)
  },
}

export interface AcademicTranscript {
  totalCreditsRegistered: number
  totalCreditsEarned: number
  cumulativeGPA: number
  overallAcademicStanding: string
  semesterBreakdowns: Array<{ semester: number; academicYear: string; totalCredits: number; creditsEarned: number; semesterGPA: number; semesterStanding: string }>
}
