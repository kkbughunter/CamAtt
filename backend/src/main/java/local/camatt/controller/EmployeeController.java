package local.camatt.controller;

import jakarta.validation.constraints.*;
import local.camatt.dto.EmployeeResponse;
import local.camatt.service.EmployeeService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController @RequestMapping("/api/employees")
public class EmployeeController {
    private final EmployeeService service;
    public EmployeeController(EmployeeService service) { this.service = service; }
    @GetMapping public List<EmployeeResponse> all() { return service.all(); }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EmployeeResponse> create(@RequestParam @NotBlank String employeeCode, @RequestParam @NotBlank String name, @RequestParam @Email String email, @RequestParam @NotBlank String department, @RequestParam @NotBlank String role, @RequestPart("photos") List<MultipartFile> photos) { return ResponseEntity.status(HttpStatus.CREATED).body(service.create(employeeCode, name, email, department, role, photos)); }
    @PostMapping("/{employeeId}/face-registration")
    public EmployeeResponse retryFaceRegistration(@PathVariable Long employeeId) { return service.retryFaceRegistration(employeeId); }
}
