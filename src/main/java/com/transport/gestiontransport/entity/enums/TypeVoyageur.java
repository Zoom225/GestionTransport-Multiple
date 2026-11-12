package com.transport.gestiontransport.entity.enums;

public enum TypeVoyageur {
    VIP,      // Gxxxx — toutes les agences, 21 jours avant
    LOCAL,    // Sxxxxx — son agence uniquement, 14 jours avant
    LIBRE     // Lxxxxx — toutes les agences, 5 jours avant
}
