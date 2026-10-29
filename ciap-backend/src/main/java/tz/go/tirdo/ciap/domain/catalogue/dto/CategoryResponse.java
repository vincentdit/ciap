package tz.go.tirdo.ciap.domain.catalogue.dto;

import tz.go.tirdo.ciap.domain.catalogue.ConsultancyCategory;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyType;

public record CategoryResponse(
        Long id,
        String code,
        String name,
        String description,
        ConsultancyType consultancyType,
        int displayOrder,
        boolean active
) {
    public static CategoryResponse from(ConsultancyCategory category) {
        return new CategoryResponse(
                category.getId(), category.getCode(), category.getName(), category.getDescription(),
                category.getConsultancyType(), category.getDisplayOrder(), category.isActive());
    }
}
