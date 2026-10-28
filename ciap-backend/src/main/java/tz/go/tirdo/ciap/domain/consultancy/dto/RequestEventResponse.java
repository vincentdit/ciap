package tz.go.tirdo.ciap.domain.consultancy.dto;

import tz.go.tirdo.ciap.domain.consultancy.RequestStatusEvent;

import java.time.Instant;

public record RequestEventResponse(
        Long id,
        Long requestId,
        String fromStatus,
        String toStatus,
        String actor,
        String reason,
        Instant createdAt
) {
    public static RequestEventResponse from(RequestStatusEvent event) {
        return new RequestEventResponse(
                event.getId(), event.getRequestId(), event.getFromStatus(), event.getToStatus(),
                event.getActor(), event.getReason(), event.getCreatedAt());
    }
}
