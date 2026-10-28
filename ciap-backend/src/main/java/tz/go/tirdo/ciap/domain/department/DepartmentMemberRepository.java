package tz.go.tirdo.ciap.domain.department;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentMemberRepository extends JpaRepository<DepartmentMember, Long> {
    Optional<DepartmentMember> findByDepartmentIdAndUserId(Long departmentId, Long userId);
    boolean existsByDepartmentIdAndUserId(Long departmentId, Long userId);
    List<DepartmentMember> findByDepartmentIdAndActiveTrue(Long departmentId);
}
