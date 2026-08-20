package tz.go.tirdo.ciap.domain.consultancy;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.ciap.common.exception.BusinessException;
import tz.go.tirdo.ciap.common.exception.ResourceNotFoundException;
import tz.go.tirdo.ciap.domain.client.Client;
import tz.go.tirdo.ciap.domain.client.ClientService;
import tz.go.tirdo.ciap.domain.consultancy.dto.AssignDto;
import tz.go.tirdo.ciap.domain.consultancy.dto.CreateRequestDto;
import tz.go.tirdo.ciap.domain.consultancy.dto.RequestResponse;
import tz.go.tirdo.ciap.domain.consultancy.dto.ReviewDto;
import tz.go.tirdo.ciap.domain.consultant.Consultant;
import tz.go.tirdo.ciap.domain.consultant.ConsultantService;


/**
 * Orchestrates the consultancy request workflow and enforces legal state transitions.
 * All public methods return DTOs mapped *inside* the transaction so lazy associations
 * (client, assignedConsultant) load correctly with open-in-view disabled.
 */
@Service
public class ConsultancyRequestService {

    private final ConsultancyRequestRepository repository;
    private final ClientService clientService;
    private final ConsultantService consultantService;
    private final tz.go.tirdo.ciap.common.sequence.ReferenceNumberService referenceNumbers;

    public ConsultancyRequestService(ConsultancyRequestRepository repository,
                                     ClientService clientService,
                                     ConsultantService consultantService,
                                     tz.go.tirdo.ciap.common.sequence.ReferenceNumberService referenceNumbers) {
        this.repository = repository;
        this.clientService = clientService;
        this.consultantService = consultantService;
        this.referenceNumbers = referenceNumbers;
    }

    @Transactional
    public RequestResponse submit(CreateRequestDto dto) {
        Client client = clientService.getById(dto.clientId());
        ConsultancyRequest request = ConsultancyRequest.builder()
                .referenceNo(generateReference())
                .client(client)
                .consultancyType(dto.consultancyType())
                .title(dto.title())
                .description(dto.description())
                .expectedStartDate(dto.expectedStartDate())
                .status(RequestStatus.SUBMITTED)
                .build();
        return RequestResponse.from(repository.save(request));
    }

    @Transactional
    public RequestResponse startReview(Long id) {
        ConsultancyRequest r = getEntity(id);
        transition(r, RequestStatus.UNDER_REVIEW);
        return RequestResponse.from(r);
    }

    @Transactional
    public RequestResponse review(Long id, ReviewDto dto) {
        ConsultancyRequest r = getEntity(id);
        r.setReviewNotes(dto.notes());
        transition(r, dto.approved() ? RequestStatus.QUOTED : RequestStatus.REJECTED);
        return RequestResponse.from(r);
    }

    /** A quotation has been issued and sent to the client (Quotation -> Payment step). */
    @Transactional
    public RequestResponse markAwaitingPayment(Long id) {
        ConsultancyRequest r = getEntity(id);
        transition(r, RequestStatus.AWAITING_PAYMENT);
        return RequestResponse.from(r);
    }

    /** Payment confirmed (e.g. via GePG callback in a later module). */
    @Transactional
    public RequestResponse confirmPayment(Long id) {
        ConsultancyRequest r = getEntity(id);
        transition(r, RequestStatus.PAID);
        return RequestResponse.from(r);
    }

    @Transactional
    public RequestResponse assign(Long id, AssignDto dto) {
        ConsultancyRequest r = getEntity(id);
        Consultant consultant = consultantService.getById(dto.consultantId());
        transition(r, RequestStatus.ASSIGNED);
        r.setAssignedConsultant(consultant);
        consultantService.incrementWorkload(consultant.getId());
        return RequestResponse.from(r);
    }

    @Transactional
    public RequestResponse startExecution(Long id) {
        ConsultancyRequest r = getEntity(id);
        if (r.getAssignedConsultant() == null) {
            throw new BusinessException("Cannot start execution without an assigned consultant");
        }
        transition(r, RequestStatus.IN_EXECUTION);
        return RequestResponse.from(r);
    }

    @Transactional
    public RequestResponse submitReport(Long id) {
        ConsultancyRequest r = getEntity(id);
        transition(r, RequestStatus.REPORT_SUBMITTED);
        return RequestResponse.from(r);
    }

    @Transactional
    public RequestResponse close(Long id) {
        ConsultancyRequest r = getEntity(id);
        boolean hadConsultant = r.getAssignedConsultant() != null;
        Long consultantId = hadConsultant ? r.getAssignedConsultant().getId() : null;
        transition(r, RequestStatus.CLOSED);
        if (hadConsultant) {
            consultantService.decrementWorkload(consultantId);
        }
        return RequestResponse.from(r);
    }

    @Transactional
    public RequestResponse cancel(Long id) {
        ConsultancyRequest r = getEntity(id);
        boolean hadConsultant = r.getAssignedConsultant() != null;
        Long consultantId = hadConsultant ? r.getAssignedConsultant().getId() : null;
        transition(r, RequestStatus.CANCELLED);
        if (hadConsultant) {
            consultantService.decrementWorkload(consultantId);
        }
        return RequestResponse.from(r);
    }

    @Transactional(readOnly = true)
    public RequestResponse get(Long id) {
        return RequestResponse.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<RequestResponse> list(RequestStatus status, Pageable pageable) {
        Page<ConsultancyRequest> page = (status == null)
                ? repository.findAll(pageable)
                : repository.findByStatus(status, pageable);
        return page.map(RequestResponse::from);
    }

    // --- internals ---

    private ConsultancyRequest getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ConsultancyRequest", id));
    }

    private void transition(ConsultancyRequest r, RequestStatus target) {
        if (!r.getStatus().canTransitionTo(target)) {
            throw new BusinessException(
                    "Illegal transition: %s -> %s for request %s"
                            .formatted(r.getStatus(), target, r.getReferenceNo()));
        }
        r.setStatus(target);
    }

    private String generateReference() {
        return referenceNumbers.formatted("CIAP", "REQUEST");
    }
}
