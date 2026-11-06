package com.brasper.platform.treasury.infrastructure.persistence.jpa.converters;

import com.brasper.platform.treasury.domain.model.valueobjects.AccountNumber;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AccountNumberAttributeConverter implements AttributeConverter<AccountNumber, String> {

    @Override
    public String convertToDatabaseColumn(AccountNumber number) {
        return number == null ? null : number.value();
    }

    @Override
    public AccountNumber convertToEntityAttribute(String value) {
        return value == null ? null : new AccountNumber(value);
    }
}
