package local.camatt.dto;
import local.camatt.model.AttendanceRecord;
import local.camatt.model.AttendanceStatus;
import java.time.LocalDateTime;
public record AttendanceResponse(Long id, Long employeeId, String employeeName, String employeeCode, String department, LocalDateTime checkIn, LocalDateTime checkOut, AttendanceStatus status, double confidence, String imageUrl) {
    public static AttendanceResponse from(AttendanceRecord r) { var e = r.getEmployee(); return new AttendanceResponse(r.getId(), e.getId(), e.getName(), e.getEmployeeCode(), e.getDepartment(), r.getCheckIn(), r.getCheckOut(), r.getStatus(), r.getConfidence(), r.getImageUrl()); }
}

