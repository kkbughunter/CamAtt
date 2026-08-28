export type AttendanceStatus = 'ABSENT' | 'PRESENT' | 'LATE' | 'CHECKED_OUT'

export interface Employee {
  id: number
  employeeCode: string
  name: string
  email: string
  department: string
  role: string
  active: boolean
  faceRegistered: boolean
  photoUrl?: string
}

export interface AttendanceRecord {
  id: number
  employeeId: number
  employeeName: string
  employeeCode: string
  department: string
  checkIn: string
  checkOut?: string
  status: AttendanceStatus
  confidence: number
  imageUrl?: string
}

export interface DailyAttendance {
  employeeId: number
  employeeName: string
  employeeCode: string
  department: string
  attendanceDate: string
  firstCheckIn?: string
  lastCheckOut?: string
  status: AttendanceStatus
  sessionCount: number
  openSession: boolean
}

export interface DashboardData {
  date: string
  totalEmployees: number
  present: number
  absent: number
  late: number
  checkedOut: number
  attendanceRate: number
  recentActivity: AttendanceRecord[]
  weekly: { day: string; present: number; total: number }[]
}

export interface CameraStatus {
  running: boolean
  cameraAvailable: boolean
  aiAvailable: boolean
  message: string
  facesSeen: number
  lastRecognition?: { employeeId: number; confidence: number; at: string }
}

const API = import.meta.env.VITE_API_URL || '/api'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API}${path}`, init)
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new Error(body.message || `Request failed (${response.status})`)
  }
  return response.json()
}

export const api = {
  dashboard: () => request<DashboardData>('/dashboard'),
  employees: () => request<Employee[]>('/employees'),
  history: (query = '') => request<AttendanceRecord[]>(`/attendance${query}`),
  dailyAttendance: (date: string) => request<DailyAttendance[]>(`/attendance/daily?date=${encodeURIComponent(date)}`),
  employeeSessions: (employeeId: number, date: string) => request<AttendanceRecord[]>(`/attendance/${employeeId}/sessions?date=${encodeURIComponent(date)}`),
  addEmployee: (data: FormData) => request<Employee>('/employees', { method: 'POST', body: data }),
  retryFaceRegistration: (employeeId: number) => request<Employee>(`/employees/${employeeId}/face-registration`, { method: 'POST' }),
  cameraStatus: async () => {
    const response = await fetch('/ai/status')
    if (!response.ok) throw new Error('Camera service is offline')
    return response.json() as Promise<CameraStatus>
  },
  setCamera: async (running: boolean) => {
    const response = await fetch(`/ai/camera/${running ? 'start' : 'stop'}`, { method: 'POST' })
    if (!response.ok) throw new Error('Could not update camera')
    return response.json() as Promise<CameraStatus>
  },
}
