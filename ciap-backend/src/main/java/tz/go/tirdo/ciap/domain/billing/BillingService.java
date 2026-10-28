package tz.go.tirdo.ciap.domain.billing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.ciap.common.exception.BusinessException;
import tz.go.tirdo.ciap.common.exception.ResourceNotFoundException;
import tz.go.tirdo.ciap.domain.billing.dto.FinancePaymentResponse;
import tz.go.tirdo.ciap.domain.billing.dto.FinanceSummary;
import tz.go.tirdo.ciap.domain.billing.dto.PaymentResponse;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyRequest;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyRequestRepository;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyRequestService;
import tz.go.tirdo.ciap.domain.consultancy.RequestStatus;
import tz.go.tirdo.ciap.domain.notification.NotificationService;
import tz.go.tirdo.ciap.security.AccessGuard;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * GePG billing (Modules 7 & 8). Issues a control number from an accepted/sent quotation,
 * and processes the settlement callback. Status transitions are delegated to the
 * consultancy request state machine so it remains the single source of truth.
 */
@Service
public class BillingService {

    private static final List<QuotationStatus> BILLABLE =
            List.of(QuotationStatus.SENT, QuotationStatus.ACCEPTED);

    private static final List<PaymentStatus> OPEN_PAYMENT =
            List.of(PaymentStatus.PENDING, PaymentStatus.PAID);

    private final PaymentRepository paymentRepository;
    private final QuotationRepository quotationRepository;
    private final ConsultancyRequestRepository requestRepository;
    private final ConsultancyRequestService requestService;
    private final AccessGuard accessGuard;
    private final NotificationService notificationService;

    public BillingService(PaymentRepository paymentRepository,
                          QuotationRepository quotationRepository,
                          ConsultancyRequestRepository requestRepository,
                          ConsultancyRequestService requestService,
                          AccessGuard accessGuard,
                          NotificationService notificationService) {
        this.paymentRepository = paymentRepository;
        this.quotationRepository = quotationRepository;
        this.requestRepository = requestRepository;
        this.requestService = requestService;
        this.accessGuard = accessGuard;
        this.notificationService = notificationService;
    }

    /**
     * Generate a GePG control number for a request that has a billable quotation.
     * Moves the request QUOTED -> AWAITING_PAYMENT via the state machine.
     */
    @Transactional
    public PaymentResponse issueControlNumber(Long requestId) {
        ConsultancyRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ConsultancyRequest", requestId));

        Quotation quotation = quotationRepository
                .findFirstByRequestIdAndStatusInOrderByIdDesc(requestId, BILLABLE)
                .orElseThrow(() -> new BusinessException(
                        "No SENT or ACCEPTED quotation exists for request " + request.getReferenceNo()));

        boolean openPayment = paymentRepository.existsByRequestIdAndStatusIn(requestId, OPEN_PAYMENT);
        boolean firstIssue = request.getStatus() == RequestStatus.QUOTED;
        boolean replacement = request.getStatus() == RequestStatus.AWAITING_PAYMENT && !openPayment;
        if (!firstIssue && !replacement) {
            throw new BusinessException(
                    "A control number can be issued for a quoted request, or to replace a failed payment (current: "
                            + request.getStatus() + ")");
        }

        Payment payment = Payment.builder()
                .controlNumber(generateControlNumber())
                .request(request)
                .quotation(quotation)
                .amount(quotation.getTotal())
                .currency(quotation.getCurrency())
                .status(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);

        if (firstIssue) {
            requestService.markAwaitingPayment(requestId);
        }

        return PaymentResponse.from(payment);
    }

    /**
     * Process a GePG settlement callback. Marks the payment PAID and moves the
     * request AWAITING_PAYMENT -> PAID.
     */
    @Transactional
    public PaymentResponse handleCallback(String controlNumber, String gepgReference) {
        return handleCallback(controlNumber, gepgReference, null);
    }

