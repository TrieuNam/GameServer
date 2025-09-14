package com.SouthMillion.role_service.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.SouthMillion.dto.role.mail.MailItem;

import java.util.ArrayList;
import java.util.List;

@Converter
public class MailItemsConverter implements AttributeConverter<List<MailItem>, String> {
    @Override
    public String convertToDatabaseColumn(List<MailItem> attribute) {
        if (attribute == null) attribute = new ArrayList<>();
        return JsonUtils.toJson(attribute);
    }

    @Override
    public List<MailItem> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return new ArrayList<>();
        return JsonUtils.fromJson(dbData, new TypeReference<List<MailItem>>(){});
    }
}