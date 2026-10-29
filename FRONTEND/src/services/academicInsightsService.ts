import { api } from './api'
import type { AcademicInsightsResponse } from '../types/insights'

export const academicInsightsService = {
  async getMyInsights(): Promise<AcademicInsightsResponse> {
    const response = await api.get<AcademicInsightsResponse>('/insights/me')
    return response.data
  },
}
