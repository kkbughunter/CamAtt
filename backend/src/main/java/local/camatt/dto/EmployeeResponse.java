package local.camatt.dto;
import local.camatt.model.Employee;
public record EmployeeResponse(Long id, String employeeCode, String name, String email, String department, String role, boolean active, boolean faceRegistered, String photoUrl) {
    public static EmployeeResponse from(Employee e) { return new EmployeeResponse(e.getId(), e.getEmployeeCode(), e.getName(), e.getEmail(), e.getDepartment(), e.getRole(), e.isActive(), e.isFaceRegistered(), e.getPhotoUrl()); }
}

