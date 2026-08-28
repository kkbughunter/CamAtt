package local.camatt.repository;
import local.camatt.model.AttendanceRecord;
import local.camatt.model.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface AttendanceRepository extends JpaRepository<AttendanceRecord, Long> {
    Optional<AttendanceRecord> findFirstByEmployeeIdAndAttendanceDateAndCheckOutIsNullOrderByCheckInDesc(Long employeeId, LocalDate date);
    Optional<AttendanceRecord> findFirstByEmployeeIdAndAttendanceDateOrderByLastSeenDesc(Long employeeId, LocalDate date);
    List<AttendanceRecord> findByEmployeeIdAndAttendanceDateOrderByCheckInAsc(Long employeeId, LocalDate date);
    List<AttendanceRecord> findByAttendanceDateOrderByCheckInDesc(LocalDate date);
    List<AttendanceRecord> findTop50ByOrderByCheckInDesc();
    long countByAttendanceDate(LocalDate date);
    long countByAttendanceDateAndStatus(LocalDate date, AttendanceStatus status);

    @Query("select count(distinct r.employee.id) from AttendanceRecord r where r.attendanceDate = :date")
    long countDistinctEmployeesByAttendanceDate(LocalDate date);
}
