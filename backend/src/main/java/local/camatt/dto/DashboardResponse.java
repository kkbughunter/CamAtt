package local.camatt.dto;
import java.time.LocalDate;
import java.util.List;
public record DashboardResponse(LocalDate date, long totalEmployees, long present, long absent, long late, long checkedOut, double attendanceRate, List<AttendanceResponse> recentActivity, List<DaySummary> weekly) {
    public record DaySummary(String day, long present, long total) {}
}

