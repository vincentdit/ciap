package tz.go.tirdo.ciap.domain.training;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.ciap.common.SearchTerms;
import tz.go.tirdo.ciap.common.exception.BusinessException;
import tz.go.tirdo.ciap.common.exception.DuplicateResourceException;
import tz.go.tirdo.ciap.common.exception.ResourceNotFoundException;
import tz.go.tirdo.ciap.domain.billing.PaymentRepository;
import tz.go.tirdo.ciap.domain.billing.PaymentStatus;
import tz.go.tirdo.ciap.domain.catalogue.PriceLabels;
import tz.go.tirdo.ciap.domain.notification.NotificationService;
import tz.go.tirdo.ciap.domain.training.dto.CreateEventRequest;
import tz.go.tirdo.ciap.domain.training.dto.EventResponse;
import tz.go.tirdo.ciap.domain.training.dto.TrainingPaymentResponse;
import tz.go.tirdo.ciap.security.AccessGuard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TrainingService {

    private static final EnumSet<RegistrationStatus> HOLDING_A_SEAT =
            EnumSet.of(RegistrationStatus.CONFIRMED, RegistrationStatus.AWAITING_PAYMENT);

    private final TrainingEventRepository eventRepository;
    private final TrainingRegistrationRepository registrationRepository;
    private final TrainingPaymentRepository trainingPaymentRepository;
    private final PaymentRepository paymentRepository;
    private final AccessGuard accessGuard;
    private final NotificationService notificationService;

    public TrainingService(TrainingEventRepository eventRepository,
                           TrainingRegistrationRepository registrationRepository,
                           TrainingPaymentRepository trainingPaymentRepository,
                           PaymentRepository paymentRepository,
                           AccessGuard accessGuard,
                           NotificationService notificationService) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.trainingPaymentRepository = trainingPaymentRepository;
        this.paymentRepository = paymentRepository;
        this.accessGuard = accessGuard;
        this.notificationService = notificationService;
    }

    @Transactional
    public TrainingEvent create(CreateEventRequest req) {
        TrainingEvent e = TrainingEvent.builder()
                .title(req.title())
                .description(req.description())
                .mode(req.mode())
                .location(req.location())
                .startsAt(req.startsAt())
                .endsAt(req.endsAt())
                .capacity(req.capacity() != null ? req.capacity() : 0)
                .published(Boolean.TRUE.equals(req.published()))
                .build();
        applyDetails(e, req);
        applyFee(e, req, true);
        return eventRepository.save(e);
    }

    @Transactional
    public TrainingEvent update(Long id, CreateEventRequest req) {
        TrainingEvent e = getById(id);
        BigDecimal previousFee = e.getFeeAmount();
        FeeKind previousKind = e.getFeeKind();
        e.setTitle(req.title());
        e.setDescription(req.description());
        e.setMode(req.mode());
        e.setLocation(req.location());
        e.setStartsAt(req.startsAt());
        e.setEndsAt(req.endsAt());
        if (req.capacity() != null) e.setCapacity(req.capacity());
        if (req.published() != null) e.setPublished(req.published());
        applyDetails(e, req);
        applyFee(e, req, false);
        if (previousKind != e.getFeeKind()
                || (previousFee == null ? e.getFeeAmount() != null : previousFee.compareTo(e.getFeeAmount() == null ? BigDecimal.ZERO : e.getFeeAmount()) != 0)) {
            notificationService.schedule(actorEmail(),
                    "Training fee for \"" + e.getTitle() + "\" is now " + feeLabel(e) + ". Existing registrations keep their original fee.");
        }
        return eventRepository.save(e);
    }

    @Transactional
    public TrainingEvent setPublished(Long id, boolean published) {
        TrainingEvent e = getById(id);
        e.setPublished(published);
        notificationService.schedule(actorEmail(),
                published ? "Training \"" + e.getTitle() + "\" is published."
                        : "Training \"" + e.getTitle() + "\" is no longer published.");
        return eventRepository.save(e);
    }

    @Transactional(readOnly = true)
    public TrainingEvent getById(Long id) {
        TrainingEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Training event", id));
        if (!event.isPublished() && accessGuard.current().kind() == AccessGuard.Kind.CLIENT) {
            throw new ResourceNotFoundException("Training event", id);
        }
        return event;
    }

    @Transactional(readOnly = true)
    public Page<TrainingEvent> listPublished(String q, Pageable pageable) {
        return eventRepository.searchPublished(SearchTerms.like(q), pageable);
    }

    @Transactional(readOnly = true)
    public Page<TrainingEvent> listAll(String q, Pageable pageable) {
        return eventRepository.searchAll(SearchTerms.like(q), pageable);
    }

    @Transactional(readOnly = true)
    public EventResponse present(TrainingEvent event, String email) {
        TrainingRegistration registration = registrationRepository
                .findByEventIdAndRegistrantEmail(event.getId(), email)
                .filter(row -> row.getStatus() != RegistrationStatus.CANCELLED)
                .orElse(null);
        String control = null;
        if (registration != null) {
            control = trainingPaymentRepository.findByRegistrationId(registration.getId())
                    .map(TrainingPayment::getControlNumber)
                    .orElse(null);
        }
        return EventResponse.from(event, holding(event.getId()), registration != null,
                registration == null ? null : registration.getStatus(), control);
    }

    @Transactional
    public EventResponse register(Long eventId, String email) {
        TrainingEvent e = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Training event", eventId));
        if (!e.isPublished()) {
            throw new ResourceNotFoundException("Training event", eventId);
        }
        Instant now = Instant.now();
        if (e.getRegistrationOpensAt() != null && now.isBefore(e.getRegistrationOpensAt())) {
            throw new BusinessException("Registration is not open yet");
        }
        if (e.getRegistrationClosesAt() != null && !now.isBefore(e.getRegistrationClosesAt())) {
            throw new BusinessException("Registration has closed");
        }
        registrationRepository.findByEventIdAndRegistrantEmail(eventId, email).ifPresent(existing -> {
            if (existing.getStatus() != RegistrationStatus.CANCELLED) {
                throw new DuplicateResourceException("You are already registered for this event");
            }
        });
        if (e.getCapacity() > 0 && holding(eventId) >= e.getCapacity()) {
            throw new BusinessException("This event is full");
        }
        boolean paid = e.getFeeKind() == FeeKind.PAID;
        TrainingRegistration registration = registrationRepository
                .findByEventIdAndRegistrantEmail(eventId, email)
                .orElseGet(() -> TrainingRegistration.builder().event(e).registrantEmail(email).build());
        registration.setFeeKind(e.getFeeKind());
        registration.setFeeAmount(paid ? e.getFeeAmount() : BigDecimal.ZERO);
        registration.setFeeCurrency(e.getFeeCurrency());
        registration.setStatus(paid ? RegistrationStatus.AWAITING_PAYMENT : RegistrationStatus.CONFIRMED);
        registration = registrationRepository.save(registration);

        String control = null;
        if (paid) {
            TrainingPayment payment = trainingPaymentRepository.findByRegistrationId(registration.getId())
                    .orElse(null);
            if (payment != null && payment.getStatus() == PaymentStatus.PAID) {
                throw new BusinessException("This registration is already paid");
            }
            if (payment == null) {
                payment = TrainingPayment.builder()
                        .registration(registration)
                        .controlNumber(newControlNumber())
                        .amount(registration.getFeeAmount())
                        .currency(registration.getFeeCurrency())
                        .status(PaymentStatus.PENDING)
                        .build();
            } else if (payment.getStatus() != PaymentStatus.PENDING) {
                payment.setControlNumber(newControlNumber());
                payment.setAmount(registration.getFeeAmount());
                payment.setCurrency(registration.getFeeCurrency());
                payment.setStatus(PaymentStatus.PENDING);
                payment.setPaidAt(null);
                payment.setGepgReference(null);
            }
            payment = trainingPaymentRepository.save(payment);
            control = payment.getControlNumber();
            notificationService.schedule(email, "Registration for \"" + e.getTitle()
                    + "\" is awaiting payment of " + PriceLabels.money(registration.getFeeAmount(), registration.getFeeCurrency())
                    + ". Control number " + control
                    + ". Finance records the payment. Submitting this form does not mark it paid.");
        } else {
            notificationService.schedule(email, "You are registered for \"" + e.getTitle() + "\". No registration fee.");
        }
        return present(e, email);
    }

    @Transactional
    public void cancelRegistration(Long eventId, String email) {
        TrainingRegistration registration = registrationRepository
                .findByEventIdAndRegistrantEmail(eventId, email)
                .orElse(null);
        if (registration == null || registration.getStatus() == RegistrationStatus.CANCELLED) {
            return;
        }
        TrainingPayment payment = trainingPaymentRepository.findByRegistrationId(registration.getId()).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.PAID) {
            registration.setStatus(RegistrationStatus.CANCELLED);
            registrationRepository.save(registration);
            return;
        }
        if (payment != null) {
            payment.setStatus(PaymentStatus.CANCELLED);
            trainingPaymentRepository.save(payment);
        }
        if (registration.getFeeKind() == FeeKind.FREE) {
            registrationRepository.delete(registration);
        } else {
            registration.setStatus(RegistrationStatus.CANCELLED);
            registrationRepository.save(registration);
        }
    }

    @Transactional
    public TrainingPaymentResponse confirmPayment(String controlNumber, String gepgReference, String note) {
        if (note == null || note.isBlank()) {
            throw new BusinessException("A reason is required to record this payment");
        }
        TrainingPayment payment = trainingPaymentRepository.findByControlNumberForUpdate(controlNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Training payment", controlNumber));
        if (payment.getStatus() == PaymentStatus.PAID) {
            return TrainingPaymentResponse.from(payment);
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException("This training payment cannot be recorded");
        }
        if (gepgReference != null && !gepgReference.isBlank()) {
            payment.setGepgReference(gepgReference.trim());
        }
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(Instant.now());
        TrainingRegistration registration = payment.getRegistration();
        registration.setStatus(RegistrationStatus.CONFIRMED);
        notificationService.schedule(registration.getRegistrantEmail(),
                "Payment for \"" + registration.getEvent().getTitle() + "\" is recorded. Your registration is confirmed.");
        return TrainingPaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public List<TrainingPaymentResponse> pendingPayments() {
        return trainingPaymentRepository.findByStatusOrderByIdDesc(PaymentStatus.PENDING).stream()
                .map(TrainingPaymentResponse::from)
                .toList();
    }

    private long holding(Long eventId) {
        return registrationRepository.countByEventIdAndStatusIn(eventId, HOLDING_A_SEAT);
    }

    private void applyDetails(TrainingEvent event, CreateEventRequest req) {
        event.setObjectives(blank(req.objectives()));
        event.setAudience(blank(req.audience()));
        event.setPrerequisites(blank(req.prerequisites()));
        event.setContactEmail(blank(req.contactEmail()));
        if (req.registrationOpensAt() != null || req.registrationClosesAt() != null) {
            event.setRegistrationOpensAt(req.registrationOpensAt());
            event.setRegistrationClosesAt(req.registrationClosesAt());
        }
        if (event.getRegistrationOpensAt() != null && event.getRegistrationClosesAt() != null
                && !event.getRegistrationOpensAt().isBefore(event.getRegistrationClosesAt())) {
            throw new BusinessException("Registration must open before it closes");
        }
    }

    private void applyFee(TrainingEvent event, CreateEventRequest req, boolean creating) {
        if (req.feeKind() == null && !creating) {
            return;
        }
        FeeKind kind = req.feeKind() == null ? FeeKind.FREE : req.feeKind();
        if (!accessGuard.isAdmin() && (kind == FeeKind.PAID || req.feeAmount() != null)) {
            throw new BusinessException("Only an administrator can set a training fee");
        }
        if (kind == FeeKind.FREE) {
            event.setFeeKind(FeeKind.FREE);
            event.setFeeAmount(null);
            event.setFeeCurrency("TZS");
            return;
        }
        if (req.feeAmount() == null || req.feeAmount().signum() <= 0) {
            throw new BusinessException("A paid event needs a fee greater than zero");
        }
        String currency = req.feeCurrency() == null || req.feeCurrency().isBlank()
                ? "TZS" : req.feeCurrency().trim().toUpperCase();
        if (!currency.matches("[A-Z]{3}")) {
            throw new BusinessException("Currency must be a three-letter code");
        }
        event.setFeeKind(FeeKind.PAID);
        event.setFeeAmount(req.feeAmount().setScale(2, RoundingMode.HALF_UP));
        event.setFeeCurrency(currency);
    }

    private String newControlNumber() {
        String cn;
        do {
            long n = ThreadLocalRandom.current().nextLong(100_000_000_000L, 1_000_000_000_000L);
            cn = Long.toString(n);
        } while (trainingPaymentRepository.existsByControlNumber(cn) || paymentRepository.existsByControlNumber(cn));
        return cn;
    }

    private static String feeLabel(TrainingEvent event) {
        if (event.getFeeKind() == FeeKind.FREE) {
            return "free";
        }
        return PriceLabels.money(event.getFeeAmount(), event.getFeeCurrency());
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String actorEmail() {
        var actor = accessGuard.current();
        return actor.email();
    }
}
