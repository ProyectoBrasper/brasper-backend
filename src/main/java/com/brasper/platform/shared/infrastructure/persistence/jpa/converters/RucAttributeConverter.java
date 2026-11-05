package com.brasper.platform.shared.infrastructure.persistence.jpa.converters;

import com.brasper.platform.shared.domain.model.valueobjects.Ruc;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RucAttributeConverter implements AttributeConverter<Ruc, String> {

    @Override
    public String convertToDatabaseColumn(Ruc ruc) {
        return ruc == null ? null : ruc.value();
    }

    @Override
    public Ruc convertToEntityAttribute(String value) {
        return value == null ? null : new Ruc(value);
    }
}
