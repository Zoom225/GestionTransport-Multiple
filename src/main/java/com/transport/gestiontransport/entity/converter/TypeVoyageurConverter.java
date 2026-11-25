package com.transport.gestiontransport.entity.converter;

import com.transport.gestiontransport.entity.enums.TypeVoyageur;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Exposes VIP/LOCAL while preserving the existing GLOBAL/SITE SQL values. */
@Converter
public class TypeVoyageurConverter implements AttributeConverter<TypeVoyageur, String> {
    @Override
    public String convertToDatabaseColumn(TypeVoyageur type) {
        if (type == null) return null;
        return switch (type) {
            case VIP -> "GLOBAL";
            case LOCAL -> "SITE";
            case LIBRE -> "LIBRE";
        };
    }

    @Override
    public TypeVoyageur convertToEntityAttribute(String value) {
        if (value == null) return null;
        return switch (value) {
            case "GLOBAL" -> TypeVoyageur.VIP;
            case "SITE", "AGENCE" -> TypeVoyageur.LOCAL;
            default -> TypeVoyageur.valueOf(value);
        };
    }
}
