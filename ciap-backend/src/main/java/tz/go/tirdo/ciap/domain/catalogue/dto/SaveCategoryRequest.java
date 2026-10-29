package tz.go.tirdo.ciap.domain.catalogue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyType;

public record SaveCategoryRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        @NotNull ConsultancyType consultancyType,
        Integer displayOrder,
        Boolean active
) {}
