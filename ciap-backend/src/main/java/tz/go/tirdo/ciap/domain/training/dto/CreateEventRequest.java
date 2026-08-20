package tz.go.tirdo.ciap.domain.training.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tz.go.tirdo.ciap.domain.training.TrainingMode;

import java.time.Instant;

public record CreateEventRequest(
        @NotBlank String title,
        String description,
        @NotNull TrainingMode mode,
        String location,
        @NotNull Instant startsAt,
        Instant endsAt,
        Integer capacity,
        Boolean published
) {}
