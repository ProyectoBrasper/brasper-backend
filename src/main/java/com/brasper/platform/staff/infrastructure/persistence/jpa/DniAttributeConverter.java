package com.brasper.platform.staff.infrastructure.persistence.jpa;

import com.brasper.platform.staff.domain.model.valueobjects.Dni;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DniAttributeConverter implements AttributeConverter<Dni, String> {
    @Override
    public String convertToDatabaseColumn(Dni dni) {
        return dni == null ? null : dni.value();
    }
    @Override
    public Dni convertToEntityAttribute(String value) {
        return value == null ? null : new Dni(value);
    }
}
