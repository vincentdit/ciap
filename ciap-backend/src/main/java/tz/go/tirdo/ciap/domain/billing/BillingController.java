package tz.go.tirdo.ciap.domain.billing;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.ciap.common.dto.ApiResponse;
import tz.go.tirdo.ciap.common.dto.PageResponse;
import tz.go.tirdo.ciap.common.exception.BusinessException;
import tz.go.tirdo.ciap.domain.billing.dto.FinancePaymentResponse;
import tz.go.tirdo.ciap.domain.billing.dto.FinanceSummary;
import tz.go.tirdo.ciap.domain.billing.dto.GepgCallbackRequest;
import tz.go.tirdo.ciap.domain.billing.dto.PaymentFlagRequest;
import tz.go.tirdo.ciap.domain.billing.dto.PaymentResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
     * Manual finance confirmation of a pending control number. This is not a
     * GePG provider callback. A reason is required, and a repeat of a paid
     * control number does not advance the request again.
     */
    @PostMapping("/gepg-callback")
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> gepgCallback(@Valid @RequestBody GepgCallbackRequest req) {
        if (req.confirmationNote() == null || req.confirmationNote().isBlank()) {
            throw new BusinessException("A reason is required to record this payment");
        }
        return ResponseEntity.ok(ApiResponse.ok("Payment recorded by Finance",
                billingService.handleCallback(req.controlNumber(), req.gepgReference(), req.confirmationNote())));
    }

    @PostMapping("/{id}/flag")
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> flag(@PathVariable Long id,
                                                             @Valid @RequestBody PaymentFlagRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Payment flagged", billingService.flag(id, req.reason())));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<FinancePaymentResponse>>> search(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @RequestParam(required = false, defaultValue = "false") boolean mismatches,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(
                billingService.search(status, startOf(from), startOfNextDay(to), like(q), mismatches, pageable))));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    public ResponseEntity<ApiResponse<FinanceSummary>> summary() {
        return ResponseEntity.ok(ApiResponse.ok(billingService.summary()));
    }

    @GetMapping("/request/{requestId}")
    @PreAuthorize("hasAnyRole('FINANCE','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> listForRequest(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.ok(billingService.listForRequest(requestId)));
    }

    private static Instant startOf(LocalDate day) {
        return day == null ? null : day.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static Instant startOfNextDay(LocalDate day) {
        return day == null ? null : day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static String like(String q) {
        if (q == null || q.isBlank()) return null;
        String cleaned = q.trim().toLowerCase().replace("%", "").replace("_", "");
        return cleaned.isEmpty() ? null : "%" + cleaned + "%";
    }
}
