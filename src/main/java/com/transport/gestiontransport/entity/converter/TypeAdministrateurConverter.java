package com.transport.gestiontransport.entity.converter;

import com.transport.gestiontransport.entity.enums.TypeAdministrateur;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Compatibility with existing agency-administrator records and SQL constraints. */
@Converter
public class TypeAdministrateurConverter implements AttributeConverter<TypeAdministrateur, String> {
    @Override
    public String convertToDatabaseColumn(TypeAdministrateur type) {
        return type == null ? null : type == TypeAdministrateur.AGENCE ? "SITE" : type.name();
    }

    @Override
    public TypeAdministrateur convertToEntityAttribute(String value) {
        return value == null ? null : "SITE".equals(value) ? TypeAdministrateur.AGENCE : TypeAdministrateur.valueOf(value);
    }
}
