package com.brasper.platform.projects.infrastructure.persistence.jpa;

import com.brasper.platform.projects.domain.model.valueobjects.ProjectCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProjectCodeAttributeConverter implements AttributeConverter<ProjectCode, String> {

    @Override
    public String convertToDatabaseColumn(ProjectCode code) {
        return code == null ? null : code.value();
    }

    @Override
    public ProjectCode convertToEntityAttribute(String value) {
        return value == null ? null : new ProjectCode(value);
    }
}
