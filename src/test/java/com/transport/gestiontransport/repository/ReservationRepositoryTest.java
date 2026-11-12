package com.transport.gestiontransport.repository;

import com.transport.gestiontransport.entity.*;
import com.transport.gestiontransport.entity.enums.StatutTrajet;
import com.transport.gestiontransport.entity.enums.StatutReservation;
import com.transport.gestiontransport.entity.enums.TypeTrajet;
import com.transport.gestiontransport.entity.enums.TypeVoyageur;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("Tests de ReservationRepository")
class ReservationRepositoryTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private TrajetRepository trajetRepository;

    @Autowired
    private VoyageurRepository voyageurRepository;

    @Autowired
    private AgenceRepository agenceRepository;

    @Autowired
    private VehiculeRepository vehiculeRepository;

    @Test
    @DisplayName("existsByTrajetIdAndVoyageurId détecte un voyageur déjà inscrit")
    void shouldFindExistingReservationForVoyageurAndTrajet() {
        Agence agence = agenceRepository.save(agence());
        Vehicule vehicule = vehiculeRepository.save(Vehicule.builder().nom("Court A").agence(agence).prix(60.0).build());
        Voyageur organisateur = voyageurRepository.save(voyageur("G1001"));
        Voyageur passager = voyageurRepository.save(voyageur("G1002"));
        Trajet trajet = trajetRepository.save(trajet(vehicule, organisateur));

        reservationRepository.save(Reservation.builder()
                .trajet(trajet)
                .voyageur(passager)
                .statut(StatutReservation.EN_ATTENTE)
                .build());

        assertThat(reservationRepository.existsByTrajetIdAndVoyageurId(trajet.getId(), passager.getId())).isTrue();
    }

    private Agence agence() {
        return Agence.builder()
                .nom("Agence Lyon")
                .adresse("12 rue de la République")
                .heureOuverture(LocalTime.of(8, 0))
                .heureFermeture(LocalTime.of(22, 0))
                .dureeTrajetMinutes(90)
                .dureeEntreTrajetMinutes(15)
                .anneeCivile(2026)
                .build();
    }

    private Voyageur voyageur(String matricule) {
        return Voyageur.builder()
                .matricule(matricule)
                .nom("Martin")
                .prenom("Lucas")
                .email(matricule.toLowerCase() + "@example.com")
                .typeVoyageur(TypeVoyageur.VIP)
                .solde(0.0)
                .build();
    }

    private Trajet trajet(Vehicule vehicule, Voyageur organisateur) {
        LocalDateTime start = LocalDateTime.of(2026, 7, 10, 10, 0);
        return Trajet.builder()
                .vehicule(vehicule)
                .chauffeur(organisateur)
                .dateDebut(start)
                .dateFin(start.plusMinutes(90))
                .typeTrajet(TypeTrajet.PUBLIC)
                .statut(StatutTrajet.PLANIFIE)
                .nbPassagersActuels(1)
                .prixTotal(60.0)
                .prixParPassager(15.0)
                .build();
    }
}
