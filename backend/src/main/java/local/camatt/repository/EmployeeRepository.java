package local.camatt.repository;
import local.camatt.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    boolean existsByEmployeeCodeIgnoreCase(String code);
    long countByActiveTrue();
    List<Employee> findByActiveTrueOrderByNameAsc();
}
