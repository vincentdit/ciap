package tz.go.tirdo.ciap.domain.billing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.ciap.common.exception.BusinessException;
import tz.go.tirdo.ciap.common.exception.ResourceNotFoundException;
import tz.go.tirdo.ciap.domain.billing.dto.ContractResponse;
import tz.go.tirdo.ciap.domain.billing.dto.CreateQuotationRequest;
import tz.go.tirdo.ciap.domain.billing.dto.QuotationItemDto;
import tz.go.tirdo.ciap.domain.billing.dto.QuotationResponse;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyRequest;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyRequestRepository;
import tz.go.tirdo.ciap.domain.consultancy.RequestStatus;
import tz.go.tirdo.ciap.security.AccessGuard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Quotation & Contract management (Module 5). Builds priced quotations with line items,
 * and generates a contract when a quotation is accepted.
 */
@Service
public class QuotationService {

    private static final BigDecimal DEFAULT_VAT = new BigDecimal("0.18");
    private final QuotationRepository quotationRepository;
    private final ContractRepository contractRepository;
    private final ConsultancyRequestRepository requestRepository;
    private final tz.go.tirdo.ciap.common.sequence.ReferenceNumberService referenceNumbers;
    private final AccessGuard accessGuard;

    public QuotationService(QuotationRepository quotationRepository,
                            ContractRepository contractRepository,
                            ConsultancyRequestRepository requestRepository,
                            tz.go.tirdo.ciap.common.sequence.ReferenceNumberService referenceNumbers,
                            AccessGuard accessGuard) {
        this.quotationRepository = quotationRepository;
        this.contractRepository = contractRepository;
        this.requestRepository = requestRepository;
        this.referenceNumbers = referenceNumbers;
        this.accessGuard = accessGuard;
    }

    @Transactional
    public QuotationResponse create(CreateQuotationRequest dto) {
        ConsultancyRequest request = requestRepository.findById(dto.requestId())
                .orElseThrow(() -> new ResourceNotFoundException("ConsultancyRequest", dto.requestId()));

        RequestStatus s = request.getStatus();
        if (s != RequestStatus.UNDER_REVIEW && s != RequestStatus.QUOTED) {
            throw new BusinessException(
                    "A quotation can only be created while the request is UNDER_REVIEW or QUOTED (current: " + s + ")");
        }

        BigDecimal vatRate = dto.vatRate() != null ? dto.vatRate() : DEFAULT_VAT;

        Quotation quotation = Quotation.builder()
                .quotationNo(generateQuotationNo())
                .request(request)
                .status(QuotationStatus.DRAFT)
                .currency("TZS")
                .vatRate(vatRate)
                .validUntil(dto.validUntil())
                .notes(dto.notes())
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        for (QuotationItemDto item : dto.items()) {
            BigDecimal lineTotal = item.quantity().multiply(item.unitPrice()).setScale(2, RoundingMode.HALF_UP);
            quotation.addItem(QuotationItem.builder()
                    .description(item.description())
                    .quantity(item.quantity())
                    .unitPrice(item.unitPrice())
                    .lineTotal(lineTotal)
                    .build());
            subtotal = subtotal.add(lineTotal);
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal vatAmount = subtotal.multiply(vatRate).setScale(2, RoundingMode.HALF_UP);
        quotation.setSubtotal(subtotal);
        quotation.setVatAmount(vatAmount);
        quotation.setTotal(subtotal.add(vatAmount).setScale(2, RoundingMode.HALF_UP));

        return QuotationResponse.from(quotationRepository.save(quotation));
    }

    @Transactional
    public QuotationResponse send(Long quotationId) {
        Quotation q = getEntity(quotationId);
        if (q.getStatus() != QuotationStatus.DRAFT) {
            throw new BusinessException("Only a DRAFT quotation can be sent (current: " + q.getStatus() + ")");
        }
        q.setStatus(QuotationStatus.SENT);
        return QuotationResponse.from(q);
    }

    /** Client accepts the quotation; a DRAFT contract is generated from it. */
    @Transactional
    public ContractResponse accept(Long quotationId) {
        Quotation q = getEntity(quotationId);
        accessGuard.assertCanReadRequest(q.getRequest());
        if (q.getStatus() != QuotationStatus.SENT) {
            throw new BusinessException("Only a SENT quotation can be accepted (current: " + q.getStatus() + ")");
        }
        q.setStatus(QuotationStatus.ACCEPTED);

        Contract existing = contractRepository.findByQuotationId(q.getId()).orElse(null);
        if (existing != null) {
            return ContractResponse.from(existing);
        }

        Contract contract = Contract.builder()
                .contractNo(generateContractNo())
                .request(q.getRequest())
                .quotation(q)
                .status(ContractStatus.DRAFT)
                .contractValue(q.getTotal())
                .currency(q.getCurrency())
                .startDate(q.getRequest().getExpectedStartDate())
                .build();

        return ContractResponse.from(contractRepository.save(contract));
    }

    @Transactional(readOnly = true)
    public QuotationResponse get(Long quotationId) {
        Quotation quotation = getEntity(quotationId);
        accessGuard.assertCanReadRequest(quotation.getRequest());
        return QuotationResponse.from(quotation);
    }

    @Transactional(readOnly = true)
    public List<QuotationResponse> listForRequest(Long requestId) {
        ConsultancyRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ConsultancyRequest", requestId));
        accessGuard.assertCanReadRequest(request);
        return quotationRepository.findByRequestIdOrderByIdDesc(requestId)
                .stream().map(QuotationResponse::from).toList();
    }

    private Quotation getEntity(Long id) {
        return quotationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation", id));
    }

    private String generateQuotationNo() {
        return referenceNumbers.formatted("QTN", "QUOTATION");
    }

    private String generateContractNo() {
        return referenceNumbers.formatted("CTR", "CONTRACT");
    }
}
