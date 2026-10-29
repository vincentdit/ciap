package tz.go.tirdo.ciap.domain.catalogue;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.ciap.common.exception.BusinessException;
import tz.go.tirdo.ciap.common.exception.DuplicateResourceException;
import tz.go.tirdo.ciap.common.exception.ResourceNotFoundException;
import tz.go.tirdo.ciap.domain.catalogue.dto.CategoryResponse;
import tz.go.tirdo.ciap.domain.catalogue.dto.PriceVersionResponse;
import tz.go.tirdo.ciap.domain.catalogue.dto.SaveCategoryRequest;
import tz.go.tirdo.ciap.domain.catalogue.dto.SaveServiceRequest;
import tz.go.tirdo.ciap.domain.catalogue.dto.ServiceOfferResponse;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyType;
import tz.go.tirdo.ciap.domain.department.DepartmentService;
import tz.go.tirdo.ciap.domain.notification.NotificationService;
import tz.go.tirdo.ciap.domain.user.RoleName;
import tz.go.tirdo.ciap.domain.user.UserRepository;
import tz.go.tirdo.ciap.security.AccessGuard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
public class CatalogueService {

    private final ConsultancyCategoryRepository categoryRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final ServicePriceVersionRepository versionRepository;
    private final AccessGuard accessGuard;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final DepartmentService departmentService;

