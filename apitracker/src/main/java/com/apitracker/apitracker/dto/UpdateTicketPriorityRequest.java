package com.apitracker.apitracker.dto;

import com.apitracker.apitracker.domain.TicketPriority;
import jakarta.validation.constraints.NotNull;

public record UpdateTicketPriorityRequest(
        @NotNull(message = "La priorité est obligatoire")
        TicketPriority priority
) {
}
