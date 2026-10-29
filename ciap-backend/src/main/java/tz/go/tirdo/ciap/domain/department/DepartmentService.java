package tz.go.tirdo.ciap.domain.department;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.ciap.common.exception.BusinessException;
import tz.go.tirdo.ciap.common.exception.ResourceNotFoundException;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyRequest;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyRequestRepository;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyType;
import tz.go.tirdo.ciap.domain.consultancy.RequestStatus;
import tz.go.tirdo.ciap.domain.consultant.AvailabilityStatus;
import tz.go.tirdo.ciap.domain.consultant.Consultant;
import tz.go.tirdo.ciap.domain.consultant.ConsultantRepository;
import tz.go.tirdo.ciap.domain.consultant.ExpertCategory;
import tz.go.tirdo.ciap.domain.department.dto.DepartmentMemberResponse;
import tz.go.tirdo.ciap.domain.department.dto.DepartmentQueueSummary;
import tz.go.tirdo.ciap.domain.department.dto.DepartmentResponse;
import tz.go.tirdo.ciap.domain.department.dto.EligibleAssigneeResponse;
import tz.go.tirdo.ciap.domain.notification.NotificationService;
import tz.go.tirdo.ciap.domain.user.RoleName;
import tz.go.tirdo.ciap.domain.user.User;
import tz.go.tirdo.ciap.domain.user.UserRepository;
import tz.go.tirdo.ciap.security.AccessGuard;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DepartmentService {

    /**
     * Configured expert-category map. A scientist joins Industrial Research,
     * which is the laboratory line used for food and materials work.
     */
    private static final Map<ExpertCategory, String> CATEGORY_CODES = new EnumMap<>(ExpertCategory.class);

    static {
        CATEGORY_CODES.put(ExpertCategory.ENGINEER, "ENGINEERING");
        CATEGORY_CODES.put(ExpertCategory.INDUSTRIAL_TECHNOLOGIST, "INDUSTRIAL");
        CATEGORY_CODES.put(ExpertCategory.SCIENTIST, "INDUSTRIAL");
        CATEGORY_CODES.put(ExpertCategory.ICT_SPECIALIST, "ICT");
        CATEGORY_CODES.put(ExpertCategory.ENVIRONMENTAL_EXPERT, "ENVIRONMENTAL");
    }

    private final DepartmentRepository departmentRepository;
    private final DepartmentMemberRepository memberRepository;
    private final ServiceRouteRepository routeRepository;
    private final ConsultancyRequestRepository requestRepository;
    private final ConsultantRepository consultantRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AccessGuard accessGuard;

    public DepartmentService(DepartmentRepository departmentRepository,
                             DepartmentMemberRepository memberRepository,
                             ServiceRouteRepository routeRepository,
                             ConsultancyRequestRepository requestRepository,
                             ConsultantRepository consultantRepository,
                             UserRepository userRepository,
                             NotificationService notificationService,
                             AccessGuard accessGuard) {
        this.departmentRepository = departmentRepository;
        this.memberRepository = memberRepository;
        this.routeRepository = routeRepository;
        this.requestRepository = requestRepository;
        this.consultantRepository = consultantRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.accessGuard = accessGuard;
    }

    /**
     * Service lines this person may browse. Null means every department.
     * A head sees the department they lead. An expert sees the department they belong to.
     */
    @Transactional(readOnly = true)
    public Set<ConsultancyType> visibleServiceLines() {
        AccessGuard.Actor actor = accessGuard.current();
        if (actor.kind() != AccessGuard.Kind.HOD && actor.kind() != AccessGuard.Kind.EXPERT) {
            return null;
        }
        List<Long> departmentIds = actor.kind() == AccessGuard.Kind.HOD
                ? departmentRepository.findByHodUser_EmailIgnoreCase(actor.email())
                    .filter(Department::isActive)
                    .map(department -> List.of(department.getId()))
                    .orElse(List.of())
                : memberRepository.findByUser_EmailIgnoreCaseAndActiveTrue(actor.email()).stream()
                    .map(DepartmentMember::getDepartment)
                    .filter(department -> department != null && department.isActive())
                    .map(Department::getId)
                    .distinct()
                    .toList();
        if (departmentIds.isEmpty()) {
            return Set.of();
        }
        return routeRepository.findByDepartment_IdInAndActiveTrue(departmentIds).stream()
                .map(ServiceRoute::getConsultancyType)
                .collect(Collectors.toSet());
    }

    /**
     * A head may open a request only on the service line that routes to their department.
     * Other callers are unchanged.
     */
    public void assertHeadCanOpen(ConsultancyType type) {
        AccessGuard.Actor actor = accessGuard.current();
        if (actor.kind() != AccessGuard.Kind.HOD) {
            return;
        }
        Department mine = departmentRepository.findByHodUser_EmailIgnoreCase(actor.email())
                .orElseThrow(() -> new BusinessException("You are not the head of a department"));
        ServiceRoute route = routeRepository.findByConsultancyTypeAndActiveTrue(type).orElse(null);
        if (route == null || route.getDepartment() == null
                || !mine.getId().equals(route.getDepartment().getId())) {
            throw new BusinessException("Open a request only for your own department");
        }
    }

    /** Attaches the request from its service line. Does not guess from the title or description. */
    public void applyRoute(ConsultancyRequest request) {
        ServiceRoute route = routeRepository.findByConsultancyTypeAndActiveTrue(request.getConsultancyType())
                .filter(found -> found.getDepartment() != null && found.getDepartment().isActive())
                .orElse(null);
        if (route == null) {
            request.setDepartment(null);
            request.setRoutingStatus(RoutingStatus.UNMAPPED);
            return;
        }
        Department department = route.getDepartment();
        request.setDepartment(department);
        User hod = department.getHodUser();
        request.setRoutingStatus(hod != null && hod.isEnabled()
                ? RoutingStatus.ROUTED
                : RoutingStatus.NO_HOD);
    }

    public void scheduleRoutingNotice(ConsultancyRequest request) {
        if (request.getRoutingStatus() == RoutingStatus.ROUTED
                && request.getDepartment() != null
                && request.getDepartment().getHodUser() != null) {
            notificationService.schedule(
                    request.getDepartment().getHodUser().getEmail(),
                    "Request " + request.getReferenceNo() + " is waiting in "
                            + request.getDepartment().getName() + ".");
            return;
        }
        String message = "Request " + request.getReferenceNo()
                + " needs a department route or a Head of Department.";
        for (String email : userRepository.findEnabledEmailsByRole(RoleName.ADMIN)) {
            notificationService.schedule(email, message);
        }
    }

    public String requireHodEmail(ConsultancyType type) {
        ServiceRoute route = routeRepository.findByConsultancyTypeAndActiveTrue(type)
                .orElseThrow(() -> new BusinessException("No department is configured for " + type));
        User hod = route.getDepartment().getHodUser();
        if (hod == null || !hod.isEnabled()) {
            throw new BusinessException(route.getDepartment().getName() + " has no active Head of Department");
        }
        return hod.getEmail();
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> list() {
        AccessGuard.Actor actor = accessGuard.current();
        if (actor.kind() == AccessGuard.Kind.HOD) {
            return departmentRepository.findByHodUser_EmailIgnoreCase(actor.email())
                    .map(department -> List.of(DepartmentResponse.from(department)))
                    .orElse(List.of());
        }
        return departmentRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(DepartmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartmentQueueSummary summary() {
        AccessGuard.Actor actor = accessGuard.current();
        if (actor.kind() == AccessGuard.Kind.HOD) {
            String email = actor.email();
            return new DepartmentQueueSummary(
                    countForHod(email, RequestStatus.SUBMITTED),
                    countForHod(email, RequestStatus.UNDER_REVIEW),
                    countForHod(email, RequestStatus.PAID),
                    countForHod(email, RequestStatus.ASSIGNED),
                    countForHod(email, RequestStatus.IN_EXECUTION),
                    requestRepository.countOverdueForHod(email, LocalDate.now()),
                    countForHod(email, RequestStatus.REPORT_SUBMITTED),
                    countForHod(email, RequestStatus.REJECTED),
                    0);
        }
        long unrouted = requestRepository.countByRoutingStatus(RoutingStatus.UNMAPPED)
                + requestRepository.countByRoutingStatus(RoutingStatus.NO_HOD);
        return new DepartmentQueueSummary(
                requestRepository.countByStatus(RequestStatus.SUBMITTED),
                requestRepository.countByStatus(RequestStatus.UNDER_REVIEW),
                requestRepository.countByStatus(RequestStatus.PAID),
                requestRepository.countByStatus(RequestStatus.ASSIGNED),
                requestRepository.countByStatus(RequestStatus.IN_EXECUTION),
                requestRepository.countOverdue(LocalDate.now()),
                requestRepository.countByStatus(RequestStatus.REPORT_SUBMITTED),
                requestRepository.countByStatus(RequestStatus.REJECTED),
                unrouted);
    }

    @Transactional
    public DepartmentResponse appointHod(Long departmentId, Long userId) {
        Department department = getActive(departmentId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!user.isEnabled()) {
            throw new BusinessException("The Head of Department account must be active");
        }
        boolean hodRole = user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.HOD);
        if (!hodRole) {
            throw new BusinessException("Choose an account whose role is Head of Department");
        }
        departmentRepository.findByHodUser_Id(user.getId()).ifPresent(other -> {
            if (!other.getId().equals(department.getId())) {
                throw new BusinessException("This person is already Head of another department");
            }
        });
        department.setHodUser(user);
        departmentRepository.save(department);
        requestRepository.markRouted(department.getId());
        return DepartmentResponse.from(department);
    }

    @Transactional
    public DepartmentMemberResponse addMember(Long departmentId, Long userId, Boolean canReceiveWork) {
        Department department = getActive(departmentId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!user.isEnabled()) {
            throw new BusinessException("An inactive account cannot join a department");
        }
        boolean receive = canReceiveWork == null || canReceiveWork;
        DepartmentMember member = memberRepository.findByDepartmentIdAndUserId(department.getId(), user.getId())
                .orElseGet(() -> DepartmentMember.builder().department(department).user(user).build());
        member.setActive(true);
        member.setCanReceiveWork(receive);
        return DepartmentMemberResponse.from(memberRepository.save(member));
    }

    @Transactional(readOnly = true)
    public List<DepartmentMemberResponse> members(Long departmentId) {
        Department department = getActive(departmentId);
        assertCanSeeDepartment(department);
        return memberRepository.findByDepartmentIdAndActiveTrue(department.getId()).stream()
                .map(DepartmentMemberResponse::from)
                .sorted(Comparator.comparing(DepartmentMemberResponse::fullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EligibleAssigneeResponse> eligibleAssignees(Long departmentId) {
        Department department = getActive(departmentId);
        assertCanSeeDepartment(department);
        return eligibleIn(department.getId());
    }

    @Transactional(readOnly = true)
    public List<EligibleAssigneeResponse> eligibleAssignees(ConsultancyRequest request) {
        if (request.getDepartment() == null) {
            return List.of();
        }
        return eligibleIn(request.getDepartment().getId());
    }

    private List<EligibleAssigneeResponse> eligibleIn(Long departmentId) {
        return memberRepository.findByDepartmentIdAndActiveTrue(departmentId).stream()
                .filter(DepartmentMember::isCanReceiveWork)
                .map(DepartmentMember::getUser)
                .filter(user -> user.isEnabled() && hasRole(user, RoleName.EXPERT))
                .map(user -> consultantRepository.findByUser_Id(user.getId()).orElse(null))
                .filter(consultant -> consultant != null && canTakeWork(consultant))
                .map(consultant -> EligibleAssigneeResponse.from(consultant, "EXPERT"))
                .sorted(Comparator.comparing(EligibleAssigneeResponse::fullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public void assertEligible(ConsultancyRequest request, Consultant consultant) {
        if (request.getDepartment() == null) {
            throw new BusinessException("Route this request to a department before it can be assigned");
        }
        if (consultant.getUser() == null || !consultant.getUser().isEnabled()) {
            throw new BusinessException("The selected person is not an active user");
        }
        if (!hasRole(consultant.getUser(), RoleName.EXPERT)) {
            throw new BusinessException("Only an expert in this department can receive the assignment");
        }
        if (!canTakeWork(consultant)) {
            throw new BusinessException("The selected person cannot take new assignments");
        }
        DepartmentMember member = memberRepository
                .findByDepartmentIdAndUserId(request.getDepartment().getId(), consultant.getUser().getId())
                .orElse(null);
        if (member == null || !member.isActive() || !member.isCanReceiveWork()) {
            throw new BusinessException("The selected person is not eligible in this department");
        }
    }

    /** Links existing consultants to the department configured for their category. */
    @Transactional
    public void enrollConsultants() {
        for (Consultant consultant : consultantRepository.findAll()) {
            if (consultant.getUser() == null || !consultant.getUser().isEnabled()) {
                continue;
            }
            String code = CATEGORY_CODES.get(consultant.getCategory());
            if (code == null) {
                continue;
            }
            Department department = departmentRepository.findByCode(code).orElse(null);
            if (department == null) {
                continue;
            }
            if (memberRepository.existsByDepartmentIdAndUserId(department.getId(), consultant.getUser().getId())) {
                continue;
            }
            memberRepository.save(DepartmentMember.builder()
                    .department(department)
                    .user(consultant.getUser())
                    .canReceiveWork(true)
                    .active(true)
                    .build());
        }
    }

    private long countForHod(String email, RequestStatus status) {
        if (email == null) {
            return 0;
        }
        return requestRepository.countByDepartment_HodUser_EmailIgnoreCaseAndStatus(email, status);
    }

    private Department getActive(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));
        if (!department.isActive()) {
            throw new BusinessException("This department is not active");
        }
        return department;
    }

    private void assertCanSeeDepartment(Department department) {
        AccessGuard.Actor actor = accessGuard.current();
        if (actor.kind() == AccessGuard.Kind.ANONYMOUS || accessGuard.isAdmin()) {
            return;
        }
        if (actor.kind() == AccessGuard.Kind.HOD
                && department.getHodUser() != null
                && actor.email() != null
                && actor.email().equalsIgnoreCase(department.getHodUser().getEmail())) {
            return;
        }
        throw new ResourceNotFoundException("Department", department.getId());
    }

    private boolean hasRole(User user, RoleName role) {
        return user.getRoles().stream().anyMatch(found -> found.getName() == role);
    }

    private boolean canTakeWork(Consultant consultant) {
        AvailabilityStatus availability = consultant.getAvailabilityStatus();
        return availability != AvailabilityStatus.ON_LEAVE && availability != AvailabilityStatus.FULLY_BOOKED;
    }
}
