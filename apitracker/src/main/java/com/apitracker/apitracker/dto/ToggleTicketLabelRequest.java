package com.apitracker.apitracker.dto;

import com.apitracker.apitracker.domain.TicketLabel;
import jakarta.validation.constraints.NotNull;

public record ToggleTicketLabelRequest(
        @NotNull(message = "L'étiquette est obligatoire")
        TicketLabel label
) {
}
