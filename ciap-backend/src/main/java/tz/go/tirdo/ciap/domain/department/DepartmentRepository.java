package tz.go.tirdo.ciap.domain.department;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByCode(String code);
    Optional<Department> findByHodUser_EmailIgnoreCase(String email);
    Optional<Department> findByHodUser_Id(Long userId);
    List<Department> findByActiveTrueOrderByNameAsc();
}
