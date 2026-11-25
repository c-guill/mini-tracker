package com.apitracker.apitracker.dto;

public record UserResponse(
        String id,
        String name,
        String initials,
        String color
) {
}
