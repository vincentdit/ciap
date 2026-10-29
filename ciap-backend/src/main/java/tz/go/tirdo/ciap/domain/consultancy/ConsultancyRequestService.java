package tz.go.tirdo.ciap.domain.consultancy;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.ciap.common.SearchTerms;
import tz.go.tirdo.ciap.common.exception.BusinessException;
import tz.go.tirdo.ciap.common.exception.ResourceNotFoundException;
import tz.go.tirdo.ciap.domain.billing.PaymentRepository;
import tz.go.tirdo.ciap.domain.billing.PaymentStatus;
import tz.go.tirdo.ciap.domain.catalogue.CatalogueService;
import tz.go.tirdo.ciap.domain.catalogue.PriceSnapshot;
import tz.go.tirdo.ciap.domain.client.Client;
import tz.go.tirdo.ciap.domain.client.ClientService;
import tz.go.tirdo.ciap.domain.consultancy.dto.AssignDto;
import tz.go.tirdo.ciap.domain.consultancy.dto.CreateRequestDto;
import tz.go.tirdo.ciap.domain.consultancy.dto.ReassignDto;
import tz.go.tirdo.ciap.domain.consultancy.dto.RequestResponse;
import tz.go.tirdo.ciap.domain.consultancy.dto.ReviewDto;
import tz.go.tirdo.ciap.domain.consultant.Consultant;
import tz.go.tirdo.ciap.domain.consultant.ConsultantService;
import tz.go.tirdo.ciap.domain.department.Department;
import tz.go.tirdo.ciap.domain.department.DepartmentRepository;
import tz.go.tirdo.ciap.domain.department.DepartmentService;
import tz.go.tirdo.ciap.domain.department.RequestAssignment;
import tz.go.tirdo.ciap.domain.department.RequestAssignmentRepository;
import tz.go.tirdo.ciap.domain.department.RoutingStatus;
import tz.go.tirdo.ciap.domain.department.dto.AssignmentResponse;
import tz.go.tirdo.ciap.domain.department.dto.EligibleAssigneeResponse;
import tz.go.tirdo.ciap.domain.notification.NotificationService;
import tz.go.tirdo.ciap.security.AccessGuard;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;


/**
 * Orchestrates the consultancy request workflow and enforces legal state transitions.
 * All public methods return DTOs mapped *inside* the transaction so lazy associations
 * (client, assignedConsultant) load correctly with open-in-view disabled.
 */
@Service
public class ConsultancyRequestService {

    private static final ZoneId DAR = ZoneId.of("Africa/Dar_es_Salaam");

    private final ConsultancyRequestRepository repository;
    private final ClientService clientService;
    private final ConsultantService consultantService;
    private final tz.go.tirdo.ciap.common.sequence.ReferenceNumberService referenceNumbers;
    private final AccessGuard accessGuard;
    private final RequestAuditService requestAuditService;
    private final PaymentRepository paymentRepository;
    private final DepartmentService departmentService;
    private final DepartmentRepository departmentRepository;
    private final RequestAssignmentRepository assignmentRepository;
    private final NotificationService notificationService;
    private final CatalogueService catalogueService;

    public ConsultancyRequestService(ConsultancyRequestRepository repository,
                                     ClientService clientService,
                                     ConsultantService consultantService,
                                     tz.go.tirdo.ciap.common.sequence.ReferenceNumberService referenceNumbers,
                                     AccessGuard accessGuard,
                                     RequestAuditService requestAuditService,
                                     PaymentRepository paymentRepository,
                                     DepartmentService departmentService,
                                     DepartmentRepository departmentRepository,
                                     RequestAssignmentRepository assignmentRepository,
                                     NotificationService notificationService,
                                     CatalogueService catalogueService) {
        this.repository = repository;
        this.clientService = clientService;
        this.consultantService = consultantService;
        this.referenceNumbers = referenceNumbers;
        this.accessGuard = accessGuard;
        this.requestAuditService = requestAuditService;
        this.paymentRepository = paymentRepository;
        this.departmentService = departmentService;
        this.departmentRepository = departmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.notificationService = notificationService;
        this.catalogueService = catalogueService;
    }

