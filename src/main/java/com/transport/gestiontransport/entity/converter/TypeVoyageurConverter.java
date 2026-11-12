package com.transport.gestiontransport.entity.converter;

import com.transport.gestiontransport.entity.enums.TypeVoyageur;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Retains the legacy SQL value while exposing AGENCE in Java and JSON. */
@Converter
public class TypeVoyageurConverter implements AttributeConverter<TypeVoyageur, String> {
    @Override
    public String convertToDatabaseColumn(TypeVoyageur type) {
        return type == null ? null : type == TypeVoyageur.AGENCE ? "SITE" : type.name();
    }

    @Override
    public TypeVoyageur convertToEntityAttribute(String value) {
        return value == null ? null : "SITE".equals(value) ? TypeVoyageur.AGENCE : TypeVoyageur.valueOf(value);
    }
}
