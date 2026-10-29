import { api } from './api'

export interface Faculty {
  id: number
  name: string
  email: string
  employeeId: string
  department: string
  designation?: string | null
  phone?: string | null
  status: 'ACTIVE' | 'INACTIVE'
  mfaEnabled: boolean
  createdAt: string
  updatedAt: string
}

export interface FacultyWrite {
  name: string
  email: string
  employeeId: string
  department: string
  password?: string
}

export const facultyService = {
  async getMe(): Promise<Faculty> { return (await api.get<Faculty>('/faculty/me')).data },
  async list(): Promise<Faculty[]> { return (await api.get<Faculty[]>('/faculty')).data },
  async create(data: FacultyWrite & { password: string }): Promise<Faculty> { return (await api.post<Faculty>('/faculty', data)).data },
  async update(id: number, data: FacultyWrite): Promise<Faculty> { return (await api.put<Faculty>(`/faculty/${id}`, data)).data },
  async setStatus(id: number, status: Faculty['status']): Promise<Faculty> {
    return (await api.patch<Faculty>(`/faculty/${id}/status`, null, { params: { status } })).data
  },
}
