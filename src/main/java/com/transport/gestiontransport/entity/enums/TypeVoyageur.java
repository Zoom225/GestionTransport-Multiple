package com.transport.gestiontransport.entity.enums;

public enum TypeVoyageur {
    GLOBAL,   // Gxxxx — toutes les agences, 3 semaines avant
    AGENCE,     // Sxxxxx — son agence uniquement, 2 semaines avant
    LIBRE     // Lxxxxx — toutes les agences, 5 jours avant
}
