package local.camatt.service;

import local.camatt.dto.RecognitionRequest;
import local.camatt.model.Employee;
import local.camatt.model.AttendanceRecord;
import local.camatt.model.AttendanceStatus;
import local.camatt.repository.*;
import org.junit.jupiter.api.Test;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AttendanceServiceTest {
    @Test void firstRecognitionChecksIn() {
        AttendanceRepository attendance = mock(AttendanceRepository.class); EmployeeRepository employees = mock(EmployeeRepository.class);
        Employee e = new Employee(); e.setId(1L); e.setName("Test Employee"); e.setEmployeeCode("EMP-1"); e.setDepartment("Test"); e.setRole("Tester"); e.setEmail("test@example.com"); e.setActive(true);
        when(employees.findById(1L)).thenReturn(Optional.of(e)); when(attendance.findFirstByEmployeeIdAndAttendanceDateOrderByLastSeenDesc(eq(1L), any())).thenReturn(Optional.empty()); when(attendance.findFirstByEmployeeIdAndAttendanceDateAndCheckOutIsNullOrderByCheckInDesc(eq(1L), any())).thenReturn(Optional.empty()); when(attendance.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new AttendanceService(attendance, employees, LocalTime.MAX, 30, 60);
        var result = service.recognize(new RecognitionRequest(1L, 97.2, null));
        assertEquals("CHECK_IN", result.action()); assertNotNull(result.attendance()); assertEquals("Test Employee", result.attendance().employeeName());
    }

    @Test void nextArrivalClosesTheOpenSession() {
        AttendanceRepository attendance = mock(AttendanceRepository.class); EmployeeRepository employees = mock(EmployeeRepository.class);
        Employee e = new Employee(); e.setId(1L); e.setName("Test Employee"); e.setEmployeeCode("EMP-1"); e.setDepartment("Test"); e.setRole("Tester"); e.setEmail("test@example.com"); e.setActive(true);
        AttendanceRecord open = new AttendanceRecord(); open.setEmployee(e); open.setAttendanceDate(LocalDate.now()); open.setCheckIn(LocalDateTime.now().minusMinutes(10)); open.setLastSeen(LocalDateTime.now().minusMinutes(10)); open.setStatus(AttendanceStatus.PRESENT); open.setConfidence(95);
        when(employees.findById(1L)).thenReturn(Optional.of(e));
        when(attendance.findFirstByEmployeeIdAndAttendanceDateOrderByLastSeenDesc(eq(1L), any())).thenReturn(Optional.of(open));
        when(attendance.findFirstByEmployeeIdAndAttendanceDateAndCheckOutIsNullOrderByCheckInDesc(eq(1L), any())).thenReturn(Optional.of(open));
        when(attendance.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new AttendanceService(attendance, employees, LocalTime.MAX, 0, 5);
        var result = service.recognize(new RecognitionRequest(1L, 97.2, null));
        assertEquals("CHECK_OUT", result.action());
        assertNotNull(open.getCheckOut());
        assertEquals(AttendanceStatus.CHECKED_OUT, open.getStatus());
    }

    @Test void arrivalAfterCompletedSessionStartsAnotherSession() {
        AttendanceRepository attendance = mock(AttendanceRepository.class); EmployeeRepository employees = mock(EmployeeRepository.class);
        Employee e = new Employee(); e.setId(1L); e.setName("Test Employee"); e.setEmployeeCode("EMP-1"); e.setDepartment("Test"); e.setRole("Tester"); e.setEmail("test@example.com"); e.setActive(true);
        AttendanceRecord completed = new AttendanceRecord(); completed.setEmployee(e); completed.setAttendanceDate(LocalDate.now()); completed.setCheckIn(LocalDateTime.now().minusHours(2)); completed.setCheckOut(LocalDateTime.now().minusHours(1)); completed.setLastSeen(LocalDateTime.now().minusHours(1)); completed.setStatus(AttendanceStatus.CHECKED_OUT);
        when(employees.findById(1L)).thenReturn(Optional.of(e));
        when(attendance.findFirstByEmployeeIdAndAttendanceDateOrderByLastSeenDesc(eq(1L), any())).thenReturn(Optional.of(completed));
        when(attendance.findFirstByEmployeeIdAndAttendanceDateAndCheckOutIsNullOrderByCheckInDesc(eq(1L), any())).thenReturn(Optional.empty());
        when(attendance.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new AttendanceService(attendance, employees, LocalTime.MAX, 0, 5);
        var result = service.recognize(new RecognitionRequest(1L, 96.5, null));
        assertEquals("CHECK_IN", result.action());
        verify(attendance).save(argThat(record -> record != completed && record.getCheckOut() == null));
    }
}
