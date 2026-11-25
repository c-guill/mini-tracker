package com.apitracker.apitracker.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum TicketLabel {
    FRONTEND("Frontend"),
    BACKEND("Backend"),
    BUG("Bug"),
    FEATURE("Feature"),
    UI_UX("UI/UX"),
    DEVOPS("DevOps");

    private final String value;

    TicketLabel(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TicketLabel fromValue(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(label -> label.value.equalsIgnoreCase(value) || label.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Étiquette invalide : " + value));
    }
}
