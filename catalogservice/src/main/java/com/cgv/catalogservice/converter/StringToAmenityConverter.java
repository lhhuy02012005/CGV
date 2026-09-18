package com.cgv.catalogservice.converter;

import com.cgv.catalogservice.enums.Amenity;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToAmenityConverter
        implements Converter<String, Amenity> {

    @Override
    public Amenity convert(String source) {
        return Amenity.fromDbValue(source);
    }
}
