package com.brasper.platform.staff.infrastructure.persistence.jpa;

import com.brasper.platform.staff.domain.model.valueobjects.CostCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
@Converter(autoApply = true)
public class CostCodeAttributeConverter implements AttributeConverter<CostCode, String> {
    @Override public String convertToDatabaseColumn(CostCode code) { return code == null ? null : code.value(); }
    @Override public CostCode convertToEntityAttribute(String value) { return value == null ? null : new CostCode(value); }
}
