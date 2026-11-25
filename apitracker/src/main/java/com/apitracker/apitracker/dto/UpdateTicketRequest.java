package com.apitracker.apitracker.dto;

import com.apitracker.apitracker.domain.TicketLabel;
import com.apitracker.apitracker.domain.TicketPriority;
import com.apitracker.apitracker.domain.TicketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateTicketRequest(
        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 255, message = "Le titre ne peut pas dépasser 255 caractères")
        String title,

        @Size(max = 5000, message = "La description ne peut pas dépasser 5000 caractères")
        String description,

        @NotNull(message = "Le statut est obligatoire")
        TicketStatus status,

        @NotNull(message = "La priorité est obligatoire")
        TicketPriority priority,

        @Pattern(regexp = "^(|\\d{4}-\\d{2}-\\d{2})$", message = "La date d'échéance doit être au format YYYY-MM-DD")
        String dueDate,

        String assigneeId,

        List<TicketLabel> labels
) {
}
