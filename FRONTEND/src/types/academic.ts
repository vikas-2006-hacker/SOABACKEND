export type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'LATE' | 'EXCUSED'

export interface AttendanceRecord {
  id: number
  studentId: number
  courseCode: string
  date: string
  status: AttendanceStatus
  remarks: string | null
  markedBy: string | null
}

export interface AttendanceWrite {
  studentId: number
  courseCode: string
  date: string
  status: AttendanceStatus
  remarks?: string
  markedBy?: string
}

export interface ResultRecord {
  id: number
  studentId: number
  semester: number
  academicYear: string
  courseCode: string
  courseName: string
  credits: number
  internalMarks: number
  externalMarks: number
  totalMarks: number
  grade: string
  gradePoints: number
  resultStatus: string
}

export interface ResultWrite {
  studentId: number
  semester: number
  academicYear: string
  courseCode: string
  courseName: string
  credits: number
  internalMarks: number
  externalMarks: number
}

export interface AcademicCourse {
  id: number
  courseCode: string
  courseName: string
  department: string
  semester: number
  academicYear: string
  credits: number
  description?: string | null
  active: boolean
  createdAt?: string
  updatedAt?: string
}

export interface FacultyCourseAssignment {
  id: number
  facultyEmail: string
  courseId: number
  academicYear: string
  semester: number
  active: boolean
  createdAt?: string
  updatedAt?: string
}

export type EnrollmentStatus = 'ACTIVE' | 'DROPPED' | 'COMPLETED'

export interface StudentCourseEnrollment {
  id: number
  studentId: number
  courseId: number
  facultyEmail: string
  academicYear: string
  semester: number
  status: EnrollmentStatus
  enrolledAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface AcademicCourseEnrollmentInput {
  courseId: number
  facultyAssignmentId: number
}

export interface CourseOfferingFaculty {
  assignmentId: number
  name: string
  email: string
  department?: string | null
  designation?: string | null
  semester: number
  academicYear: string
}

export interface CourseOffering extends AcademicCourse {
  faculty: CourseOfferingFaculty[]
  enrollmentStatus: EnrollmentStatus | null
}

export interface StudentCourseEnrollmentView {
  id: number
  courseId: number
  courseName: string
  courseCode: string
  department: string
  credits: number
  courseSemester: number
  courseAcademicYear: string
  description?: string | null
  facultyName: string
  facultyEmail: string
  academicYear: string
  semester: number
  status: EnrollmentStatus
  enrolledAt?: string
}
