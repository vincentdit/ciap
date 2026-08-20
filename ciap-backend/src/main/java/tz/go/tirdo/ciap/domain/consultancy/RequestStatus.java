package tz.go.tirdo.ciap.domain.consultancy;

import java.util.Map;
import java.util.Set;

/**
 * Workflow states per Module 3:
 * Request -> Review -> Quotation -> Payment -> Assignment -> Execution -> Report -> Closure
 * (CANCELLED / REJECTED are terminal off-ramps.)
 */
public enum RequestStatus {
    SUBMITTED,
    UNDER_REVIEW,
    QUOTED,
    AWAITING_PAYMENT,
    PAID,
    ASSIGNED,
    IN_EXECUTION,
    REPORT_SUBMITTED,
    CLOSED,
    REJECTED,
    CANCELLED;

    private static final Map<RequestStatus, Set<RequestStatus>> TRANSITIONS = Map.of(
            SUBMITTED,        Set.of(UNDER_REVIEW, CANCELLED),
            UNDER_REVIEW,     Set.of(QUOTED, REJECTED, CANCELLED),
            QUOTED,           Set.of(AWAITING_PAYMENT, REJECTED, CANCELLED),
            AWAITING_PAYMENT, Set.of(PAID, CANCELLED),
            PAID,             Set.of(ASSIGNED, CANCELLED),
            ASSIGNED,         Set.of(IN_EXECUTION, CANCELLED),
            IN_EXECUTION,     Set.of(REPORT_SUBMITTED, CANCELLED),
            REPORT_SUBMITTED, Set.of(CLOSED),
            CLOSED,           Set.of()
    );

    public boolean canTransitionTo(RequestStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean isTerminal() {
        return this == CLOSED || this == REJECTED || this == CANCELLED;
    }
}
