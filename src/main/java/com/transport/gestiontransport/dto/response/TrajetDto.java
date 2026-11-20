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
        LocalDateTime dateConversionPublic,
        com.transport.gestiontransport.entity.enums.VilleBelge villeDepart,
        com.transport.gestiontransport.entity.enums.VilleBelge villeArrivee,
        Long agenceId,
        Long chauffeurId,
        String chauffeurNom,
        LocalDateTime penaliteImputeeLe,
        LocalDateTime penalitePayeeLe
) {
    // Backward-compatible constructor for historical trips and existing clients.
    public TrajetDto(Long id, Long vehiculeId, String vehiculeNom, String agenceNom,
            Long organisateurId, String organisateurNom, LocalDate date, LocalTime heureDebut,
            LocalTime heureFin, TypeTrajet typeTrajet, StatutTrajet statut, Integer nbPassagersActuels,
            Double prixParPassager, LocalDateTime dateConversionPublic,
            com.transport.gestiontransport.entity.enums.VilleBelge villeDepart,
            com.transport.gestiontransport.entity.enums.VilleBelge villeArrivee, Long agenceId) {
        this(id, vehiculeId, vehiculeNom, agenceNom, organisateurId, organisateurNom, date, heureDebut,
                heureFin, typeTrajet, statut, nbPassagersActuels, prixParPassager, dateConversionPublic,
                villeDepart, villeArrivee, agenceId, organisateurId, organisateurNom, null, null);
    }
    public TrajetDto(Long id, Long vehiculeId, String vehiculeNom, String agenceNom,
            Long organisateurId, String organisateurNom, LocalDate date, LocalTime heureDebut,
            LocalTime heureFin, TypeTrajet typeTrajet, StatutTrajet statut, Integer nbPassagersActuels,
            Double prixParPassager, LocalDateTime dateConversionPublic) {
        this(id, vehiculeId, vehiculeNom, agenceNom, organisateurId, organisateurNom, date, heureDebut,
                heureFin, typeTrajet, statut, nbPassagersActuels, prixParPassager, dateConversionPublic, null, null, null);
    }

    @com.fasterxml.jackson.annotation.JsonProperty
    public Long chauffeurId() { return chauffeurId; }

    @com.fasterxml.jackson.annotation.JsonProperty
    public String chauffeurNom() { return chauffeurNom; }

    @com.fasterxml.jackson.annotation.JsonProperty
    public int placesRestantes() { return com.transport.gestiontransport.entity.Trajet.MAX_CLIENTS - (nbPassagersActuels == null ? 0 : nbPassagersActuels); }

    @com.fasterxml.jackson.annotation.JsonProperty
    public int placesMaximum() { return com.transport.gestiontransport.entity.Trajet.MAX_CLIENTS; }
}
