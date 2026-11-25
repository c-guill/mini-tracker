package com.apitracker.apitracker.dto;

import jakarta.validation.constraints.Pattern;

public record UpdateTicketDueDateRequest(
        @Pattern(regexp = "^(|\\d{4}-\\d{2}-\\d{2})$", message = "La date d'échéance doit être au format YYYY-MM-DD")
        String dueDate
) {
}
