package tz.go.tirdo.ciap.domain.consultancy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyType;

import java.time.LocalDate;

public record CreateRequestDto(
        @NotNull Long clientId,
        @NotNull ConsultancyType consultancyType,
        @NotBlank String title,
        String description,
        LocalDate expectedStartDate
) {}