    @Transactional
    public RequestResponse submit(CreateRequestDto dto) {
        Long clientId = dto.clientId();
        AccessGuard.Actor actor = accessGuard.current();
        if (actor.kind() == AccessGuard.Kind.CLIENT) {
            clientId = clientService.getByUserEmail(actor.email()).getId();
        }
        PriceSnapshot price = dto.serviceId() == null
                ? null
                : catalogueService.confirm(dto.serviceId(), dto.seenPriceVersionId(), dto.quantity());
        ConsultancyType type = price == null ? dto.consultancyType() : price.consultancyType();
        if (actor.kind() == AccessGuard.Kind.HOD) {
            departmentService.assertHeadCanOpen(type);
        }
        Client client = clientService.getById(clientId);
        ConsultancyRequest request = ConsultancyRequest.builder()
                .referenceNo(generateReference())
                .client(client)
                .consultancyType(type)
                .title(dto.title())
                .description(dto.description())
                .expectedStartDate(dto.expectedStartDate())
                .status(RequestStatus.SUBMITTED)
                .build();
        if (price != null) {
            request.setServiceId(price.serviceId());
            request.setServiceName(price.serviceName());
            request.setPricingMethod(price.pricingMethod());
            request.setSnapshotAmount(price.amount());
            request.setSnapshotCurrency(price.currency());
            request.setPriceVersionId(price.priceVersionId());
            request.setQuotedQuantity(price.quantity());
        }
        departmentService.applyRoute(request);
        ConsultancyRequest saved = repository.save(request);
        requestAuditService.record(saved, null, RequestStatus.SUBMITTED, null);
        departmentService.scheduleRoutingNotice(saved);
        return present(saved);
    }

    @Transactional
    public RequestResponse startReview(Long id) {
        ConsultancyRequest r = getEntity(id);
        assertDepartmentLead(r);
        transition(r, RequestStatus.UNDER_REVIEW);
        return present(r);
    }

    @Transactional
    public RequestResponse review(Long id, ReviewDto dto) {
        ConsultancyRequest r = getEntity(id);
        assertDepartmentLead(r);
        if (!dto.approved() && (dto.notes() == null || dto.notes().isBlank())) {
            throw new BusinessException("A rejection needs a reason");
        }
        r.setReviewNotes(dto.notes());
        String clientReason = dto.approved() ? null : dto.notes().trim();
        transition(r, dto.approved() ? RequestStatus.QUOTED : RequestStatus.REJECTED, clientReason);
        return present(r);
    }

    /** Moves QUOTED to AWAITING_PAYMENT only after a pending control number exists. */
    @Transactional
    public RequestResponse markAwaitingPayment(Long id) {
        ConsultancyRequest r = getEntity(id);
        if (!paymentRepository.existsByRequestIdAndStatus(id, PaymentStatus.PENDING)) {
            throw new BusinessException("Issue a control number before the request can await payment");
        }
        transition(r, RequestStatus.AWAITING_PAYMENT);
        return present(r);
    }

    /** Moves AWAITING_PAYMENT to PAID only after the payment record itself is paid. */
    @Transactional
    public RequestResponse confirmPayment(Long id) {
        ConsultancyRequest r = getEntity(id);
        if (!paymentRepository.existsByRequestIdAndStatus(id, PaymentStatus.PAID)) {
            throw new BusinessException("This request can be marked paid only after Finance records the payment");
        }
        transition(r, RequestStatus.PAID);
        return present(r);
    }

    private static final Set<RequestStatus> OPEN_FOR_ASSIGNMENT = EnumSet.of(
            RequestStatus.SUBMITTED,
            RequestStatus.UNDER_REVIEW,
            RequestStatus.QUOTED,
            RequestStatus.AWAITING_PAYMENT,
            RequestStatus.PAID);

