package com.transport.gestiontransport.dto.response;

import com.transport.gestiontransport.entity.enums.StatutTrajet;
import com.transport.gestiontransport.entity.enums.TypeTrajet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record TrajetDto(
        Long id,
        Long vehiculeId,
        String vehiculeNom,
        String agenceNom,
        Long organisateurId,
        String organisateurNom,
        LocalDate date,
        LocalTime heureDebut,
        LocalTime heureFin,
        TypeTrajet typeTrajet,
        StatutTrajet statut,
        Integer nbPassagersActuels,
        Double prixParPassager,
        LocalDateTime dateConversionPublic
) {}
