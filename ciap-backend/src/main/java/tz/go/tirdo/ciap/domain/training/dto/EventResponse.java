package tz.go.tirdo.ciap.domain.training.dto;

import tz.go.tirdo.ciap.domain.training.FeeKind;
import tz.go.tirdo.ciap.domain.training.RegistrationStatus;
import tz.go.tirdo.ciap.domain.training.TrainingEvent;
import tz.go.tirdo.ciap.domain.training.TrainingMode;

import java.math.BigDecimal;
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
        boolean registered,
        FeeKind feeKind,
        BigDecimal feeAmount,
        String feeCurrency,
        Instant registrationOpensAt,
        Instant registrationClosesAt,
        String objectives,
        String audience,
        String prerequisites,
        String contactEmail,
        RegistrationStatus registrationStatus,
        String controlNumber
) {
    public static EventResponse from(TrainingEvent e, long registeredCount, boolean registered,
                                      RegistrationStatus registrationStatus, String controlNumber) {
        return new EventResponse(
                e.getId(), e.getTitle(), e.getDescription(), e.getMode(), e.getLocation(),
                e.getStartsAt(), e.getEndsAt(), e.getCapacity(), registeredCount,
                e.isPublished(), registered,
                e.getFeeKind(), e.getFeeKind() == FeeKind.PAID ? e.getFeeAmount() : BigDecimal.ZERO,
                e.getFeeCurrency(), e.getRegistrationOpensAt(), e.getRegistrationClosesAt(),
                e.getObjectives(), e.getAudience(), e.getPrerequisites(), e.getContactEmail(),
                registrationStatus, controlNumber);
    }
}
