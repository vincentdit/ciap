package tz.go.tirdo.ciap.domain.department;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.ciap.common.dto.ApiResponse;
import tz.go.tirdo.ciap.domain.department.dto.AddMemberRequest;
import tz.go.tirdo.ciap.domain.department.dto.AppointHodRequest;
import tz.go.tirdo.ciap.domain.department.dto.DepartmentMemberResponse;
import tz.go.tirdo.ciap.domain.department.dto.EligibleAssigneeResponse;
import tz.go.tirdo.ciap.domain.department.dto.DepartmentQueueSummary;
import tz.go.tirdo.ciap.domain.department.dto.DepartmentResponse;

import java.util.List;

@RestController
@RequestMapping("/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HOD','ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(departmentService.list()));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('HOD','ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<DepartmentQueueSummary>> summary() {
        return ResponseEntity.ok(ApiResponse.ok(departmentService.summary()));
    }

    @PutMapping("/{id}/hod")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> appoint(
            @PathVariable Long id, @Valid @RequestBody AppointHodRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Head of Department appointed",
                departmentService.appointHod(id, request.userId())));
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentMemberResponse>> addMember(
            @PathVariable Long id, @Valid @RequestBody AddMemberRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Department member saved",
                departmentService.addMember(id, request.userId(), request.canReceiveWork())));
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('HOD','ADMIN')")
    public ResponseEntity<ApiResponse<List<DepartmentMemberResponse>>> members(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(departmentService.members(id)));
    }

    @GetMapping("/{id}/assignees")
    @PreAuthorize("hasAnyRole('HOD','ADMIN')")
    public ResponseEntity<ApiResponse<List<EligibleAssigneeResponse>>> assignees(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(departmentService.eligibleAssignees(id)));
    }
}
