package local.camatt.dto;

import local.camatt.model.AttendanceRecord;
import local.camatt.model.AttendanceStatus;
import local.camatt.model.Employee;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record DailyAttendanceResponse(
    Long employeeId,
    String employeeName,
    String employeeCode,
    String department,
    LocalDate attendanceDate,
    LocalDateTime firstCheckIn,
    LocalDateTime lastCheckOut,
    AttendanceStatus status,
    int sessionCount,
    boolean openSession
) {
    public static DailyAttendanceResponse from(Employee employee, LocalDate date, List<AttendanceRecord> sessions) {
        if (sessions.isEmpty()) {
            return new DailyAttendanceResponse(employee.getId(), employee.getName(), employee.getEmployeeCode(), employee.getDepartment(), date, null, null, AttendanceStatus.ABSENT, 0, false);
        }
        AttendanceRecord first = sessions.stream().min(Comparator.comparing(AttendanceRecord::getCheckIn)).orElseThrow();
        LocalDateTime lastCheckOut = sessions.stream().map(AttendanceRecord::getCheckOut).filter(value -> value != null).max(Comparator.naturalOrder()).orElse(null);
        AttendanceRecord open = sessions.stream().filter(record -> record.getCheckOut() == null).findFirst().orElse(null);
        return new DailyAttendanceResponse(
            employee.getId(), employee.getName(), employee.getEmployeeCode(), employee.getDepartment(),
            first.getAttendanceDate(), first.getCheckIn(), lastCheckOut,
            open == null ? AttendanceStatus.CHECKED_OUT : open.getStatus(), sessions.size(), open != null
        );
    }
}