    /**
     * Names a consultant from the request's own department.
     * Allowed from the moment the request is opened, through review and payment.
     * The status becomes ASSIGNED only once the request is paid.
     */
    @Transactional
    public RequestResponse assign(Long id, AssignDto dto) {
        ConsultancyRequest r = getEntity(id);
        if (!OPEN_FOR_ASSIGNMENT.contains(r.getStatus())) {
            throw new BusinessException(
                    "A consultant can be named from the time the request is opened until it is paid");
        }
        Consultant consultant = consultantService.getById(dto.consultantId());
        if (accessGuard.current().kind() != AccessGuard.Kind.ANONYMOUS) {
            assertDepartmentLead(r);
            departmentService.assertEligible(r, consultant);
        }
        boolean samePerson = r.getAssignedConsultant() != null
                && r.getAssignedConsultant().getId().equals(consultant.getId());
        if (r.getStatus() == RequestStatus.PAID) {
            transition(r, RequestStatus.ASSIGNED);
        }
        if (dto.deadline() != null) {
            r.setAssignmentDeadline(dto.deadline());
        }
        if (!samePerson) {
            Consultant previous = r.getAssignedConsultant();
            if (previous != null) {
                consultantService.decrementWorkload(previous.getId());
            }
            r.setAssignedConsultant(consultant);
            consultantService.incrementWorkload(consultant.getId());
            rememberAssignment(r, consultant, previous, trim(dto.note()), dto.deadline(), null);
            notifyAssignee(r, consultant, "Request " + r.getReferenceNo() + " has been assigned to you.");
        }
        return present(r);
    }

    @Transactional
    public RequestResponse reassign(Long id, ReassignDto dto) {
        ConsultancyRequest r = getEntity(id);
        assertDepartmentLead(r);
        if (r.getStatus() != RequestStatus.ASSIGNED && r.getStatus() != RequestStatus.IN_EXECUTION) {
            throw new BusinessException("This request can be reassigned only after it has an assignee");
        }
        if (r.getAssignedConsultant() == null) {
            throw new BusinessException("This request has no assignee to replace");
        }
        Consultant next = consultantService.getById(dto.consultantId());
        if (next.getId().equals(r.getAssignedConsultant().getId())) {
            throw new BusinessException("Choose a different person");
        }
        departmentService.assertEligible(r, next);
        Consultant previous = r.getAssignedConsultant();
        consultantService.decrementWorkload(previous.getId());
        r.setAssignedConsultant(next);
        if (dto.deadline() != null) {
            r.setAssignmentDeadline(dto.deadline());
        }
        consultantService.incrementWorkload(next.getId());
        rememberAssignment(r, next, previous, trim(dto.note()), r.getAssignmentDeadline(), trim(dto.reason()));
        notifyAssignee(r, previous, "Request " + r.getReferenceNo() + " is no longer assigned to you.");
        notifyAssignee(r, next, "Request " + r.getReferenceNo() + " has been assigned to you.");
        notificationService.schedule(clientEmail(r), "Request " + r.getReferenceNo() + " has a new assignee.");
        return present(r);
    }

    @Transactional
    public RequestResponse route(Long id, Long departmentId) {
        if (accessGuard.current().kind() != AccessGuard.Kind.ANONYMOUS && !accessGuard.isAdmin()) {
            throw new BusinessException("Only an administrator can route a request");
        }
        ConsultancyRequest r = getEntity(id);
        if (r.getStatus().isTerminal()) {
            throw new BusinessException("A closed request cannot be routed");
        }
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department", departmentId));
        if (!department.isActive()) {
            throw new BusinessException("This department is not active");
        }
        r.setDepartment(department);
        var hod = department.getHodUser();
        r.setRoutingStatus(hod != null && hod.isEnabled() ? RoutingStatus.ROUTED : RoutingStatus.NO_HOD);
        requestAuditService.record(r, r.getStatus(), r.getStatus(), "Routed to " + department.getName());
        departmentService.scheduleRoutingNotice(r);
        return present(r);
    }

