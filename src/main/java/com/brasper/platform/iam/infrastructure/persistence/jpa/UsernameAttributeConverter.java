package com.brasper.platform.iam.infrastructure.persistence.jpa;

import com.brasper.platform.iam.domain.model.valueobjects.Username;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UsernameAttributeConverter implements AttributeConverter<Username, String> {

    @Override
    public String convertToDatabaseColumn(Username username) {
        return username == null ? null : username.value();
    }

    @Override
    public Username convertToEntityAttribute(String value) {
        return value == null ? null : new Username(value);
    }
}
