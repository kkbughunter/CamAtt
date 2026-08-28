package local.camatt.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "employees", uniqueConstraints = @UniqueConstraint(columnNames = "employee_code"))
public class Employee {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "employee_code", nullable = false, length = 40) private String employeeCode;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String email;
    @Column(nullable = false) private String department;
    @Column(nullable = false) private String role;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "face_registered", nullable = false) private boolean faceRegistered;
    @Column(name = "photo_url") private String photoUrl;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getEmployeeCode() { return employeeCode; } public void setEmployeeCode(String v) { employeeCode = v; }
    public String getName() { return name; } public void setName(String v) { name = v; }
    public String getEmail() { return email; } public void setEmail(String v) { email = v; }
    public String getDepartment() { return department; } public void setDepartment(String v) { department = v; }
    public String getRole() { return role; } public void setRole(String v) { role = v; }
    public boolean isActive() { return active; } public void setActive(boolean v) { active = v; }
    public boolean isFaceRegistered() { return faceRegistered; } public void setFaceRegistered(boolean v) { faceRegistered = v; }
    public String getPhotoUrl() { return photoUrl; } public void setPhotoUrl(String v) { photoUrl = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

