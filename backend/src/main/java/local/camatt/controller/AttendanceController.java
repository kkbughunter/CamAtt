package local.camatt.controller;

import jakarta.validation.Valid;
import local.camatt.dto.*;
import local.camatt.service.AttendanceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController @RequestMapping("/api")
public class AttendanceController {
    private final AttendanceService service;
    public AttendanceController(AttendanceService service) { this.service = service; }
    @GetMapping("/attendance") public List<AttendanceResponse> history(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) { return service.history(date); }
    @GetMapping("/attendance/daily") public List<DailyAttendanceResponse> daily(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) { return service.daily(date); }
    @GetMapping("/attendance/{employeeId}/sessions") public List<AttendanceResponse> employeeSessions(@PathVariable Long employeeId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) { return service.employeeSessions(employeeId, date); }
    @PostMapping("/attendance/recognitions") public RecognitionResponse recognition(@Valid @RequestBody RecognitionRequest request) { return service.recognize(request); }
    @GetMapping("/dashboard") public DashboardResponse dashboard() { return service.dashboard(); }
}
