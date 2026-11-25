package com.apitracker.apitracker.dto;

import com.apitracker.apitracker.domain.IssueLinkType;
import jakarta.validation.constraints.NotNull;

public record CreateIssueLinkRequest(
        @NotNull(message = "L'identifiant du ticket cible est obligatoire")
        Long targetTicketId,

        @NotNull(message = "Le type de lien est obligatoire")
        IssueLinkType type
) {
}
