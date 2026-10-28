package tz.go.tirdo.ciap.domain.consultancy.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AssignDto(
        @NotNull Long consultantId,
        @Size(max = 500) String note,
        LocalDate deadline
) {
    public AssignDto(Long consultantId) {
        this(consultantId, null, null);
    }
}
