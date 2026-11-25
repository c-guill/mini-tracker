package com.apitracker.apitracker.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum IssueLinkType {
    BLOCKS("blocks", "bloque"),
    IS_BLOCKED_BY("is_blocked_by", "est bloqué par"),
    RELATES_TO("relates_to", "est lié à");

    private final String value;
    private final String frenchLabel;

    IssueLinkType(String value, String frenchLabel) {
        this.value = value;
        this.frenchLabel = frenchLabel;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getFrenchLabel() {
        return frenchLabel;
    }

    public IssueLinkType mirror() {
        return switch (this) {
            case BLOCKS -> IS_BLOCKED_BY;
            case IS_BLOCKED_BY -> BLOCKS;
            case RELATES_TO -> RELATES_TO;
        };
    }

    @JsonCreator
    public static IssueLinkType fromValue(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(type -> type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Type de lien invalide : " + value));
    }
}