    @Transactional
    public RequestResponse startExecution(Long id) {
        ConsultancyRequest r = getEntity(id);
        accessGuard.assertCanReadRequest(r);
        if (r.getAssignedConsultant() == null) {
            throw new BusinessException("Cannot start execution without an assigned consultant");
        }
        transition(r, RequestStatus.IN_EXECUTION);
        return present(r);
    }

    @Transactional
    public RequestResponse submitReport(Long id) {
        ConsultancyRequest r = getEntity(id);
        accessGuard.assertCanReadRequest(r);
        transition(r, RequestStatus.REPORT_SUBMITTED);
        if (r.getDepartment() != null && r.getDepartment().getHodUser() != null) {
            notificationService.schedule(r.getDepartment().getHodUser().getEmail(),
                    "Request " + r.getReferenceNo() + " was submitted for review.");
        }
        return present(r);
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
        return present(r);
    }

    @Transactional
    public RequestResponse cancel(Long id) {
        ConsultancyRequest r = getEntity(id);
        accessGuard.assertCanReadRequest(r);
        boolean hadConsultant = r.getAssignedConsultant() != null;
        Long consultantId = hadConsultant ? r.getAssignedConsultant().getId() : null;
        transition(r, RequestStatus.CANCELLED);
        if (hadConsultant) {
            consultantService.decrementWorkload(consultantId);
        }
        return present(r);
    }

    @Transactional(readOnly = true)
    public RequestResponse get(Long id) {
        ConsultancyRequest request = getEntity(id);
        accessGuard.assertCanReadRequest(request);
        return present(request);
    }

