package com.apitracker.apitracker.domain;

public enum TicketStatus {
    TODO("À FAIRE"),
    IN_PROGRESS("EN COURS"),
    DONE("TERMINÉ");

    private final String frenchLabel;

    TicketStatus(String frenchLabel) {
        this.frenchLabel = frenchLabel;
    }

    public String getFrenchLabel() {
        return frenchLabel;
    }

    public TicketStatus next() {
        return switch (this) {
            case TODO -> IN_PROGRESS;
            case IN_PROGRESS -> DONE;
            case DONE -> TODO;
        };
    }
}
