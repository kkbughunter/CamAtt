package local.camatt.service;

import local.camatt.dto.*;
import local.camatt.model.*;
import local.camatt.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class AttendanceService {
    private final AttendanceRepository attendance; private final EmployeeRepository employees;
    private final LocalTime workdayStart; private final long minimumMinutes; private final long cooldownSeconds;
    public AttendanceService(AttendanceRepository attendance, EmployeeRepository employees, @Value("${camatt.attendance.workday-start}") LocalTime start, @Value("${camatt.attendance.minimum-session-minutes}") long min, @Value("${camatt.attendance.recognition-cooldown-seconds}") long cooldown) { this.attendance = attendance; this.employees = employees; workdayStart = start; minimumMinutes = min; cooldownSeconds = cooldown; }

    @Transactional
    public RecognitionResponse recognize(RecognitionRequest request) {
        Employee employee = employees.findById(request.employeeId()).orElseThrow(() -> new NoSuchElementException("Employee not found"));
        if (!employee.isActive()) return new RecognitionResponse("IGNORED", "Employee is inactive", null);
        LocalDateTime now = LocalDateTime.now(); LocalDate today = now.toLocalDate();
        Optional<AttendanceRecord> latest = attendance.findFirstByEmployeeIdAndAttendanceDateOrderByLastSeenDesc(employee.getId(), today);
        if (latest.isPresent() && Duration.between(latest.get().getLastSeen(), now).getSeconds() < cooldownSeconds)
            return new RecognitionResponse("IGNORED", "Duplicate recognition inside cooldown", AttendanceResponse.from(latest.get()));

        Optional<AttendanceRecord> openSession = attendance.findFirstByEmployeeIdAndAttendanceDateAndCheckOutIsNullOrderByCheckInDesc(employee.getId(), today);
        if (openSession.isEmpty()) {
            AttendanceRecord record = new AttendanceRecord(); record.setEmployee(employee); record.setAttendanceDate(today); record.setCheckIn(now); record.setLastSeen(now); record.setConfidence(request.confidence()); record.setImageUrl(publicImageUrl(request.imagePath())); record.setStatus(now.toLocalTime().isAfter(workdayStart) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT);
            record = attendance.save(record); return new RecognitionResponse("CHECK_IN", "Welcome, " + employee.getName(), AttendanceResponse.from(record));
        }
        AttendanceRecord record = openSession.get();
        record.setLastSeen(now);
        if (Duration.between(record.getCheckIn(), now).toMinutes() < minimumMinutes) { attendance.save(record); return new RecognitionResponse("IGNORED", "Minimum session has not elapsed", AttendanceResponse.from(record)); }
        record.setCheckOut(now); record.setStatus(AttendanceStatus.CHECKED_OUT); record.setConfidence(Math.max(record.getConfidence(), request.confidence())); if (request.imagePath() != null) record.setImageUrl(publicImageUrl(request.imagePath()));
        record = attendance.save(record); return new RecognitionResponse("CHECK_OUT", "Goodbye, " + employee.getName(), AttendanceResponse.from(record));
    }
    @Transactional(readOnly = true)
    public List<AttendanceResponse> history(LocalDate date) { return (date == null ? attendance.findTop50ByOrderByCheckInDesc() : attendance.findByAttendanceDateOrderByCheckInDesc(date)).stream().map(AttendanceResponse::from).toList(); }

    @Transactional(readOnly = true)
    public List<DailyAttendanceResponse> daily(LocalDate date) {
        LocalDate selectedDate = date == null ? LocalDate.now() : date;
        Map<Long, List<AttendanceRecord>> byEmployee = new LinkedHashMap<>();
        attendance.findByAttendanceDateOrderByCheckInDesc(selectedDate).forEach(record ->
            byEmployee.computeIfAbsent(record.getEmployee().getId(), ignored -> new ArrayList<>()).add(record));
        return employees.findByActiveTrueOrderByNameAsc().stream()
            .map(employee -> DailyAttendanceResponse.from(employee, selectedDate, byEmployee.getOrDefault(employee.getId(), List.of())))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> employeeSessions(Long employeeId, LocalDate date) {
        if (!employees.existsById(employeeId)) throw new NoSuchElementException("Employee not found");
        return attendance.findByEmployeeIdAndAttendanceDateOrderByCheckInAsc(employeeId, date == null ? LocalDate.now() : date)
            .stream().map(AttendanceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        LocalDate today = LocalDate.now(); long total = employees.countByActiveTrue(); var todayRows = attendance.findByAttendanceDateOrderByCheckInDesc(today);
        Map<Long, List<AttendanceRecord>> dailyByEmployee = todayRows.stream().collect(java.util.stream.Collectors.groupingBy(r -> r.getEmployee().getId()));
        long arrived = dailyByEmployee.size();
        long late = dailyByEmployee.values().stream().filter(rows -> rows.stream().map(AttendanceRecord::getCheckIn).min(Comparator.naturalOrder()).orElseThrow().toLocalTime().isAfter(workdayStart)).count();
        long checkedOut = dailyByEmployee.values().stream().filter(rows -> rows.stream().noneMatch(r -> r.getCheckOut() == null)).count();
        List<DashboardResponse.DaySummary> weekly = new ArrayList<>(); LocalDate monday = today.with(DayOfWeek.MONDAY);
        for (int i = 0; i < 7; i++) { LocalDate day = monday.plusDays(i); weekly.add(new DashboardResponse.DaySummary(day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), attendance.countDistinctEmployeesByAttendanceDate(day), total)); }
        return new DashboardResponse(today, total, arrived, Math.max(0, total - arrived), late, checkedOut, total == 0 ? 0 : Math.round(arrived * 1000.0 / total) / 10.0, todayRows.stream().limit(8).map(AttendanceResponse::from).toList(), weekly);
    }
    private String publicImageUrl(String path) { if (path == null || path.isBlank()) return null; String normalized = path.replace('\\', '/'); int storage = normalized.indexOf("/storage/"); return storage >= 0 ? normalized.substring(storage) : normalized.startsWith("storage/") ? "/" + normalized : null; }
}
