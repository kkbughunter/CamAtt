import type { AttendanceRecord, DashboardData, Employee } from './api'

const now = new Date()
const at = (hours: number, minutes: number) => {
  const d = new Date(now)
  d.setHours(hours, minutes, 0, 0)
  return d.toISOString()
}

export const demoRecords: AttendanceRecord[] = [
  { id: 1, employeeId: 1, employeeName: 'Aarav Mehta', employeeCode: 'EMP-014', department: 'Engineering', checkIn: at(8, 54), status: 'PRESENT', confidence: 97.8 },
  { id: 2, employeeId: 2, employeeName: 'Maya Kapoor', employeeCode: 'EMP-027', department: 'Design', checkIn: at(9, 7), status: 'PRESENT', confidence: 96.4 },
  { id: 3, employeeId: 3, employeeName: 'Rohan Shah', employeeCode: 'EMP-032', department: 'Operations', checkIn: at(9, 22), status: 'LATE', confidence: 94.9 },
  { id: 4, employeeId: 4, employeeName: 'Diya Nair', employeeCode: 'EMP-041', department: 'People', checkIn: at(8, 46), checkOut: at(17, 18), status: 'CHECKED_OUT', confidence: 98.2 },
]

export const demoDashboard: DashboardData = {
  date: now.toISOString().slice(0, 10), totalEmployees: 24, present: 18, absent: 5, late: 1, checkedOut: 1, attendanceRate: 79.2,
  recentActivity: demoRecords,
  weekly: [
    { day: 'Mon', present: 21, total: 24 }, { day: 'Tue', present: 19, total: 24 },
    { day: 'Wed', present: 22, total: 24 }, { day: 'Thu', present: 20, total: 24 },
    { day: 'Fri', present: 19, total: 24 }, { day: 'Sat', present: 0, total: 0 },
    { day: 'Sun', present: 0, total: 0 },
  ],
}

export const demoEmployees: Employee[] = [
  { id: 1, employeeCode: 'EMP-014', name: 'Aarav Mehta', email: 'aarav@company.com', department: 'Engineering', role: 'Senior Engineer', active: true, faceRegistered: true },
  { id: 2, employeeCode: 'EMP-027', name: 'Maya Kapoor', email: 'maya@company.com', department: 'Design', role: 'Product Designer', active: true, faceRegistered: true },
  { id: 3, employeeCode: 'EMP-032', name: 'Rohan Shah', email: 'rohan@company.com', department: 'Operations', role: 'Operations Lead', active: true, faceRegistered: true },
  { id: 4, employeeCode: 'EMP-041', name: 'Diya Nair', email: 'diya@company.com', department: 'People', role: 'People Partner', active: true, faceRegistered: true },
]

