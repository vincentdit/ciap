package tz.go.tirdo.ciap.domain.consultancy;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.ciap.common.exception.ResourceNotFoundException;
import tz.go.tirdo.ciap.domain.consultancy.dto.RequestEventResponse;
import tz.go.tirdo.ciap.domain.notification.NotificationService;
import tz.go.tirdo.ciap.security.AccessGuard;

import java.time.Instant;
import java.util.List;

@Service
public class RequestAuditService {

    private final RequestStatusEventRepository eventRepository;
    private final ConsultancyRequestRepository requestRepository;
    private final AccessGuard accessGuard;
    private final NotificationService notificationService;

    public RequestAuditService(RequestStatusEventRepository eventRepository,
                               ConsultancyRequestRepository requestRepository,
                               AccessGuard accessGuard,
                               NotificationService notificationService) {
        this.eventRepository = eventRepository;
        this.requestRepository = requestRepository;
        this.accessGuard = accessGuard;
        this.notificationService = notificationService;
    }

    public void record(ConsultancyRequest request, RequestStatus from, RequestStatus to, String reason) {
        String actor = actorName();
        String note = trimReason(reason);
        eventRepository.save(RequestStatusEvent.builder()
                .requestId(request.getId())
                .fromStatus(from == null ? null : from.name())
                .toStatus(to.name())
                .actor(actor)
                .reason(note)
                .createdAt(Instant.now())
                .build());

        String message = "Request " + request.getReferenceNo() + " is now " + to.name() + ".";
        String clientEmail = request.getClient() == null ? null : request.getClient().getEmail();
        notificationService.schedule(clientEmail, message);
        if (actor.contains("@") && (clientEmail == null || !actor.equalsIgnoreCase(clientEmail))) {
            notificationService.schedule(actor, message);
        }
    }

    @Transactional(readOnly = true)
    public List<RequestEventResponse> list(Long requestId) {
        ConsultancyRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ConsultancyRequest", requestId));
        accessGuard.assertCanReadRequest(request);
        return eventRepository.findByRequestIdOrderByIdAsc(requestId)
                .stream().map(RequestEventResponse::from).toList();
    }

    private String trimReason(String reason) {
        if (reason == null) return null;
        String note = reason.trim();
        if (note.isEmpty()) return null;
        return note.length() > 500 ? note.substring(0, 500) : note;
    }

    private String actorName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "system";
        }
        String name = auth.getName();
        if (name == null || name.isBlank() || "anonymousUser".equals(name)) {
            return "system";
        }
        return name.length() > 150 ? name.substring(0, 150) : name;
    }
}
