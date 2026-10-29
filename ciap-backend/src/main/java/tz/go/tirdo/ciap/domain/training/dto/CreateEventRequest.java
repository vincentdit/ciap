package tz.go.tirdo.ciap.domain.training.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tz.go.tirdo.ciap.domain.training.FeeKind;
import tz.go.tirdo.ciap.domain.training.TrainingMode;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateEventRequest(
        @NotBlank String title,
        String description,
        @NotNull TrainingMode mode,
        String location,
        @NotNull Instant startsAt,
        Instant endsAt,
        Integer capacity,
        Boolean published,
        FeeKind feeKind,
        BigDecimal feeAmount,
        String feeCurrency,
        Instant registrationOpensAt,
        Instant registrationClosesAt,
        String objectives,
        String audience,
        String prerequisites,
        String contactEmail
) {
    @JsonIgnore
    public CreateEventRequest(String title, String description, TrainingMode mode, String location,
                               Instant startsAt, Instant endsAt, Integer capacity, Boolean published) {
        this(title, description, mode, location, startsAt, endsAt, capacity, published,
                null, null, null, null, null, null, null, null, null);
    }
}
