package com.transport.gestiontransport.entity.enums;

public enum StatutReservation {
    EN_ATTENTE,   // place réservée, paiement pas encore effectué
    CONFIRMEE,    // paiement effectué
    ANNULEE,      // place libérée
    EXPIREE       // demande non payée après son échéance
}
