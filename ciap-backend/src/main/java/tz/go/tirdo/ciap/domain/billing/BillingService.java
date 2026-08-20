package tz.go.tirdo.ciap.domain.billing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.ciap.common.exception.BusinessException;
import tz.go.tirdo.ciap.common.exception.ResourceNotFoundException;
import tz.go.tirdo.ciap.domain.billing.dto.PaymentResponse;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyRequest;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyRequestRepository;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyRequestService;
import tz.go.tirdo.ciap.domain.consultancy.RequestStatus;

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

    private final PaymentRepository paymentRepository;
    private final QuotationRepository quotationRepository;
    private final ConsultancyRequestRepository requestRepository;
    private final ConsultancyRequestService requestService;

    public BillingService(PaymentRepository paymentRepository,
                          QuotationRepository quotationRepository,
                          ConsultancyRequestRepository requestRepository,
                          ConsultancyRequestService requestService) {
        this.paymentRepository = paymentRepository;
        this.quotationRepository = quotationRepository;
        this.requestRepository = requestRepository;
        this.requestService = requestService;
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

        if (request.getStatus() != RequestStatus.QUOTED) {
            throw new BusinessException(
                    "Control number can only be issued while request is QUOTED (current: "
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

        // Drive the workflow: QUOTED -> AWAITING_PAYMENT
        requestService.markAwaitingPayment(requestId);

        return PaymentResponse.from(payment);
    }

    /**
     * Process a GePG settlement callback. Marks the payment PAID and moves the
     * request AWAITING_PAYMENT -> PAID.
     */
    @Transactional
    public PaymentResponse handleCallback(String controlNumber, String gepgReference) {
        Payment payment = paymentRepository.findByControlNumber(controlNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Payment (control number)", controlNumber));

        if (payment.getStatus() == PaymentStatus.PAID) {
            return PaymentResponse.from(payment); // idempotent: already settled
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException("Payment is not awaiting settlement (status: " + payment.getStatus() + ")");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setGepgReference(gepgReference);
        payment.setPaidAt(Instant.now());

        requestService.confirmPayment(payment.getRequest().getId());

        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listForRequest(Long requestId) {
        return paymentRepository.findByRequestIdOrderByIdDesc(requestId)
                .stream().map(PaymentResponse::from).toList();
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
