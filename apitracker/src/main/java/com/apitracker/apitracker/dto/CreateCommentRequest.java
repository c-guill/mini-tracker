package com.apitracker.apitracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "Le contenu du commentaire est obligatoire")
        @Size(max = 2000, message = "Le commentaire ne peut pas dépasser 2000 caractères")
        String content,

        @Size(max = 100, message = "Le nom de l'auteur ne peut pas dépasser 100 caractères")
        String author
) {
}
