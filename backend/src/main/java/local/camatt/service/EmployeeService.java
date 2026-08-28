package local.camatt.service;

import local.camatt.dto.EmployeeResponse;
import local.camatt.model.Employee;
import local.camatt.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class EmployeeService {
    private final EmployeeRepository repository; private final StorageService storage; private final AiServiceClient ai;
    public EmployeeService(EmployeeRepository repository, StorageService storage, AiServiceClient ai) { this.repository = repository; this.storage = storage; this.ai = ai; }
    public List<EmployeeResponse> all() { return repository.findAll().stream().map(EmployeeResponse::from).toList(); }
    @Transactional
    public EmployeeResponse create(String employeeCode, String name, String email, String department, String role, List<MultipartFile> photos) {
        if (photos == null || photos.size() < 2 || photos.size() > 3) throw new IllegalArgumentException("Exactly 2 or 3 face photos are required");
        if (repository.existsByEmployeeCodeIgnoreCase(employeeCode.trim())) throw new IllegalArgumentException("Employee ID is already registered");
        Employee employee = new Employee(); employee.setEmployeeCode(employeeCode.trim()); employee.setName(name.trim()); employee.setEmail(email.trim()); employee.setDepartment(department.trim()); employee.setRole(role.trim());
        employee = repository.save(employee);
        var saved = storage.saveEmployeePhotos(employee.getId(), photos);
        employee.setPhotoUrl(saved.getFirst().publicUrl());
        employee.setFaceRegistered(ai.register(employee.getId(), saved));
        return EmployeeResponse.from(repository.save(employee));
    }
    @Transactional
    public EmployeeResponse retryFaceRegistration(Long employeeId) {
        Employee employee = repository.findById(employeeId).orElseThrow(() -> new NoSuchElementException("Employee not found"));
        var photos = storage.employeePhotos(employeeId);
        if (photos.size() < 2) throw new IllegalArgumentException("At least two stored photos are required to register this face");
        if (!ai.register(employeeId, photos)) throw new IllegalStateException("Face service could not register these photos. Check the AI service log and try again");
        employee.setFaceRegistered(true);
        return EmployeeResponse.from(repository.save(employee));
    }
}
