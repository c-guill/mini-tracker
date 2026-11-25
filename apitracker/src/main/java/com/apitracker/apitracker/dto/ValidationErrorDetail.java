package com.apitracker.apitracker.dto;

public record ValidationErrorDetail(
        String field,
        String message
) {
}