    /**
     * Process a settlement confirmation. A repeat call after PAID returns the
     * same payment and does not change the confirmer or the workflow.
     */
    @Transactional
    public PaymentResponse handleCallback(String controlNumber, String gepgReference, String confirmationNote) {
        Payment payment = paymentRepository.findByControlNumber(controlNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Payment (control number)", controlNumber));

        if (payment.getStatus() == PaymentStatus.PAID) {
            return PaymentResponse.from(payment);
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException("Payment is not awaiting settlement (status: " + payment.getStatus() + ")");
        }
        if (paymentRepository.existsByGepgReferenceIgnoreCaseAndIdNot(gepgReference.trim(), payment.getId())) {
            throw new BusinessException("This transaction reference is already recorded");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setGepgReference(gepgReference.trim());
        payment.setPaidAt(Instant.now());
        payment.setConfirmedBy(currentConfirmer());
        payment.setConfirmationNote(trimNote(confirmationNote));
        paymentRepository.saveAndFlush(payment);

        requestService.confirmPayment(payment.getRequest().getId());

        return PaymentResponse.from(payment);
    }

    /**
     * Finance could not confirm a pending control number. The request stays
     * awaiting payment. The reason is kept on the payment, not in the client history.
     */
    @Transactional
    public PaymentResponse flag(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        accessGuard.assertCanReadRequest(payment.getRequest());
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException("Only a pending payment can be flagged (status: " + payment.getStatus() + ")");
        }
        payment.setStatus(PaymentStatus.FAILED);
        payment.setConfirmedBy(currentConfirmer());
        payment.setConfirmationNote(trimNote(reason));
        if (payment.getConfirmationNote() == null) {
            throw new BusinessException("A reason is required");
        }
        String email = payment.getRequest().getClient() == null ? null : payment.getRequest().getClient().getEmail();
        notificationService.schedule(email,
                "Payment for request " + payment.getRequest().getReferenceNo()
                        + " could not be confirmed. Finance will follow up.");
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listForRequest(Long requestId) {
        ConsultancyRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ConsultancyRequest", requestId));
        accessGuard.assertCanReadRequest(request);
        return paymentRepository.findByRequestIdOrderByIdDesc(requestId)
                .stream().map(PaymentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Page<FinancePaymentResponse> search(PaymentStatus status, Instant from, Instant to, String term, Pageable pageable) {
        return search(status, from, to, term, false, pageable);
    }

    @Transactional(readOnly = true)
    public Page<FinancePaymentResponse> search(PaymentStatus status, Instant from, Instant to,
                                               String term, boolean onlyMismatches, Pageable pageable) {
        return paymentRepository.search(status, from, to, term, onlyMismatches, pageable)
                .map(FinancePaymentResponse::from);
    }

    @Transactional(readOnly = true)
    public FinanceSummary summary() {
        return new FinanceSummary(
                requestRepository.countByStatus(RequestStatus.QUOTED),
                paymentRepository.countByStatus(PaymentStatus.PENDING),
                paymentRepository.countByStatus(PaymentStatus.PAID),
                paymentRepository.countByStatus(PaymentStatus.FAILED),
                paymentRepository.countMismatches());
    }

    private String currentConfirmer() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return null;
        }
        String name = auth.getName();
        if (name == null || name.isBlank() || "anonymousUser".equals(name)) {
            return null;
        }
        return name.length() > 150 ? name.substring(0, 150) : name;
    }

    private String trimNote(String note) {
        if (note == null) return null;
        String trimmed = note.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.length() > 500) {
            throw new BusinessException("Confirmation note is too long");
        }
        return trimmed;
    }

    /** 12-digit numeric control number (GePG-style). Retries on the rare collision. */
    private String generateControlNumber() {
        String cn;
        do {
            long n = ThreadLocalRandom.current().nextLong(100_000_000_000L, 1_000_000_000_000L);
            cn = Long.toString(n);
        } while (paymentRepository.existsByControlNumber(cn));
        return cn;
    }
}
