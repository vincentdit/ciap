package tz.go.tirdo.ciap.domain.consultancy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateRequestDto(
        @NotNull Long clientId,
        @NotNull ConsultancyType consultancyType,
        @NotBlank String title,
        String description,
        LocalDate expectedStartDate,
        Long serviceId,
        Long seenPriceVersionId,
        BigDecimal quantity
) {
    @JsonIgnore
    public CreateRequestDto(Long clientId, ConsultancyType consultancyType, String title,
                             String description, LocalDate expectedStartDate) {
        this(clientId, consultancyType, title, description, expectedStartDate, null, null, null);
    }
}