    @Transactional(readOnly = true)
    public List<EligibleAssigneeResponse> eligibleAssignees(Long id) {
        ConsultancyRequest request = getEntity(id);
        assertDepartmentLead(request);
        return departmentService.eligibleAssignees(request);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> assignments(Long id) {
        ConsultancyRequest request = getEntity(id);
        accessGuard.assertCanReadRequest(request);
        if (accessGuard.current().kind() == AccessGuard.Kind.CLIENT) {
            throw new ResourceNotFoundException("ConsultancyRequest", id);
        }
        return assignmentRepository.findByRequestIdOrderByIdAsc(id).stream()
                .map(AssignmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<RequestResponse> list(RequestStatus status, Pageable pageable) {
        return list(status, null, null, null, null, false, pageable);
    }

    /**
     * Role-scoped page of requests. {@code q} matches reference, title, client,
     * assignee, or department. Dates use Africa/Dar_es_Salaam.
     */
    @Transactional(readOnly = true)
    public Page<RequestResponse> list(RequestStatus status, String q, ConsultancyType type,
                                      LocalDate from, LocalDate to, boolean overdue, Pageable pageable) {
        AccessGuard.Actor actor = accessGuard.current();
        String term = SearchTerms.like(q);
        String email = actor.email() == null ? "" : actor.email().toLowerCase();
        Pageable ordered = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));
        Specification<ConsultancyRequest> spec = (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            Join<ConsultancyRequest, Client> client = root.join("client", JoinType.LEFT);
            Join<ConsultancyRequest, Consultant> consultant = root.join("assignedConsultant", JoinType.LEFT);
            Join<ConsultancyRequest, Department> department = root.join("department", JoinType.LEFT);
            if (actor.kind() == AccessGuard.Kind.CLIENT) {
                parts.add(cb.equal(cb.lower(client.get("user").get("email")), email));
            } else if (actor.kind() == AccessGuard.Kind.EXPERT) {
                parts.add(cb.equal(cb.lower(consultant.get("user").get("email")), email));
            } else if (actor.kind() == AccessGuard.Kind.HOD) {
                parts.add(cb.equal(cb.lower(department.get("hodUser").get("email")), email));
            }
            if (status != null) {
                parts.add(cb.equal(root.get("status"), status));
            }
            if (type != null) {
                parts.add(cb.equal(root.get("consultancyType"), type));
            }
            if (from != null) {
                parts.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay(DAR).toInstant()));
            }
            if (to != null) {
                parts.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay(DAR).toInstant()));
            }
            if (overdue) {
                parts.add(cb.lessThan(root.get("assignmentDeadline"), LocalDate.now(DAR)));
                parts.add(root.get("status").in(RequestStatus.ASSIGNED, RequestStatus.IN_EXECUTION));
            }
            if (term != null) {
                parts.add(cb.or(
                        cb.like(cb.lower(root.get("referenceNo")), term),
                        cb.like(cb.lower(root.get("title")), term),
                        cb.like(cb.lower(client.get("companyName")), term),
                        cb.like(cb.lower(cb.coalesce(consultant.<String>get("fullName"), "")), term),
                        cb.like(cb.lower(cb.coalesce(department.<String>get("name"), "")), term)
                ));
            }
            return parts.isEmpty() ? cb.conjunction() : cb.and(parts.toArray(Predicate[]::new));
        };
        return repository.findAll(spec, ordered).map(this::present);
    }

    // --- internals ---

    private ConsultancyRequest getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ConsultancyRequest", id));
    }

    private RequestResponse present(ConsultancyRequest request) {
        boolean internal = accessGuard.current().kind() != AccessGuard.Kind.CLIENT;
        return RequestResponse.from(request, internal);
    }

    /**
     * Review and assignment belong to the Head of Department for this request,
     * or to an administrator. Unauthenticated service calls keep the previous
     * behaviour so existing pipeline tests still walk the whole status machine.
     */
    private void assertDepartmentLead(ConsultancyRequest request) {
        AccessGuard.Actor actor = accessGuard.current();
        if (actor.kind() == AccessGuard.Kind.ANONYMOUS) {
            return;
        }
        if (accessGuard.isAdmin()) {
            return;
        }
        if (!accessGuard.canReadRequest(request)) {
            throw new ResourceNotFoundException("ConsultancyRequest", request.getId());
        }
        if (actor.kind() == AccessGuard.Kind.HOD && accessGuard.isHodOf(request)) {
            return;
        }
        throw new BusinessException("Only the Head of Department for this request can do that");
    }

    private void rememberAssignment(ConsultancyRequest request, Consultant consultant, Consultant previous,
                                    String note, java.time.LocalDate deadline, String reason) {
        assignmentRepository.save(RequestAssignment.builder()
                .requestId(request.getId())
                .departmentId(request.getDepartment() == null ? null : request.getDepartment().getId())
                .consultant(consultant)
                .assignedBy(actorEmail())
                .note(note)
                .deadline(deadline)
                .previousConsultant(previous)
                .reason(reason)
                .createdAt(Instant.now())
                .build());
    }

    private void notifyAssignee(ConsultancyRequest request, Consultant consultant, String message) {
        if (consultant.getUser() == null) {
            return;
        }
        notificationService.schedule(consultant.getUser().getEmail(), message);
    }

    private String clientEmail(ConsultancyRequest request) {
        return request.getClient() == null ? null : request.getClient().getEmail();
    }

    private String actorEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()
                || "anonymousUser".equals(auth.getName())) {
            return "system";
        }
        String name = auth.getName();
        return name.length() > 150 ? name.substring(0, 150) : name;
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String note = value.trim();
        if (note.isEmpty()) {
            return null;
        }
        return note.length() > 500 ? note.substring(0, 500) : note;
    }

    private void transition(ConsultancyRequest r, RequestStatus target) {
        transition(r, target, null);
    }

    private void transition(ConsultancyRequest r, RequestStatus target, String reason) {
        RequestStatus from = r.getStatus();
        if (!from.canTransitionTo(target)) {
            throw new BusinessException(
                    "Illegal transition: %s -> %s for request %s"
                            .formatted(from, target, r.getReferenceNo()));
        }
        r.setStatus(target);
        requestAuditService.record(r, from, target, reason);
    }

    private String generateReference() {
        return referenceNumbers.formatted("CIAP", "REQUEST");
    }
}
