import { useEffect, useMemo, useRef, useState } from 'react'
import {
  Activity, Bell, CalendarDays, Camera, Check, ChevronDown, CircleUserRound,
  Clock3, Download, History, LayoutDashboard, Menu, Plus, Search, Settings,
  ShieldCheck, Sparkles, Square, Upload, UserRoundCheck, Users, X,
} from 'lucide-react'
import { api, type AttendanceRecord, type CameraStatus, type DailyAttendance, type DashboardData, type Employee } from './api'
import { demoDashboard, demoEmployees, demoRecords } from './demo'

type Page = 'dashboard' | 'employees' | 'history' | 'settings'

const initials = (name: string) => name.split(' ').map((part) => part[0]).join('').slice(0, 2).toUpperCase()
const time = (value?: string) => value ? new Intl.DateTimeFormat('en-IN', { hour: '2-digit', minute: '2-digit' }).format(new Date(value)) : '—'
const date = (value: string) => new Intl.DateTimeFormat('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(value))
const today = () => {
  const now = new Date(); const offset = now.getTimezoneOffset() * 60_000
  return new Date(now.getTime() - offset).toISOString().slice(0, 10)
}

function App() {
  const [page, setPage] = useState<Page>('dashboard')
  const [mobileNav, setMobileNav] = useState(false)
  const [enrolling, setEnrolling] = useState(false)
  const [dashboard, setDashboard] = useState<DashboardData>(demoDashboard)
  const [employees, setEmployees] = useState<Employee[]>(demoEmployees)
  const [records, setRecords] = useState<AttendanceRecord[]>(demoRecords)
  const [dailyRecords, setDailyRecords] = useState<DailyAttendance[]>([])
  const [connected, setConnected] = useState(false)
  const [loading, setLoading] = useState(true)
  const [toast, setToast] = useState('')

  const refresh = async () => {
    try {
      const [dash, people, history, daily] = await Promise.all([api.dashboard(), api.employees(), api.history(), api.dailyAttendance(today())])
      setDashboard(dash); setEmployees(people); setRecords(history); setDailyRecords(daily); setConnected(true)
    } catch {
      setConnected(false)
    } finally { setLoading(false) }
  }

  useEffect(() => { refresh() }, [])
  useEffect(() => {
    if (!toast) return
    const id = window.setTimeout(() => setToast(''), 3200)
    return () => clearTimeout(id)
  }, [toast])

  const navigate = (next: Page) => { setPage(next); setMobileNav(false) }

  return (
    <div className="app-shell">
      <aside className={`sidebar ${mobileNav ? 'open' : ''}`}>
        <div className="brand">
          <div className="brand-mark"><Camera size={21} strokeWidth={2.4} /></div>
          <div><strong>CamAtt</strong><span>Attendance workspace</span></div>
        </div>
        <nav>
          <NavItem active={page === 'dashboard'} icon={<LayoutDashboard />} label="Overview" onClick={() => navigate('dashboard')} />
          <NavItem active={page === 'employees'} icon={<Users />} label="Employees" onClick={() => navigate('employees')} count={employees.length} />
          <NavItem active={page === 'history'} icon={<History />} label="Attendance" onClick={() => navigate('history')} />
          <div className="nav-label">Workspace</div>
          <NavItem active={page === 'settings'} icon={<Settings />} label="Settings" onClick={() => navigate('settings')} />
        </nav>
        <div className="sidebar-footer">
          <div className={`system-state ${connected ? '' : 'demo'}`}><span />{connected ? 'System online' : 'Preview data'}</div>
          <div className="admin-card"><div className="avatar admin">AK</div><div><strong>Admin</strong><span>Local administrator</span></div><ChevronDown size={16} /></div>
        </div>
      </aside>
      {mobileNav && <button className="nav-scrim" aria-label="Close menu" onClick={() => setMobileNav(false)} />}

      <main>
        <header className="topbar">
          <button className="icon-button menu-button" onClick={() => setMobileNav(true)}><Menu size={21} /></button>
          <div className="search"><Search size={18} /><input aria-label="Search" placeholder="Search employees or records..." /></div>
          <div className="top-actions">
            {!connected && !loading && <span className="preview-pill">Preview mode</span>}
            <button className="icon-button" aria-label="Notifications"><Bell size={19} /><i /></button>
            <button className="primary-button" onClick={() => setEnrolling(true)}><Plus size={18} />Add employee</button>
          </div>
        </header>

        <div className="content">
          {page === 'dashboard' && <Dashboard data={dashboard} onViewHistory={() => setPage('history')} />}
          {page === 'employees' && <Employees employees={employees} onAdd={() => setEnrolling(true)} onRefresh={refresh} />}
          {page === 'history' && <Attendance initialRecords={dailyRecords} />}
          {page === 'settings' && <SettingsPage connected={connected} />}
        </div>
      </main>
      {enrolling && <EnrollmentModal onClose={() => setEnrolling(false)} onSaved={() => { setEnrolling(false); setToast('Employee enrolled successfully'); refresh() }} />}
      {toast && <div className="toast"><Check size={17} />{toast}</div>}
    </div>
  )
}

function NavItem({ active, icon, label, count, onClick }: { active: boolean; icon: React.ReactNode; label: string; count?: number; onClick: () => void }) {
  return <button className={`nav-item ${active ? 'active' : ''}`} onClick={onClick}>{icon}<span>{label}</span>{count !== undefined && <em>{count}</em>}</button>
}

function PageIntro({ eyebrow, title, text, action }: { eyebrow: string; title: string; text: string; action?: React.ReactNode }) {
  return <div className="page-intro"><div><span>{eyebrow}</span><h1>{title}</h1><p>{text}</p></div>{action}</div>
}

function Dashboard({ data, onViewHistory }: { data: DashboardData; onViewHistory: () => void }) {
  return <>
    <PageIntro eyebrow={new Intl.DateTimeFormat('en-IN', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date())} title="Good morning, Admin" text="Here’s how your team is showing up today." />
    <section className="stat-grid">
      <Stat icon={<Users />} label="Total employees" value={data.totalEmployees} note="Registered team members" tone="ink" />
      <Stat icon={<UserRoundCheck />} label="Present today" value={data.present} note={`${data.attendanceRate}% attendance rate`} tone="green" />
      <Stat icon={<Clock3 />} label="Late arrivals" value={data.late} note="After 09:15 AM" tone="amber" />
      <Stat icon={<CircleUserRound />} label="Not checked in" value={data.absent} note="Awaiting arrival" tone="rose" />
    </section>
    <section className="dashboard-grid">
      <CameraPanel />
      <WeeklyChart data={data.weekly} rate={data.attendanceRate} />
    </section>
    <section className="card activity-card">
      <div className="card-heading"><div><span className="eyebrow">Live log</span><h2>Recent activity</h2></div><button className="text-button" onClick={onViewHistory}>View all <span>→</span></button></div>
      <RecordTable records={data.recentActivity} compact />
    </section>
  </>
}

function Stat({ icon, label, value, note, tone }: { icon: React.ReactNode; label: string; value: number; note: string; tone: string }) {
  return <article className="stat-card"><div className={`stat-icon ${tone}`}>{icon}</div><div className="stat-copy"><span>{label}</span><strong>{value}</strong><small>{note}</small></div></article>
}

function CameraPanel() {
  const [status, setStatus] = useState<CameraStatus>({ running: false, cameraAvailable: false, aiAvailable: false, message: 'Connecting to camera service…', facesSeen: 0 })
  const [busy, setBusy] = useState(false)
  const poll = async () => { try { setStatus(await api.cameraStatus()) } catch { setStatus(s => ({ ...s, running: false, message: 'Camera service is offline' })) } }
  useEffect(() => { poll(); const id = setInterval(poll, 5000); return () => clearInterval(id) }, [])
  const toggle = async () => { setBusy(true); try { setStatus(await api.setCamera(!status.running)) } catch { setStatus(s => ({ ...s, message: 'Camera service is offline' })) } finally { setBusy(false) } }
  return <article className="camera-card">
    <div className="camera-toolbar"><div><span className={`live-dot ${status.running ? 'on' : ''}`} />{status.running ? 'LIVE CAMERA' : 'CAMERA STANDBY'}</div><span>Built-in webcam</span></div>
    <div className="camera-view">
      {status.running && status.cameraAvailable ? <img src="/ai/camera/stream" alt="Live attendance camera" /> : <div className="camera-placeholder"><div className="face-frame"><Camera size={30} /></div><strong>{status.running ? 'Starting camera…' : 'Ready when you are'}</strong><span>{status.message}</span></div>}
      <div className="scan-line" />
    </div>
    <div className="camera-bottom"><div><strong>Automatic recognition</strong><span>{status.aiAvailable ? 'Face matching is ready' : 'Waiting for AI service'}</span></div><button disabled={busy} className={status.running ? 'stop-button' : 'light-button'} onClick={toggle}>{status.running ? <Square size={15} fill="currentColor" /> : <Camera size={17} />}{status.running ? 'Stop camera' : 'Start camera'}</button></div>
  </article>
}

function WeeklyChart({ data, rate }: { data: DashboardData['weekly']; rate: number }) {
  return <article className="card chart-card"><div className="card-heading"><div><span className="eyebrow">This week</span><h2>Attendance rhythm</h2></div><div className="rate"><strong>{Math.round(rate)}%</strong><span>average</span></div></div><div className="bars">{data.map((d) => { const height = d.total ? Math.round((d.present / d.total) * 100) : 4; return <div className="bar-item" key={d.day}><div className="bar-track"><div className="bar-fill" style={{ height: `${height}%` }}><span>{d.present || ''}</span></div></div><small>{d.day}</small></div> })}</div><div className="chart-note"><Sparkles size={16} />Strongest attendance was on Wednesday</div></article>
}

function Employees({ employees, onAdd, onRefresh }: { employees: Employee[]; onAdd: () => void; onRefresh: () => Promise<void> }) {
  const [query, setQuery] = useState('')
  const [registering, setRegistering] = useState<number | null>(null)
  const filtered = useMemo(() => employees.filter(e => `${e.name} ${e.department} ${e.employeeCode}`.toLowerCase().includes(query.toLowerCase())), [employees, query])
  return <><PageIntro eyebrow="Team directory" title="Employees" text={`${employees.length} people registered for local attendance.`} action={<button className="primary-button" onClick={onAdd}><Plus size={18} />Add employee</button>} />
    <section className="card list-card"><div className="list-toolbar"><div className="search inner"><Search size={17} /><input value={query} onChange={e => setQuery(e.target.value)} placeholder="Find an employee" /></div><button className="filter-button"><ShieldCheck size={17} />All departments<ChevronDown size={15} /></button></div>
      <div className="employee-grid">{filtered.map((employee, index) => <article className="employee-card" key={employee.id}><div className={`avatar avatar-${index % 5}`}>{initials(employee.name)}</div><div className="employee-main"><strong>{employee.name}</strong><span>{employee.role}</span><small>{employee.employeeCode} · {employee.department}</small></div>{employee.faceRegistered ? <span className="face-badge"><i />Face ready</span> : <button className="face-badge pending retry-face" disabled={registering === employee.id} onClick={async () => { setRegistering(employee.id); try { await api.retryFaceRegistration(employee.id); await onRefresh() } finally { setRegistering(null) } }}><i />{registering === employee.id ? 'Registering…' : 'Retry face'}</button>}</article>)}</div>
      {!filtered.length && <div className="empty-state"><Users size={28} /><strong>No employees found</strong><span>Try a different name or department.</span></div>}
    </section></>
}

function Attendance({ initialRecords }: { initialRecords: DailyAttendance[] }) {
  const [query, setQuery] = useState('')
  const [selectedDate, setSelectedDate] = useState(today())
  const [records, setRecords] = useState(initialRecords)
  const [selected, setSelected] = useState<DailyAttendance | null>(null)
  const [sessions, setSessions] = useState<AttendanceRecord[]>([])
  const [detailsLoading, setDetailsLoading] = useState(false)
  useEffect(() => { setRecords(initialRecords) }, [initialRecords])
  useEffect(() => {
    if (selectedDate === today()) { setRecords(initialRecords); return }
    api.dailyAttendance(selectedDate).then(setRecords).catch(() => setRecords([]))
  }, [selectedDate, initialRecords])
  const filtered = records.filter(r => `${r.employeeName} ${r.employeeCode} ${r.department}`.toLowerCase().includes(query.toLowerCase()))
  const openDetails = async (record: DailyAttendance) => {
    setSelected(record); setSessions([]); setDetailsLoading(true)
    try { setSessions(await api.employeeSessions(record.employeeId, selectedDate)) }
    finally { setDetailsLoading(false) }
  }
  const exportCsv = () => {
    const rows = [['Employee', 'Code', 'Department', 'Date', 'First check in', 'Last check out', 'Sessions', 'Status'], ...filtered.map(r => [r.employeeName, r.employeeCode, r.department, r.attendanceDate, r.firstCheckIn || '', r.lastCheckOut || '', String(r.sessionCount), r.status])]
    const blob = new Blob([rows.map(row => row.map(v => `"${v}"`).join(',')).join('\n')], { type: 'text/csv' })
    const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = 'camatt-attendance.csv'; a.click(); URL.revokeObjectURL(a.href)
  }
  return <><PageIntro eyebrow="Attendance records" title="History" text="First arrival, last departure and every session for each employee." action={<button className="secondary-button" onClick={exportCsv}><Download size={17} />Export CSV</button>} />
    <section className="card list-card"><div className="list-toolbar"><div className="search inner"><Search size={17} /><input value={query} onChange={e => setQuery(e.target.value)} placeholder="Search attendance" /></div><label className="date-filter"><CalendarDays size={17} /><input type="date" value={selectedDate} onChange={e => setSelectedDate(e.target.value)} /></label></div>
      <div className="table-wrap"><table><thead><tr><th>Employee</th><th>Department</th><th>First check in</th><th>Last check out</th><th>Sessions</th><th>Status</th></tr></thead><tbody>{filtered.map((record, index) => <tr className="clickable-row" key={`${record.employeeId}-${record.attendanceDate}`} onClick={() => openDetails(record)}><td><div className="person-cell"><div className={`avatar small avatar-${index % 5}`}>{initials(record.employeeName)}</div><div><strong>{record.employeeName}</strong><span>{record.employeeCode}</span></div></div></td><td>{record.department}</td><td>{record.firstCheckIn ? <><strong>{time(record.firstCheckIn)}</strong><span className="cell-date">{date(record.firstCheckIn)}</span></> : '—'}</td><td>{time(record.lastCheckOut)}</td><td><span className="session-count">{record.sessionCount}</span></td><td><span className={`status ${record.status.toLowerCase()}`}><i />{record.status === 'CHECKED_OUT' ? 'Checked out' : record.status[0] + record.status.slice(1).toLowerCase()}</span></td></tr>)}</tbody></table>{!filtered.length && <div className="empty-state"><History size={28} /><strong>No employees found</strong><span>Try another date or search.</span></div>}</div>
    </section>
    {selected && <AttendanceDetails employee={selected} sessions={sessions} loading={detailsLoading} onClose={() => setSelected(null)} />}
  </>
}

function AttendanceDetails({ employee, sessions, loading, onClose }: { employee: DailyAttendance; sessions: AttendanceRecord[]; loading: boolean; onClose: () => void }) {
  return <div className="modal-backdrop" role="presentation" onMouseDown={e => { if (e.target === e.currentTarget) onClose() }}><div className="modal attendance-details" role="dialog" aria-modal="true" aria-label={`${employee.employeeName} attendance details`}><button className="modal-close" onClick={onClose}><X size={20} /></button>
    <div className="modal-head"><span className="eyebrow">{date(`${employee.attendanceDate}T00:00:00`)}</span><h2>{employee.employeeName}</h2><p>{employee.employeeCode} · {employee.department} · {employee.sessionCount} session{employee.sessionCount === 1 ? '' : 's'}</p></div>
    {loading ? <div className="empty-state"><Clock3 size={25} /><strong>Loading sessions…</strong></div> : sessions.length ? <div className="session-list">{sessions.map((session, index) => <article key={session.id} className="session-item"><div className="session-number">{index + 1}</div><div><span>Check in</span><strong>{time(session.checkIn)}</strong></div><div><span>Check out</span><strong>{time(session.checkOut)}</strong></div><div><span>Match</span><strong>{session.confidence.toFixed(1)}%</strong></div><span className={`status ${session.status.toLowerCase()}`}><i />{session.checkOut ? 'Complete' : 'Open'}</span></article>)}</div> : <div className="empty-state"><History size={28} /><strong>No sessions</strong><span>This employee was absent on the selected date.</span></div>}
  </div></div>
}

function RecordTable({ records, compact = false }: { records: AttendanceRecord[]; compact?: boolean }) {
  return <div className="table-wrap"><table><thead><tr><th>Employee</th><th>Department</th><th>Check in</th><th>Check out</th><th>Status</th>{!compact && <th>Match</th>}</tr></thead><tbody>{records.map((record, index) => <tr key={record.id}><td><div className="person-cell"><div className={`avatar small avatar-${index % 5}`}>{initials(record.employeeName)}</div><div><strong>{record.employeeName}</strong><span>{record.employeeCode}</span></div></div></td><td>{record.department}</td><td><strong>{time(record.checkIn)}</strong><span className="cell-date">{date(record.checkIn)}</span></td><td>{time(record.checkOut)}</td><td><span className={`status ${record.status.toLowerCase()}`}><i />{record.status === 'CHECKED_OUT' ? 'Checked out' : record.status[0] + record.status.slice(1).toLowerCase()}</span></td>{!compact && <td><span className="confidence">{record.confidence.toFixed(1)}%</span></td>}</tr>)}</tbody></table>{!records.length && <div className="empty-state"><History size={28} /><strong>No attendance yet</strong><span>Recognitions will appear here.</span></div>}</div>
}

function SettingsPage({ connected }: { connected: boolean }) {
  return <><PageIntro eyebrow="Local workspace" title="Settings" text="Configure the attendance rules for this laptop." /><div className="settings-grid"><section className="card settings-card"><h2>Attendance policy</h2><p>Control when a later recognition becomes a check-out.</p><label>Workday starts<input type="time" defaultValue="09:15" /></label><label>Minimum session<select defaultValue="30"><option value="15">15 minutes</option><option value="30">30 minutes</option><option value="60">1 hour</option></select></label><button className="primary-button">Save policy</button></section><section className="card settings-card"><h2>System health</h2><p>All data remains on this machine.</p><div className="health-row"><span>Spring Boot API</span><strong className={connected ? 'ok' : ''}>{connected ? 'Connected' : 'Offline'}</strong></div><div className="health-row"><span>Storage</span><strong className="ok">Local filesystem</strong></div><div className="health-row"><span>Recognition model</span><strong>InsightFace</strong></div></section></div></>
}

function EnrollmentModal({ onClose, onSaved }: { onClose: () => void; onSaved: () => void }) {
  const [step, setStep] = useState(1)
  const [files, setFiles] = useState<File[]>([])
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [details, setDetails] = useState({ name: '', employeeCode: '', department: '', email: '', role: '' })
  const formRef = useRef<HTMLFormElement>(null)
  const chooseFiles = (list: FileList | null) => setFiles(Array.from(list || []).slice(0, 3))
  const submit = async () => {
    if (files.length < 2) { setError('Please add at least 2 clear face photos.'); return }
    setSaving(true); setError('')
    try {
      const data = new FormData()
      Object.entries(details).forEach(([key, value]) => data.append(key, value))
      files.forEach(file => data.append('photos', file))
      await api.addEmployee(data); onSaved()
    }
    catch (e) { setError(e instanceof Error ? e.message : 'Could not save employee'); setSaving(false) }
  }
  return <div className="modal-backdrop" role="presentation"><div className="modal" role="dialog" aria-modal="true" aria-label="Add employee"><button className="modal-close" onClick={onClose}><X size={20} /></button><div className="modal-head"><span className="eyebrow">Employee enrollment</span><h2>{step === 1 ? 'Add their details' : 'Register their face'}</h2><p>{step === 1 ? 'Basic information for the attendance directory.' : 'Upload 2–3 clear photos from slightly different angles.'}</p><div className="steps"><i className="active">1</i><span /><i className={step === 2 ? 'active' : ''}>2</i></div></div>
    <form ref={formRef} onSubmit={e => e.preventDefault()}>{step === 1 ? <div className="form-grid"><label className="wide">Full name<input name="name" required value={details.name} onChange={e => setDetails({ ...details, name: e.target.value })} placeholder="e.g. Ananya Rao" /></label><label>Employee ID<input name="employeeCode" required value={details.employeeCode} onChange={e => setDetails({ ...details, employeeCode: e.target.value })} placeholder="EMP-048" /></label><label>Department<input name="department" required value={details.department} onChange={e => setDetails({ ...details, department: e.target.value })} placeholder="Engineering" /></label><label className="wide">Work email<input name="email" type="email" required value={details.email} onChange={e => setDetails({ ...details, email: e.target.value })} placeholder="ananya@company.com" /></label><label className="wide">Role<input name="role" required value={details.role} onChange={e => setDetails({ ...details, role: e.target.value })} placeholder="Software Engineer" /></label></div> : <div><label className="upload-zone"><input type="file" accept="image/jpeg,image/png" multiple onChange={e => chooseFiles(e.target.files)} /><Upload size={25} /><strong>Choose 2–3 face photos</strong><span>JPG or PNG · front and slight side angles</span></label>{files.length > 0 && <div className="photo-previews">{files.map((file, i) => <div key={`${file.name}-${i}`}><img src={URL.createObjectURL(file)} alt={`Face photo ${i + 1}`} /><span><Check size={13} />Photo {i + 1}</span></div>)}</div>}</div>}
      {error && <div className="form-error">{error}</div>}<div className="modal-actions">{step === 1 ? <><button type="button" className="secondary-button" onClick={onClose}>Cancel</button><button type="button" className="primary-button" onClick={() => { if (formRef.current?.reportValidity()) setStep(2) }}>Continue <span>→</span></button></> : <><button type="button" className="secondary-button" onClick={() => setStep(1)}>Back</button><button type="button" disabled={saving} className="primary-button" onClick={submit}>{saving ? 'Creating face profile…' : 'Finish enrollment'}</button></>}</div></form></div></div>
}

export default App
