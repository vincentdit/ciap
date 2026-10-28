package tz.go.tirdo.ciap.domain.training;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.ciap.common.SearchTerms;
import tz.go.tirdo.ciap.common.exception.BusinessException;
import tz.go.tirdo.ciap.common.exception.DuplicateResourceException;
import tz.go.tirdo.ciap.common.exception.ResourceNotFoundException;
import tz.go.tirdo.ciap.domain.training.dto.CreateEventRequest;
import tz.go.tirdo.ciap.security.AccessGuard;

@Service
public class TrainingService {

    private final TrainingEventRepository eventRepository;
    private final TrainingRegistrationRepository registrationRepository;
    private final AccessGuard accessGuard;

    public TrainingService(TrainingEventRepository eventRepository,
                           TrainingRegistrationRepository registrationRepository,
                           AccessGuard accessGuard) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.accessGuard = accessGuard;
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
        return eventRepository.save(e);
    }

    @Transactional
    public TrainingEvent update(Long id, CreateEventRequest req) {
        TrainingEvent e = getById(id);
        e.setTitle(req.title());
        e.setDescription(req.description());
        e.setMode(req.mode());
        e.setLocation(req.location());
        e.setStartsAt(req.startsAt());
        e.setEndsAt(req.endsAt());
        if (req.capacity() != null) e.setCapacity(req.capacity());
        if (req.published() != null) e.setPublished(req.published());
        return eventRepository.save(e);
    }

    @Transactional
    public TrainingEvent setPublished(Long id, boolean published) {
        TrainingEvent e = getById(id);
        e.setPublished(published);
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
    public Page<TrainingEvent> listPublished(Pageable pageable) {
        return listPublished(null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<TrainingEvent> listPublished(String q, Pageable pageable) {
        return eventRepository.searchPublished(SearchTerms.like(q), pageable);
    }

    @Transactional(readOnly = true)
    public Page<TrainingEvent> listAll(Pageable pageable) {
        return listAll(null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<TrainingEvent> listAll(String q, Pageable pageable) {
        return eventRepository.searchAll(SearchTerms.like(q), pageable);
    }

    @Transactional(readOnly = true)
    public long registeredCount(Long eventId) {
        return registrationRepository.countByEventId(eventId);
    }

    @Transactional(readOnly = true)
    public boolean isRegistered(Long eventId, String email) {
        return registrationRepository.existsByEventIdAndRegistrantEmail(eventId, email);
    }

    @Transactional
    public void register(Long eventId, String email) {
        TrainingEvent e = getById(eventId);
        if (!e.isPublished()) {
            throw new ResourceNotFoundException("Training event", eventId);
        }
        if (registrationRepository.existsByEventIdAndRegistrantEmail(eventId, email)) {
            throw new DuplicateResourceException("You are already registered for this event");
        }
        if (e.getCapacity() > 0 && registrationRepository.countByEventId(eventId) >= e.getCapacity()) {
            throw new BusinessException("This event is full");
        }
        registrationRepository.save(TrainingRegistration.builder()
                .event(e)
                .registrantEmail(email)
                .build());
    }

    @Transactional
    public void cancelRegistration(Long eventId, String email) {
        registrationRepository.findByEventIdAndRegistrantEmail(eventId, email)
                .ifPresent(registrationRepository::delete);
    }
}
