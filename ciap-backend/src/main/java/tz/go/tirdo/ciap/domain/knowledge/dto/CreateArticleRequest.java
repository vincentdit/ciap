package tz.go.tirdo.ciap.domain.knowledge.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateArticleRequest(
        @NotBlank String title,
        String category,
        String summary,
        @NotBlank String body,
        Boolean published
) {}
