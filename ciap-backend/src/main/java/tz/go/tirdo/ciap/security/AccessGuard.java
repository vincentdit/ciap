package tz.go.tirdo.ciap.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import tz.go.tirdo.ciap.common.exception.ResourceNotFoundException;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyRequest;
import tz.go.tirdo.ciap.domain.consultant.Consultant;

/**
 * Object-level access. A client sees only its own organisation. An expert sees
 * only engagements assigned to that expert. A Head of Department sees only the
 * department they head. Admin, manager, and finance keep organisation-wide read
 * access. Calls with no authenticated user are left unchanged.
 */
@Component
public class AccessGuard {

    public enum Kind { ANONYMOUS, CLIENT, EXPERT, HOD, INSTITUTION }

    public record Actor(Kind kind, String email) {}

    public Actor current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return new Actor(Kind.ANONYMOUS, null);
        }
        if (has(auth, "ADMIN") || has(auth, "MANAGER") || has(auth, "FINANCE")) {
            return new Actor(Kind.INSTITUTION, auth.getName());
        }
        if (has(auth, "HOD")) {
            return new Actor(Kind.HOD, auth.getName());
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
            case HOD -> isHodOf(request);
        };
    }

    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && has(auth, "ADMIN");
    }

    public boolean isHodOf(ConsultancyRequest request) {
        Actor actor = current();
        if (actor.kind() != Kind.HOD || actor.email() == null || request.getDepartment() == null) {
            return false;
        }
        var department = request.getDepartment();
        var hod = department.getHodUser();
        return department.isActive()
                && hod != null
                && hod.isEnabled()
                && actor.email().equalsIgnoreCase(hod.getEmail());
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
