package com.apitracker.apitracker.dto;

import com.apitracker.apitracker.domain.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTicketStatusRequest(
        @NotNull(message = "Le statut est obligatoire")
        TicketStatus status
) {
}
