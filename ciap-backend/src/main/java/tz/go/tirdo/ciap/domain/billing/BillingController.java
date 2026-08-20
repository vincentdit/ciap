package tz.go.tirdo.ciap.domain.billing;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.ciap.common.dto.ApiResponse;
import tz.go.tirdo.ciap.domain.billing.dto.GepgCallbackRequest;
import tz.go.tirdo.ciap.domain.billing.dto.PaymentResponse;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    /** Issue a GePG control number for a billable request (moves it to AWAITING_PAYMENT). */
    @PostMapping("/issue/{requestId}")
    @PreAuthorize("hasAnyRole('FINANCE','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> issue(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.ok("Control number issued", billingService.issueControlNumber(requestId)));
    }

    /**
     * GePG settlement callback. In production this is secured by GePG signature
     * verification rather than a user role; for now it is restricted to FINANCE/ADMIN
     * so it can be exercised with a staff token.
     */
    @PostMapping("/gepg-callback")
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> gepgCallback(@Valid @RequestBody GepgCallbackRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Payment confirmed",
                billingService.handleCallback(req.controlNumber(), req.gepgReference())));
    }

    @GetMapping("/request/{requestId}")
    @PreAuthorize("hasAnyRole('FINANCE','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> listForRequest(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.ok(billingService.listForRequest(requestId)));
    }
}
