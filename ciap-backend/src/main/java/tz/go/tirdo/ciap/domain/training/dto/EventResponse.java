package tz.go.tirdo.ciap.domain.training.dto;

import tz.go.tirdo.ciap.domain.training.TrainingEvent;
import tz.go.tirdo.ciap.domain.training.TrainingMode;

import java.time.Instant;

public record EventResponse(
        Long id,
        String title,
        String description,
        TrainingMode mode,
        String location,
        Instant startsAt,
        Instant endsAt,
        int capacity,
        long registeredCount,
        boolean published,
        boolean registered
) {
    public static EventResponse from(TrainingEvent e, long registeredCount, boolean registered) {
        return new EventResponse(
                e.getId(), e.getTitle(), e.getDescription(), e.getMode(), e.getLocation(),
                e.getStartsAt(), e.getEndsAt(), e.getCapacity(), registeredCount,
                e.isPublished(), registered);
    }
}
