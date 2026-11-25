package com.apitracker.apitracker.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TicketLabelConverter implements AttributeConverter<TicketLabel, String> {

    @Override
    public String convertToDatabaseColumn(TicketLabel attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public TicketLabel convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TicketLabel.fromValue(dbData);
    }
}
