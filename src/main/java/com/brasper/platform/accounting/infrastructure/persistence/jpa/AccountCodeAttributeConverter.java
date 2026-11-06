package com.brasper.platform.accounting.infrastructure.persistence.jpa;

import com.brasper.platform.accounting.domain.model.valueobjects.AccountCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AccountCodeAttributeConverter implements AttributeConverter<AccountCode, String> {
    @Override
    public String convertToDatabaseColumn(AccountCode code) {
        return code == null ? null : code.value();
    }
    @Override
    public AccountCode convertToEntityAttribute(String value) {
        return value == null ? null : new AccountCode(value);
    }
}
