package com.apitracker.apitracker.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class IssueLinkTypeConverter implements AttributeConverter<IssueLinkType, String> {

    @Override
    public String convertToDatabaseColumn(IssueLinkType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public IssueLinkType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : IssueLinkType.fromValue(dbData);
    }
}
