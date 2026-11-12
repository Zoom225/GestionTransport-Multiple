package com.transport.gestiontransport.repository;

import com.transport.gestiontransport.entity.Trajet;
import com.transport.gestiontransport.entity.enums.StatutTrajet;
import com.transport.gestiontransport.entity.enums.TypeTrajet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrajetRepository extends JpaRepository<Trajet, Long> {

    List<Trajet> findByTypeTrajetAndStatut(TypeTrajet type, StatutTrajet statut);

    List<Trajet> findByVehiculeAgenceId(Long agenceId);

    List<Trajet> findByChauffeurId(Long organisateurId);

    @Query("SELECT count(t) > 0 FROM Trajet t WHERE t.chauffeur.id = :chauffeurId " +
            "AND t.statut <> com.transport.gestiontransport.entity.enums.StatutTrajet.ANNULE " +
            "AND t.dateDebut < :fin AND t.dateFin > :debut")
    boolean existsChauffeurOverlap(@Param("chauffeurId") Long chauffeurId,
            @Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    List<Trajet> findByDateDebutBetweenAndStatut(LocalDateTime start, LocalDateTime end, StatutTrajet statut);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Trajet m WHERE m.id = :id")
    Optional<Trajet> findByIdForUpdate(@Param("id") Long id);

    /**
     * Trouve les trajets sur un vehicule donné qui chevauchent un créneau horaire spécifié,
     * en excluant un certain statut (typiquement ANNULE).
     *
     * Un chevauchement se produit si :
     * (startA < endB) et (endA > startB)
     *
     * @param vehiculeId L'ID du vehicule.
     * @param start     La date et l'heure de début du créneau à vérifier.
     * @param end       La date et l'heure de fin du créneau à vérifier.
     * @param statut    Le statut à exclure de la recherche.
     * @return Une liste de trajets qui se chevauchent.
     */
    @Query("SELECT m FROM Trajet m WHERE m.vehicule.id = :vehiculeId " +
           "AND m.statut <> :statut " +
           "AND m.dateDebut < :end " +
           "AND m.dateFin > :start")
    List<Trajet> findOverlappingTrajets(
            @Param("vehiculeId") Long vehiculeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("statut") StatutTrajet statut
    );
}
