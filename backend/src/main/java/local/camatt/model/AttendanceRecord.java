package local.camatt.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_records", indexes = {
    @Index(name = "idx_attendance_employee_date", columnList = "employee_id,attendance_date"),
    @Index(name = "idx_attendance_date_checkin", columnList = "attendance_date,check_in")
})
public class AttendanceRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "employee_id") private Employee employee;
    @Column(name = "attendance_date", nullable = false) private LocalDate attendanceDate;
    @Column(name = "check_in", nullable = false) private LocalDateTime checkIn;
    @Column(name = "check_out") private LocalDateTime checkOut;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private AttendanceStatus status;
    @Column(nullable = false) private double confidence;
    @Column(name = "image_url") private String imageUrl;
    @Column(name = "last_seen", nullable = false) private LocalDateTime lastSeen;

    public Long getId() { return id; }
    public Employee getEmployee() { return employee; } public void setEmployee(Employee v) { employee = v; }
    public LocalDate getAttendanceDate() { return attendanceDate; } public void setAttendanceDate(LocalDate v) { attendanceDate = v; }
    public LocalDateTime getCheckIn() { return checkIn; } public void setCheckIn(LocalDateTime v) { checkIn = v; }
    public LocalDateTime getCheckOut() { return checkOut; } public void setCheckOut(LocalDateTime v) { checkOut = v; }
    public AttendanceStatus getStatus() { return status; } public void setStatus(AttendanceStatus v) { status = v; }
    public double getConfidence() { return confidence; } public void setConfidence(double v) { confidence = v; }
    public String getImageUrl() { return imageUrl; } public void setImageUrl(String v) { imageUrl = v; }
    public LocalDateTime getLastSeen() { return lastSeen; } public void setLastSeen(LocalDateTime v) { lastSeen = v; }
}
