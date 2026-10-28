package tz.go.tirdo.ciap.domain.consultancy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ReassignDto(
        @NotNull Long consultantId,
        @NotBlank @Size(max = 500) String reason,
        @Size(max = 500) String note,
        LocalDate deadline
) {}
