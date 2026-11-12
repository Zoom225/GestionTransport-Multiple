package com.transport.gestiontransport.repository;

import com.transport.gestiontransport.config.PersistenceConfig;
import com.transport.gestiontransport.entity.*;
import com.transport.gestiontransport.entity.enums.*;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import(PersistenceConfig.class)
class CovoituragePersistenceTest {
    @Autowired VoyageurRepository voyageurs;
    @Autowired AgenceRepository agences;
    @Autowired VehiculeRepository vehicules;
    @Autowired TrajetRepository trajets;
    @Autowired JdbcTemplate jdbc;

    private Trajet trajet() {
        Agence agence = agences.saveAndFlush(Agence.builder().nom("Mons")
                .adresse("Mons").heureOuverture(LocalTime.MIN).heureFermeture(LocalTime.MAX)
                .dureeTrajetMinutes(60).dureeEntreTrajetMinutes(15).anneeCivile(2026).build());
        Vehicule vehicule = vehicules.saveAndFlush(Vehicule.builder().nom("Voiture").agence(agence).build());
        Voyageur chauffeur = voyageurs.saveAndFlush(Voyageur.builder().matricule(UUID.randomUUID().toString())
                .nom("Test").prenom("Chauffeur").typeVoyageur(TypeVoyageur.VIP)
                .peutProposerTrajet(true).solde(0.0).build());
        LocalDateTime depart = LocalDateTime.now().plusDays(1);
        return Trajet.builder().chauffeur(chauffeur).vehicule(vehicule)
                .villeDepart(VilleBelge.MONS).villeArrivee(VilleBelge.BRUXELLES)
                .dateDebut(depart).dateFin(depart.plusHours(1)).typeTrajet(TypeTrajet.PUBLIC)
                .statut(StatutTrajet.PLANIFIE).nbPassagersActuels(0).prixTotal(100.0).prixParPassager(25.0).build();
    }

    @Test
    void storesChauffeurCitiesAndLegacyCategoryWithoutAddingPassenger() {
        Trajet saved = trajets.saveAndFlush(trajet());
        assertThat(saved.getChauffeur()).isNotNull();
        assertThat(saved.getVilleDepart()).isEqualTo(VilleBelge.MONS);
        assertThat(saved.getVilleArrivee()).isEqualTo(VilleBelge.BRUXELLES);
        assertThat(saved.getNbPassagersActuels()).isZero();
        assertThat(saved.isPublicationAutomatiqueAutorisee()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(jdbc.queryForObject("SELECT type_membre FROM membres WHERE id=?", String.class,
                saved.getChauffeur().getId())).isEqualTo("GLOBAL");
        assertThat(trajets.existsChauffeurOverlap(saved.getChauffeur().getId(), saved.getDateDebut(), saved.getDateFin(), 0L)).isTrue();
    }

    @Test
    void refusesMissingChauffeur() {
        Trajet invalid = trajet();
        invalid.setChauffeur(null);
        assertThatThrownBy(() -> trajets.saveAndFlush(invalid)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void refusesCounterAboveFourClients() {
        Trajet invalid = trajet();
        invalid.setNbPassagersActuels(5);
        assertThatThrownBy(() -> trajets.saveAndFlush(invalid)).isInstanceOf(ConstraintViolationException.class);
    }
}
