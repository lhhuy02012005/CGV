package com.cgv.catalogservice.converter;

import com.cgv.catalogservice.enums.Format;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToFormatConverter
        implements Converter<String, Format> {

    @Override
    public Format convert(String source) {
        return Format.fromDbValue(source);
    }
}
