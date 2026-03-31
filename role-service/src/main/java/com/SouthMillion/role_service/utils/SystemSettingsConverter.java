package com.SouthMillion.role_service.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.SouthMillion.dto.role.settings.SystemSettings;

@Converter
public class SystemSettingsConverter implements AttributeConverter<SystemSettings, String> {
    @Override
    public String convertToDatabaseColumn(SystemSettings attribute) {
        if (attribute == null) attribute = SystemSettings.defaults();
        return JsonUtils.toJson(attribute);
    }

    @Override
    public SystemSettings convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return SystemSettings.defaults();
        return JsonUtils.fromJson(dbData, SystemSettings.class);
    }
}