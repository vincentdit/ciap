package tz.go.tirdo.ciap.domain.consultancy;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.ciap.common.dto.ApiResponse;
import tz.go.tirdo.ciap.common.dto.PageResponse;
import tz.go.tirdo.ciap.domain.consultancy.dto.AssignDto;
import tz.go.tirdo.ciap.domain.consultancy.dto.CreateRequestDto;
import tz.go.tirdo.ciap.domain.consultancy.dto.ReassignDto;
import tz.go.tirdo.ciap.domain.consultancy.dto.RequestEventResponse;
import tz.go.tirdo.ciap.domain.consultancy.dto.RequestResponse;
import tz.go.tirdo.ciap.domain.consultancy.dto.ReviewDto;
import tz.go.tirdo.ciap.domain.department.dto.AssignmentResponse;
import tz.go.tirdo.ciap.domain.department.dto.EligibleAssigneeResponse;
import tz.go.tirdo.ciap.domain.department.dto.RouteDepartmentRequest;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/consultancy-requests")
public class ConsultancyRequestController {

    private final ConsultancyRequestService service;
    private final RequestAuditService requestAuditService;

    public ConsultancyRequestController(ConsultancyRequestService service, RequestAuditService requestAuditService) {
        this.service = service;
        this.requestAuditService = requestAuditService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENT','HOD','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<RequestResponse>> submit(@Valid @RequestBody CreateRequestDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Request submitted", service.submit(dto)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT','EXPERT','HOD','MANAGER','ADMIN','FINANCE')")
    public ResponseEntity<ApiResponse<RequestResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.get(id)));
    }

    @GetMapping("/{id}/events")
    @PreAuthorize("hasAnyRole('CLIENT','EXPERT','HOD','MANAGER','ADMIN','FINANCE')")
    public ResponseEntity<ApiResponse<List<RequestEventResponse>>> events(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(requestAuditService.list(id)));
    }

    @GetMapping("/{id}/assignees")
    @PreAuthorize("hasAnyRole('HOD','ADMIN')")
    public ResponseEntity<ApiResponse<List<EligibleAssigneeResponse>>> assignees(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.eligibleAssignees(id)));
    }

    @GetMapping("/{id}/assignments")
    @PreAuthorize("hasAnyRole('EXPERT','HOD','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> assignments(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.assignments(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT','EXPERT','HOD','MANAGER','ADMIN','FINANCE')")
    public ResponseEntity<ApiResponse<PageResponse<RequestResponse>>> list(
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ConsultancyType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "false") boolean overdue,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(
                service.list(status, q, type, from, to, overdue, pageable))));
    }

    // --- workflow transitions ---

    @PostMapping("/{id}/start-review")
    @PreAuthorize("hasAnyRole('HOD','ADMIN')")
    public ResponseEntity<ApiResponse<RequestResponse>> startReview(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.startReview(id)));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('HOD','ADMIN')")
    public ResponseEntity<ApiResponse<RequestResponse>> review(@PathVariable Long id, @Valid @RequestBody ReviewDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(service.review(id, dto)));
    }

    @PostMapping("/{id}/route")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RequestResponse>> route(
            @PathVariable Long id, @Valid @RequestBody RouteDepartmentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Request routed", service.route(id, request.departmentId())));
    }

    @PostMapping("/{id}/await-payment")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','FINANCE')")
    public ResponseEntity<ApiResponse<RequestResponse>> awaitPayment(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.markAwaitingPayment(id)));
    }

    @PostMapping("/{id}/confirm-payment")
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    public ResponseEntity<ApiResponse<RequestResponse>> confirmPayment(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.confirmPayment(id)));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('HOD','ADMIN')")
    public ResponseEntity<ApiResponse<RequestResponse>> assign(@PathVariable Long id, @Valid @RequestBody AssignDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Consultant assigned", service.assign(id, dto)));
    }

    @PostMapping("/{id}/reassign")
    @PreAuthorize("hasAnyRole('HOD','ADMIN')")
    public ResponseEntity<ApiResponse<RequestResponse>> reassign(@PathVariable Long id, @Valid @RequestBody ReassignDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Request reassigned", service.reassign(id, dto)));
    }

    @PostMapping("/{id}/start-execution")
    @PreAuthorize("hasAnyRole('EXPERT','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<RequestResponse>> startExecution(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.startExecution(id)));
    }

    @PostMapping("/{id}/submit-report")
    @PreAuthorize("hasAnyRole('EXPERT','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<RequestResponse>> submitReport(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.submitReport(id)));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<RequestResponse>> close(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Request closed", service.close(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CLIENT','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<RequestResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Request cancelled", service.cancel(id)));
    }
}
