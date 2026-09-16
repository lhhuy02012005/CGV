package com.cgv.catalogservice.converter;

import com.cgv.catalogservice.enums.Amenity;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AmenityConverter
        implements AttributeConverter<Amenity, String> {

    @Override
    public String convertToDatabaseColumn(Amenity attribute) {
        if (attribute == null) {
            return null;
        }

        return attribute.getDbValue();
    }

    @Override
    public Amenity convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        return Amenity.fromDbValue(dbData);
    }
}
