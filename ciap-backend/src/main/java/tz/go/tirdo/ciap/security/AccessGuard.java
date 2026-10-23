package tz.go.tirdo.ciap.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import tz.go.tirdo.ciap.common.exception.ResourceNotFoundException;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyRequest;
import tz.go.tirdo.ciap.domain.consultant.Consultant;

/**
 * Object-level access for the two boundaries this application actually has:
 * a client organisation may see only its own engagements, and an expert may
 * see only engagements assigned to that expert. Institutional roles
 * (admin, manager, finance) keep organisation-wide access.
 *
 * Calls with no authenticated user — service-layer tests and internal
 * workflows — are left unchanged.
 */
@Component
public class AccessGuard {

    public enum Kind { ANONYMOUS, CLIENT, EXPERT, INSTITUTION }

    public record Actor(Kind kind, String email) {}

    public Actor current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return new Actor(Kind.ANONYMOUS, null);
        }
        if (has(auth, "ADMIN") || has(auth, "MANAGER") || has(auth, "FINANCE")) {
            return new Actor(Kind.INSTITUTION, auth.getName());
        }
        if (has(auth, "EXPERT")) {
            return new Actor(Kind.EXPERT, auth.getName());
        }
        if (has(auth, "CLIENT")) {
            return new Actor(Kind.CLIENT, auth.getName());
        }
        return new Actor(Kind.INSTITUTION, auth.getName());
    }

    public void assertCanReadRequest(ConsultancyRequest request) {
        if (!canReadRequest(request)) {
            throw new ResourceNotFoundException("ConsultancyRequest", request.getId());
        }
    }

    public boolean canReadRequest(ConsultancyRequest request) {
        Actor actor = current();
        return switch (actor.kind()) {
            case ANONYMOUS, INSTITUTION -> true;
            case CLIENT -> ownsClient(request, actor.email());
            case EXPERT -> isAssignedExpert(request, actor.email());
        };
    }

    private boolean ownsClient(ConsultancyRequest request, String email) {
        return request.getClient() != null
                && request.getClient().getUser() != null
                && email != null
                && email.equalsIgnoreCase(request.getClient().getUser().getEmail());
    }

    private boolean isAssignedExpert(ConsultancyRequest request, String email) {
        Consultant consultant = request.getAssignedConsultant();
        return consultant != null
                && consultant.getUser() != null
                && email != null
                && email.equalsIgnoreCase(consultant.getUser().getEmail());
    }

    private boolean has(Authentication auth, String role) {
        String prefixed = "ROLE_" + role;
        return auth.getAuthorities().stream()
                .anyMatch(a -> prefixed.equals(a.getAuthority()) || role.equals(a.getAuthority()));
    }
}
