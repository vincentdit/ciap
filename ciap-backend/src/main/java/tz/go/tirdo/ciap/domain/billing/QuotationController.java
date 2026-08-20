package tz.go.tirdo.ciap.domain.billing;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.ciap.common.dto.ApiResponse;
import tz.go.tirdo.ciap.domain.billing.dto.ContractResponse;
import tz.go.tirdo.ciap.domain.billing.dto.CreateQuotationRequest;
import tz.go.tirdo.ciap.domain.billing.dto.QuotationResponse;

import java.util.List;

@RestController
@RequestMapping("/quotations")
public class QuotationController {

    private final QuotationService quotationService;

    public QuotationController(QuotationService quotationService) {
        this.quotationService = quotationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<QuotationResponse>> create(@Valid @RequestBody CreateQuotationRequest dto) {
        return ResponseEntity.ok(ApiResponse.ok("Quotation created", quotationService.create(dto)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','FINANCE','CLIENT')")
    public ResponseEntity<ApiResponse<QuotationResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(quotationService.get(id)));
    }

    @GetMapping("/request/{requestId}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','FINANCE','CLIENT')")
    public ResponseEntity<ApiResponse<List<QuotationResponse>>> listForRequest(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.ok(quotationService.listForRequest(requestId)));
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<QuotationResponse>> send(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Quotation sent", quotationService.send(id)));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAnyRole('CLIENT','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<ContractResponse>> accept(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Quotation accepted; contract generated", quotationService.accept(id)));
    }
}
