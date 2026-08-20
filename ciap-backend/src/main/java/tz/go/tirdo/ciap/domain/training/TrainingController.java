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

@RestController
@RequestMapping("/training")
public class TrainingController {

    private final TrainingService service;

    public TrainingController(TrainingService service) {
        this.service = service;
    }

    /** Published events — any authenticated user; marks whether the caller is registered. */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EventResponse>>> listPublished(Pageable pageable,
                                                                                  Authentication auth) {
        Page<EventResponse> page = service.listPublished(pageable).map(e ->
                EventResponse.from(e, service.registeredCount(e.getId()),
                        service.isRegistered(e.getId(), auth.getName())));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<EventResponse>>> listAll(Pageable pageable, Authentication auth) {
        Page<EventResponse> page = service.listAll(pageable).map(e ->
                EventResponse.from(e, service.registeredCount(e.getId()),
                        service.isRegistered(e.getId(), auth.getName())));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> get(@PathVariable Long id, Authentication auth) {
        var e = service.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(
                EventResponse.from(e, service.registeredCount(id), service.isRegistered(id, auth.getName()))));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<EventResponse>> create(@Valid @RequestBody CreateEventRequest req,
                                                             Authentication auth) {
        var e = service.create(req);
        return ResponseEntity.ok(ApiResponse.ok("Event created",
                EventResponse.from(e, 0, false)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<EventResponse>> update(@PathVariable Long id,
                                                             @Valid @RequestBody CreateEventRequest req,
                                                             Authentication auth) {
        var e = service.update(id, req);
        return ResponseEntity.ok(ApiResponse.ok("Event updated",
                EventResponse.from(e, service.registeredCount(id), service.isRegistered(id, auth.getName()))));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<EventResponse>> publish(@PathVariable Long id, Authentication auth) {
        var e = service.setPublished(id, true);
        return ResponseEntity.ok(ApiResponse.ok(
                EventResponse.from(e, service.registeredCount(id), service.isRegistered(id, auth.getName()))));
    }

    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<EventResponse>> unpublish(@PathVariable Long id, Authentication auth) {
        var e = service.setPublished(id, false);
        return ResponseEntity.ok(ApiResponse.ok(
                EventResponse.from(e, service.registeredCount(id), service.isRegistered(id, auth.getName()))));
    }

    /** Register the current user for an event. */
    @PostMapping("/{id}/register")
    public ResponseEntity<ApiResponse<Void>> register(@PathVariable Long id, Authentication auth) {
        service.register(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.message("Registered"));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long id, Authentication auth) {
        service.cancelRegistration(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.message("Registration cancelled"));
    }
}
