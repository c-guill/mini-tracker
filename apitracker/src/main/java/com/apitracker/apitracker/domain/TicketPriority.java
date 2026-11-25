package com.apitracker.apitracker.domain;

public enum TicketPriority {
    LOW("Basse"),
    HIGH("Haute");

    private final String frenchLabel;

    TicketPriority(String frenchLabel) {
        this.frenchLabel = frenchLabel;
    }

    public String getFrenchLabel() {
        return frenchLabel;
    }
}
