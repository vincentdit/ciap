package tz.go.tirdo.ciap.domain.consultancy;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.ciap.common.dto.ApiResponse;
import tz.go.tirdo.ciap.common.dto.PageResponse;
import tz.go.tirdo.ciap.domain.consultancy.dto.AssignDto;
import tz.go.tirdo.ciap.domain.consultancy.dto.CreateRequestDto;
import tz.go.tirdo.ciap.domain.consultancy.dto.RequestResponse;
import tz.go.tirdo.ciap.domain.consultancy.dto.ReviewDto;

@RestController
@RequestMapping("/consultancy-requests")
public class ConsultancyRequestController {

    private final ConsultancyRequestService service;

    public ConsultancyRequestController(ConsultancyRequestService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENT','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<RequestResponse>> submit(@Valid @RequestBody CreateRequestDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Request submitted", service.submit(dto)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT','EXPERT','MANAGER','ADMIN','FINANCE')")
    public ResponseEntity<ApiResponse<RequestResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.get(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT','EXPERT','MANAGER','ADMIN','FINANCE')")
    public ResponseEntity<ApiResponse<PageResponse<RequestResponse>>> list(
            @RequestParam(required = false) RequestStatus status, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(service.list(status, pageable))));
    }

    // --- workflow transitions ---

    @PostMapping("/{id}/start-review")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<RequestResponse>> startReview(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.startReview(id)));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<RequestResponse>> review(@PathVariable Long id, @Valid @RequestBody ReviewDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(service.review(id, dto)));
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
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<RequestResponse>> assign(@PathVariable Long id, @Valid @RequestBody AssignDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Consultant assigned", service.assign(id, dto)));
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
