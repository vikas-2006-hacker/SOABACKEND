export type StudentStatus = 'ACTIVE' | 'INACTIVE' | 'GRADUATED'

export interface Student {
  id: number
  name: string
  email: string
  age: number
  department: string
  courses: string | null
  contact: string | null
  assignedFacultyEmail?: string | null
  role: string
  semester: number
  academicYear: string
  status: StudentStatus
}

export type CurrentStudent = Pick<Student, 'id' | 'name' | 'email' | 'department' | 'courses' | 'semester' | 'academicYear' | 'status'>

export interface StudentWrite {
  name: string
  email: string
  age: number
  department: string
  courses?: string
  contact?: string
  assignedFacultyEmail?: string
  password?: string
  semester: number
  academicYear?: string
  status: StudentStatus
}

export interface AttendanceRecord {
  id: number
  studentId: number
  courseCode: string
  date: string
  status: string
  remarks: string | null
  markedBy: string | null
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

export interface StudentProfile {
  studentDetails: Student
  attendanceRecords: AttendanceRecord[] | null
  resultRecords: ResultRecord[] | null
  totalAttendanceRecords: number
  totalResultRecords: number
}
