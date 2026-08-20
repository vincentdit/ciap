package tz.go.tirdo.ciap.domain.consultant;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.ciap.common.dto.ApiResponse;
import tz.go.tirdo.ciap.common.dto.PageResponse;
import tz.go.tirdo.ciap.domain.consultant.dto.ConsultantResponse;
import tz.go.tirdo.ciap.domain.consultant.dto.CreateConsultantRequest;

@RestController
@RequestMapping("/consultants")
public class ConsultantController {

    private final ConsultantService consultantService;

    public ConsultantController(ConsultantService consultantService) {
        this.consultantService = consultantService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<ConsultantResponse>> create(@Valid @RequestBody CreateConsultantRequest req) {
        var c = consultantService.create(req);
        return ResponseEntity.ok(ApiResponse.ok("Consultant created", ConsultantResponse.from(c)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EXPERT')")
    public ResponseEntity<ApiResponse<ConsultantResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(ConsultantResponse.from(consultantService.getById(id))));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<ConsultantResponse>>> list(Pageable pageable) {
        var page = consultantService.list(pageable).map(ConsultantResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }
}
