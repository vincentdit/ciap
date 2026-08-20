package tz.go.tirdo.ciap.domain.consultancy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RequestStatus state machine")
class RequestStatusTest {

    @Test
    @DisplayName("happy path advances one step at a time")
    void happyPathTransitions() {
        assertThat(RequestStatus.SUBMITTED.canTransitionTo(RequestStatus.UNDER_REVIEW)).isTrue();
        assertThat(RequestStatus.UNDER_REVIEW.canTransitionTo(RequestStatus.QUOTED)).isTrue();
        assertThat(RequestStatus.QUOTED.canTransitionTo(RequestStatus.AWAITING_PAYMENT)).isTrue();
        assertThat(RequestStatus.AWAITING_PAYMENT.canTransitionTo(RequestStatus.PAID)).isTrue();
        assertThat(RequestStatus.PAID.canTransitionTo(RequestStatus.ASSIGNED)).isTrue();
        assertThat(RequestStatus.ASSIGNED.canTransitionTo(RequestStatus.IN_EXECUTION)).isTrue();
        assertThat(RequestStatus.IN_EXECUTION.canTransitionTo(RequestStatus.REPORT_SUBMITTED)).isTrue();
        assertThat(RequestStatus.REPORT_SUBMITTED.canTransitionTo(RequestStatus.CLOSED)).isTrue();
    }

    @Test
    @DisplayName("rejects skipping a step")
    void rejectsSkips() {
        assertThat(RequestStatus.SUBMITTED.canTransitionTo(RequestStatus.ASSIGNED)).isFalse();
        assertThat(RequestStatus.SUBMITTED.canTransitionTo(RequestStatus.PAID)).isFalse();
        assertThat(RequestStatus.QUOTED.canTransitionTo(RequestStatus.CLOSED)).isFalse();
        assertThat(RequestStatus.ASSIGNED.canTransitionTo(RequestStatus.CLOSED)).isFalse();
    }

    @Test
    @DisplayName("rejects moving backwards")
    void rejectsBackwards() {
        assertThat(RequestStatus.PAID.canTransitionTo(RequestStatus.QUOTED)).isFalse();
        assertThat(RequestStatus.IN_EXECUTION.canTransitionTo(RequestStatus.ASSIGNED)).isFalse();
    }

    @Test
    @DisplayName("terminal states allow no further transitions")
    void terminalStates() {
        assertThat(RequestStatus.CLOSED.isTerminal()).isTrue();
        assertThat(RequestStatus.REJECTED.isTerminal()).isTrue();
        assertThat(RequestStatus.CANCELLED.isTerminal()).isTrue();
        for (RequestStatus s : RequestStatus.values()) {
            assertThat(RequestStatus.CLOSED.canTransitionTo(s)).isFalse();
        }
    }

    @Test
    @DisplayName("cancellation is allowed from active states but not from closed")
    void cancellationRules() {
        assertThat(RequestStatus.SUBMITTED.canTransitionTo(RequestStatus.CANCELLED)).isTrue();
        assertThat(RequestStatus.ASSIGNED.canTransitionTo(RequestStatus.CANCELLED)).isTrue();
        assertThat(RequestStatus.IN_EXECUTION.canTransitionTo(RequestStatus.CANCELLED)).isTrue();
        assertThat(RequestStatus.CLOSED.canTransitionTo(RequestStatus.CANCELLED)).isFalse();
    }

    @Test
    @DisplayName("rejection only from review/quotation stages")
    void rejectionRules() {
        assertThat(RequestStatus.UNDER_REVIEW.canTransitionTo(RequestStatus.REJECTED)).isTrue();
        assertThat(RequestStatus.QUOTED.canTransitionTo(RequestStatus.REJECTED)).isTrue();
        assertThat(RequestStatus.PAID.canTransitionTo(RequestStatus.REJECTED)).isFalse();
        assertThat(RequestStatus.IN_EXECUTION.canTransitionTo(RequestStatus.REJECTED)).isFalse();
    }
}
