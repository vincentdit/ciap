package tz.go.tirdo.ciap.domain.training;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.ciap.common.dto.ApiResponse;
import tz.go.tirdo.ciap.common.dto.PageResponse;
import tz.go.tirdo.ciap.domain.training.dto.CreateEventRequest;
import tz.go.tirdo.ciap.domain.training.dto.EventResponse;
import tz.go.tirdo.ciap.domain.training.dto.TrainingPaymentResponse;

@RestController
@RequestMapping("/training")
public class TrainingController {

    private final TrainingService service;

    public TrainingController(TrainingService service) {
        this.service = service;
    }

    /** Published events — any authenticated user; marks whether the caller is registered. */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EventResponse>>> listPublished(
            @RequestParam(required = false) String q, Pageable pageable, Authentication auth) {
        Page<EventResponse> page = service.listPublished(q, pageable)
                .map(e -> service.present(e, auth.getName()));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<EventResponse>>> listAll(
            @RequestParam(required = false) String q, Pageable pageable, Authentication auth) {
        Page<EventResponse> page = service.listAll(q, pageable)
                .map(e -> service.present(e, auth.getName()));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> get(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(service.present(service.getById(id), auth.getName())));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<EventResponse>> create(@Valid @RequestBody CreateEventRequest req,
                                                             Authentication auth) {
        var e = service.create(req);
        return ResponseEntity.ok(ApiResponse.ok("Event created", service.present(e, auth.getName())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<EventResponse>> update(@PathVariable Long id,
                                                             @Valid @RequestBody CreateEventRequest req,
                                                             Authentication auth) {
        var e = service.update(id, req);
        return ResponseEntity.ok(ApiResponse.ok("Event updated", service.present(e, auth.getName())));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<EventResponse>> publish(@PathVariable Long id, Authentication auth) {
        var e = service.setPublished(id, true);
        return ResponseEntity.ok(ApiResponse.ok(service.present(e, auth.getName())));
    }

    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<EventResponse>> unpublish(@PathVariable Long id, Authentication auth) {
        var e = service.setPublished(id, false);
        return ResponseEntity.ok(ApiResponse.ok(service.present(e, auth.getName())));
    }

    @PostMapping("/{id}/register")
    public ResponseEntity<ApiResponse<EventResponse>> register(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Registered", service.register(id, auth.getName())));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long id, Authentication auth) {
        service.cancelRegistration(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.message("Registration cancelled"));
    }

    @GetMapping("/payments")
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    public ResponseEntity<ApiResponse<java.util.List<TrainingPaymentResponse>>> payments() {
        return ResponseEntity.ok(ApiResponse.ok(service.pendingPayments()));
    }

    @PostMapping("/payments/confirm")
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    public ResponseEntity<ApiResponse<TrainingPaymentResponse>> confirmPayment(
            @Valid @RequestBody tz.go.tirdo.ciap.domain.billing.dto.GepgCallbackRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Training payment recorded by Finance",
                service.confirmPayment(req.controlNumber(), req.gepgReference(), req.confirmationNote())));
    }
}
