package com.cgv.catalogservice.converter;

import com.cgv.catalogservice.enums.Format;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class FormatConverter implements AttributeConverter<Format, String> {

    @Override
    public String convertToDatabaseColumn(Format attribute) {
        if (attribute == null) {
            return null;
        }

        return attribute.getDbValue();
    }

    @Override
    public Format convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        return Format.fromDbValue(dbData);
    }
}
