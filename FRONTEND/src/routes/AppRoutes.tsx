import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { BrowserRouter } from 'react-router-dom'
import { AppShell } from '../components/layout/AppShell'
import { LoginPage } from '../pages/auth/LoginPage'
import { RegisterPage } from '../pages/auth/RegisterPage'
import { ProtectedRoute } from './ProtectedRoute'
import { RoleRoute } from './RoleRoute'
import { useAuth } from '../hooks/useAuth'
import type { UserRole } from '../types/auth'

const DashboardPage = lazy(() => import('../pages/dashboard/DashboardPage').then((module) => ({ default: module.DashboardPage })))
const StudentDetailsPage = lazy(() => import('../pages/students/StudentDetailsPage').then((module) => ({ default: module.StudentDetailsPage })))
const StudentFormPage = lazy(() => import('../pages/students/StudentFormPage').then((module) => ({ default: module.StudentFormPage })))
const StudentsPage = lazy(() => import('../pages/students/StudentsPage').then((module) => ({ default: module.StudentsPage })))
const AttendancePage = lazy(() => import('../pages/attendance/AttendancePage').then((module) => ({ default: module.AttendancePage })))
const ResultsPage = lazy(() => import('../pages/results/ResultsPage').then((module) => ({ default: module.ResultsPage })))
const FacultyPage = lazy(() => import('../pages/faculty/FacultyPage').then((module) => ({ default: module.FacultyPage })))
const AdminCoursesPage = lazy(() => import('../pages/academic/AdminCoursesPage').then((module) => ({ default: module.AdminCoursesPage })))
const CoursesPage = lazy(() => import('../pages/academic/AcademicPages').then((module) => ({ default: module.CoursesPage })))
const MyCoursesPage = lazy(() => import('../pages/academic/AcademicPages').then((module) => ({ default: module.MyCoursesPage })))
const FacultyStudentsPage = lazy(() => import('../pages/academic/AcademicPages').then((module) => ({ default: module.FacultyStudentsPage })))
const FacultyCoursesPage = lazy(() => import('../pages/academic/AcademicPages').then((module) => ({ default: module.FacultyCoursesPage })))
const MyProfilePage = lazy(() => import('../pages/dashboard/StudentPages').then((module) => ({ default: module.MyProfilePage })))
const MyAttendancePage = lazy(() => import('../pages/dashboard/StudentPages').then((module) => ({ default: module.MyAttendancePage })))
const MyResultsPage = lazy(() => import('../pages/dashboard/StudentPages').then((module) => ({ default: module.MyResultsPage })))

function LoginEntry() {
  const { isAuthenticated } = useAuth()
  return isAuthenticated ? <Navigate to="/dashboard" replace /> : <LoginPage />
}

const staffRoles: UserRole[] = ['ADMIN', 'FACULTY']
const studentRoles: UserRole[] = ['STUDENT']
const adminRoles: UserRole[] = ['ADMIN']
const courseUserRoles: UserRole[] = ['STUDENT', 'FACULTY']

function MyCoursesEntry() {
  const { user } = useAuth()
  return user?.role === 'FACULTY' ? <FacultyCoursesPage /> : <MyCoursesPage />
}

export function AppRoutes() {
  return (
    <BrowserRouter>
      <AppRouteContent />
    </BrowserRouter>
  )
}

export function AppRouteContent() {
  return (
      <Suspense fallback={<div className="page-loading" role="status">Loading CampusCore…</div>}>
      <Routes>
        <Route path="/login" element={<LoginEntry />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<AppShell />}>
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route element={<RoleRoute allowedRoles={studentRoles} />}>
              <Route path="/courses" element={<CoursesPage />} />
              <Route path="/my-profile" element={<MyProfilePage />} />
              <Route path="/my-attendance" element={<MyAttendancePage />} />
              <Route path="/my-results" element={<MyResultsPage />} />
            </Route>
            <Route element={<RoleRoute allowedRoles={courseUserRoles} />}>
              <Route path="/my-courses" element={<MyCoursesEntry />} />
            </Route>
            <Route element={<RoleRoute allowedRoles={staffRoles} />}>
              <Route path="/profile" element={<MyProfilePage />} />
              <Route path="/my-students" element={<FacultyStudentsPage />} />
              <Route path="/students" element={<StudentsPage />} />
              <Route path="/students/add" element={<StudentFormPage />} />
              <Route path="/students/:id/edit" element={<StudentFormPage />} />
              <Route path="/students/:id" element={<StudentDetailsPage />} />
              <Route path="/attendance" element={<AttendancePage />} />
              <Route path="/results" element={<ResultsPage />} />
            </Route>
            <Route element={<RoleRoute allowedRoles={adminRoles} unauthorized="forbidden" />}>
              <Route path="/admin/courses" element={<AdminCoursesPage />} />
              <Route path="/faculty" element={<FacultyPage />} />
            </Route>
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Route>
        </Route>
      </Routes>
      </Suspense>
  )
}
