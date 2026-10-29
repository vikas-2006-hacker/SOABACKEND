export interface AcademicInsight {
  category: string
  title: string
  detail: string
  severity: string
}

export interface AcademicAttendanceInsight {
  totalClasses: number
  classesAttended: number
  classesAbsent: number
  classesExcused: number
  percentage: number
  examEligible: boolean
  status: 'EXCELLENT' | 'GOOD' | 'WARNING' | 'CRITICAL' | 'INSUFFICIENT_DATA' | string
  risk: 'LOW' | 'MODERATE' | 'HIGH' | 'UNKNOWN' | string
  requiredThreshold: number
}

export interface AcademicResultSnapshot {
  courseName: string
  courseCode: string
  semester: number
  academicYear: string
  marks: number
  grade: string
  gradePoints: number
  credits: number
  resultStatus: string
}

export interface AcademicPerformanceInsight {
  cumulativeGpa: number | null
  standing: string | null
  resultsCount: number | null
  averageMarks: number | null
  creditsRegistered: number | null
  creditsEarned: number | null
  strongestSubject: AcademicResultSnapshot | null
  focusSubject: AcademicResultSnapshot | null
  latestResult: AcademicResultSnapshot | null
}

export interface AcademicInsightsResponse {
  studentId: number
  studentName: string
  department: string | null
  semester: number
  studentStatus: string
  attendance: AcademicAttendanceInsight | null
  performance: AcademicPerformanceInsight | null
  insights: AcademicInsight[]
  unavailableSources: string[]
}