    public CatalogueService(ConsultancyCategoryRepository categoryRepository,
                            ServiceOfferingRepository serviceRepository,
                            ServicePriceVersionRepository versionRepository,
                            AccessGuard accessGuard,
                            UserRepository userRepository,
                            NotificationService notificationService,
                            DepartmentService departmentService) {
        this.categoryRepository = categoryRepository;
        this.serviceRepository = serviceRepository;
        this.versionRepository = versionRepository;
        this.accessGuard = accessGuard;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.departmentService = departmentService;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories(boolean manage) {
        boolean unrestricted = manage && accessGuard.isAdmin();
        List<ConsultancyCategory> rows = unrestricted
                ? categoryRepository.findAllByOrderByDisplayOrderAscNameAsc()
                : categoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc();
        if (!unrestricted) {
            Set<ConsultancyType> lines = departmentService.visibleServiceLines();
            if (lines != null) {
                rows = rows.stream().filter(row -> lines.contains(row.getConsultancyType())).toList();
            }
        }
        return rows.stream().map(CategoryResponse::from).toList();
    }

    @Transactional
    public CategoryResponse createCategory(SaveCategoryRequest request) {
        String code = normalizeCode(request.code());
        if (categoryRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw new DuplicateResourceException("A category with this code already exists");
        }
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("A category with this name already exists");
        }
        ConsultancyCategory category = categoryRepository.save(ConsultancyCategory.builder()
                .code(code)
                .name(name)
                .description(blankToNull(request.description()))
                .consultancyType(request.consultancyType())
                .displayOrder(request.displayOrder() == null ? 0 : request.displayOrder())
                .active(request.active() == null || request.active())
                .build());
        return CategoryResponse.from(category);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, SaveCategoryRequest request) {
        ConsultancyCategory category = category(id);
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("A category with this name already exists");
        }
        category.setName(name);
        category.setDescription(blankToNull(request.description()));
        if (request.displayOrder() != null) {
            category.setDisplayOrder(request.displayOrder());
        }
        if (request.active() != null) {
            category.setActive(request.active());
        }
        return CategoryResponse.from(category);
    }

    @Transactional(readOnly = true)
    public List<ServiceOfferResponse> listServices(Long categoryId, boolean manage) {
        ConsultancyCategory category = category(categoryId);
        if (!manage || !accessGuard.isAdmin()) {
            assertVisible(category);
        }
        if (!manage || !accessGuard.isAdmin()) {
            if (!category.isActive()) {
                throw new ResourceNotFoundException("Consultancy category", categoryId);
            }
        }
        return serviceRepository.findByCategoryIdOrderByNameAsc(categoryId).stream()
                .filter(service -> manage && accessGuard.isAdmin()
                        || (service.isActive() && service.isPublished() && category.isActive()))
                .map(service -> ServiceOfferResponse.from(service, latestId(service.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceOfferResponse getService(Long id) {
        ServiceOffering service = service(id);
        boolean visible = service.isActive() && service.isPublished() && service.getCategory().isActive();
        if (!visible && !accessGuard.isAdmin()) {
            throw new ResourceNotFoundException("Consultancy service", id);
        }
        if (!accessGuard.isAdmin()) {
            assertVisible(service.getCategory());
        }
        return ServiceOfferResponse.from(service, latestId(id));
    }

    @Transactional
    public ServiceOfferResponse createService(SaveServiceRequest request) {
        ConsultancyCategory category = category(request.categoryId());
        String name = request.name().trim();
        if (serviceRepository.findByCategoryIdAndNameIgnoreCase(category.getId(), name).isPresent()) {
            throw new DuplicateResourceException("This category already has a service with that name");
        }
        Priced priced = priceOf(request, true);
        ServiceOffering service = serviceRepository.save(ServiceOffering.builder()
                .category(category)
                .name(name)
                .description(blankToNull(request.description()))
                .pricingMethod(priced.method())
                .amount(priced.amount())
                .currency(priced.currency())
                .unitLabel(priced.unitLabel())
                .conditions(priced.conditions())
                .published(Boolean.TRUE.equals(request.published()))
                .active(request.active() == null || request.active())
                .build());
        ServicePriceVersion version = writeVersion(service, request.note());
        if (service.isPublished()) {
            notifyAdmins("Consultancy service \"" + service.getName() + "\" is published. "
                    + PriceLabels.customerLabel(service.getPricingMethod(), priced.publicAmount(),
                    service.getCurrency(), service.getUnitLabel()));
        }
        return ServiceOfferResponse.from(service, version.getId());
    }

    @Transactional
    public ServiceOfferResponse updateService(Long id, SaveServiceRequest request) {
        ServiceOffering service = service(id);
        ConsultancyCategory category = category(request.categoryId());
        String name = request.name().trim();
        if (serviceRepository.existsByCategoryIdAndNameIgnoreCaseAndIdNot(category.getId(), name, id)) {
            throw new DuplicateResourceException("This category already has a service with that name");
        }
        Priced priced = priceOf(request, false);
        boolean priceChanged = priceChanged(service, priced);
        if (priceChanged && (request.note() == null || request.note().isBlank())) {
            throw new BusinessException("A note is required when the price changes");
        }
        boolean wasPublished = service.isPublished();
        service.setCategory(category);
        service.setName(name);
        service.setDescription(blankToNull(request.description()));
        service.setPricingMethod(priced.method());
        service.setAmount(priced.amount());
        service.setCurrency(priced.currency());
        service.setUnitLabel(priced.unitLabel());
        service.setConditions(priced.conditions());
        if (request.published() != null) {
            service.setPublished(request.published());
        }
        if (request.active() != null) {
            service.setActive(request.active());
        }
        Long versionId = latestId(id);
        if (priceChanged) {
            versionId = writeVersion(service, request.note()).getId();
            notifyAdmins("The price of \"" + service.getName() + "\" changed. "
                    + PriceLabels.customerLabel(priced.method(), priced.publicAmount(),
                    priced.currency(), priced.unitLabel()));
        }
        if (wasPublished != service.isPublished()) {
            notifyAdmins(service.isPublished()
                    ? "Consultancy service \"" + service.getName() + "\" is published."
                    : "Consultancy service \"" + service.getName() + "\" is no longer published.");
        }
        return ServiceOfferResponse.from(service, versionId);
    }

    @Transactional(readOnly = true)
    public List<PriceVersionResponse> priceHistory(Long serviceId) {
        service(serviceId);
        return versionRepository.findByServiceIdOrderByIdDesc(serviceId).stream()
                .map(PriceVersionResponse::from)
                .toList();
    }

    /**
     * Checks the price the customer saw. A newer fixed, starting, or unit price
     * is refused so the customer can review it. The returned snapshot is what
     * gets stored on the request.
     */
    @Transactional(readOnly = true)
    public PriceSnapshot confirm(Long serviceId, Long seenPriceVersionId, BigDecimal quantity) {
        ServiceOffering service = service(serviceId);
        if (!service.isActive() || !service.isPublished() || !service.getCategory().isActive()) {
            throw new BusinessException("This service is not available");
        }
        ServicePriceVersion latest = versionRepository.findFirstByServiceIdOrderByIdDesc(serviceId)
                .orElseThrow(() -> new BusinessException("This service has no price"));
        if (seenPriceVersionId == null || !seenPriceVersionId.equals(latest.getId())) {
            throw new BusinessException("The price changed. Review it and submit again. "
                    + PriceLabels.customerLabel(latest.getPricingMethod(),
                    latest.getPricingMethod().requiresAmount() ? latest.getAmount() : null,
                    latest.getCurrency(), latest.getUnitLabel()));
        }
        BigDecimal snapshotAmount = null;
        BigDecimal storedQuantity = null;
        if (latest.getPricingMethod() == PricingMethod.VARIABLE) {
            if (quantity == null || quantity.signum() <= 0) {
                throw new BusinessException("Enter how many units you need");
            }
            storedQuantity = quantity.setScale(2, RoundingMode.HALF_UP);
            snapshotAmount = latest.getAmount().multiply(storedQuantity).setScale(2, RoundingMode.HALF_UP);
        } else if (latest.getPricingMethod().requiresAmount()) {
            snapshotAmount = latest.getAmount();
        }
        return new PriceSnapshot(
                service.getId(),
                service.getName(),
                service.getCategory().getConsultancyType(),
                latest.getPricingMethod(),
                snapshotAmount,
                latest.getCurrency(),
                latest.getId(),
                storedQuantity,
                PriceLabels.customerLabel(latest.getPricingMethod(),
                        latest.getPricingMethod() == PricingMethod.VARIABLE ? latest.getAmount() : snapshotAmount,
                        latest.getCurrency(), latest.getUnitLabel()));
    }

    private ServicePriceVersion writeVersion(ServiceOffering service, String note) {
        return versionRepository.save(ServicePriceVersion.builder()
                .service(service)
                .pricingMethod(service.getPricingMethod())
                .amount(service.getAmount())
                .currency(service.getCurrency())
                .unitLabel(service.getUnitLabel())
                .conditions(service.getConditions())
                .note(blankToNull(note))
                .changedBy(actor())
                .changedAt(Instant.now())
                .build());
    }

    private boolean priceChanged(ServiceOffering service, Priced priced) {
        return service.getPricingMethod() != priced.method()
                || !sameMoney(service.getAmount(), priced.amount())
                || !service.getCurrency().equals(priced.currency())
                || !sameText(service.getUnitLabel(), priced.unitLabel())
                || !sameText(service.getConditions(), priced.conditions());
    }

    private Priced priceOf(SaveServiceRequest request, boolean creating) {
        PricingMethod method = request.pricingMethod();
        String currency = request.currency() == null || request.currency().isBlank()
                ? "TZS" : request.currency().trim().toUpperCase();
        if (!currency.matches("[A-Z]{3}")) {
            throw new BusinessException("Currency must be a three-letter code");
        }
        BigDecimal amount = request.amount();
        if (method.requiresAmount()) {
            if (amount == null || amount.signum() <= 0) {
                throw new BusinessException("Enter a price greater than zero");
            }
            amount = amount.setScale(2, RoundingMode.HALF_UP);
        } else if (amount != null) {
            throw new BusinessException("This pricing method does not take a public amount");
        }
        if (method == PricingMethod.VARIABLE && (request.unitLabel() == null || request.unitLabel().isBlank())) {
            throw new BusinessException("A variable price needs a unit, such as per sample");
        }
        if (!creating && request.note() != null && request.note().length() > 500) {
            throw new BusinessException("The price note is too long");
        }
        return new Priced(method, amount, currency, blankToNull(request.unitLabel()), blankToNull(request.conditions()));
    }

    private void assertVisible(ConsultancyCategory category) {
        Set<ConsultancyType> lines = departmentService.visibleServiceLines();
        if (lines != null && !lines.contains(category.getConsultancyType())) {
            throw new ResourceNotFoundException("Consultancy category", category.getId());
        }
    }

    private Long latestId(Long serviceId) {
        return versionRepository.findFirstByServiceIdOrderByIdDesc(serviceId)
                .map(ServicePriceVersion::getId)
                .orElse(null);
    }

    private ConsultancyCategory category(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consultancy category", id));
    }

    private ServiceOffering service(Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consultancy service", id));
    }

    private void notifyAdmins(String message) {
        for (String email : userRepository.findEnabledEmailsByRole(RoleName.ADMIN)) {
            notificationService.schedule(email, message);
        }
    }

    private static String normalizeCode(String code) {
        String normalized = code.trim().toUpperCase().replace(' ', '_');
        if (!normalized.matches("[A-Z][A-Z0-9_]{1,29}")) {
            throw new BusinessException("Use a short category code made of letters, numbers, and underscores");
        }
        return normalized;
    }

    private static boolean sameMoney(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return left == null && right == null;
        }
        return left.compareTo(right) == 0;
    }

    private static boolean sameText(String left, String right) {
        String a = left == null ? "" : left;
        String b = right == null ? "" : right;
        return a.equals(b);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String actor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "system";
        }
        return authentication.getName();
    }

    private record Priced(PricingMethod method, BigDecimal amount, String currency, String unitLabel, String conditions) {
        BigDecimal publicAmount() {
            return method.requiresAmount() ? amount : null;
        }
    }
}
