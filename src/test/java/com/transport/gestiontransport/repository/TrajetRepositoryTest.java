package com.transport.gestiontransport.repository;

import com.transport.gestiontransport.entity.Trajet;
import com.transport.gestiontransport.entity.Voyageur;
import com.transport.gestiontransport.entity.Agence;
import com.transport.gestiontransport.entity.Vehicule;
import com.transport.gestiontransport.entity.enums.StatutTrajet;
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
@DisplayName("Tests de TrajetRepository")
class TrajetRepositoryTest {

    @Autowired
    private TrajetRepository trajetRepository;

    @Autowired
    private AgenceRepository agenceRepository;

    @Autowired
    private VehiculeRepository vehiculeRepository;

    @Autowired
    private VoyageurRepository voyageurRepository;

    @Test
    void listeLesTrajetsDuVraiOrganisateurEtConserveLeRepliHistorique() {
        Vehicule voiture = vehiculeRepository.save(Vehicule.builder().nom("Demo")
                .agence(agenceRepository.save(agence())).build());
        Voyageur chauffeur = voyageurRepository.save(voyageur("V1001"));
        Voyageur client = voyageurRepository.save(voyageur("F10002"));
        Trajet nouveau = trajet(voiture, chauffeur, LocalDateTime.now().plusDays(1), StatutTrajet.PLANIFIE);
        nouveau.setOrganisateur(client);
        trajetRepository.save(nouveau);
        Trajet historique = trajetRepository.save(trajet(voiture, chauffeur, LocalDateTime.now().plusDays(2), StatutTrajet.PLANIFIE));
        assertThat(trajetRepository.findOrganisesPar(client.getId())).containsExactly(nouveau);
        assertThat(trajetRepository.findOrganisesPar(chauffeur.getId())).containsExactly(historique);
    }

    @Test
    @DisplayName("findOverlappingTrajets retourne les trajets planifiés qui chevauchent le créneau")
    void shouldFindOverlappingTrajets() {
        Vehicule vehicule = vehiculeRepository.save(Vehicule.builder()
                .nom("Peugeot 308")
                .agence(agenceRepository.save(agence()))
                .prix(60.0)
                .build());
        Voyageur organisateur = voyageurRepository.save(voyageur("G1001"));

        LocalDateTime start = LocalDateTime.of(2026, 7, 10, 10, 0);
        trajetRepository.save(trajet(vehicule, organisateur, start, StatutTrajet.PLANIFIE));

        assertThat(trajetRepository.findOverlappingTrajets(
                vehicule.getId(),
                start.plusMinutes(30),
                start.plusMinutes(120),
                StatutTrajet.ANNULE
        )).hasSize(1);
    }

    @Test
    @DisplayName("findOverlappingTrajets ignore les trajets annulés")
    void shouldIgnoreCancelledTrajets() {
        Vehicule vehicule = vehiculeRepository.save(Vehicule.builder()
                .nom("Peugeot 308")
                .agence(agenceRepository.save(agence()))
                .prix(60.0)
                .build());
        Voyageur organisateur = voyageurRepository.save(voyageur("G1001"));

        LocalDateTime start = LocalDateTime.of(2026, 7, 10, 10, 0);
        trajetRepository.save(trajet(vehicule, organisateur, start, StatutTrajet.ANNULE));

        assertThat(trajetRepository.findOverlappingTrajets(
                vehicule.getId(),
                start.plusMinutes(30),
                start.plusMinutes(120),
                StatutTrajet.ANNULE
        )).isEmpty();
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

    private Trajet trajet(Vehicule vehicule, Voyageur organisateur, LocalDateTime start, StatutTrajet statut) {
        return Trajet.builder()
                .vehicule(vehicule)
                .chauffeur(organisateur)
                .dateDebut(start)
                .dateFin(start.plusMinutes(90))
                .typeTrajet(TypeTrajet.PUBLIC)
                .statut(statut)
                .nbPassagersActuels(1)
                .prixTotal(60.0)
                .prixParPassager(15.0)
                .build();
    }
}
