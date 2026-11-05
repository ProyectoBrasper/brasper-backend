package com.brasper.platform.shared.infrastructure.persistence.jpa.converters;

import com.brasper.platform.shared.domain.model.valueobjects.Percentage;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;

/** Stored as the fraction (18% = 0.1800), so columns are numeric(5,4). */
@Converter(autoApply = true)
public class PercentageAttributeConverter implements AttributeConverter<Percentage, BigDecimal> {

    @Override
    public BigDecimal convertToDatabaseColumn(Percentage percentage) {
        return percentage == null ? null : percentage.value();
    }

    @Override
    public Percentage convertToEntityAttribute(BigDecimal value) {
        return value == null ? null : new Percentage(value);
    }
}
