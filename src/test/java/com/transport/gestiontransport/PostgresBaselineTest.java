package com.transport.gestiontransport;

import com.transport.gestiontransport.entity.*;
import com.transport.gestiontransport.entity.enums.*;
import com.transport.gestiontransport.exception.BusinessException;
import com.transport.gestiontransport.repository.*;
import com.transport.gestiontransport.service.ReservationService;
import com.transport.gestiontransport.service.PaiementService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

/** Opt-in: only an explicitly named isolated PostgreSQL validation database is allowed. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "POSTGRES_BASELINE_URL", matches = ".*/issue1_validation_[a-z0-9_]+")
class PostgresBaselineTest {
    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getenv("POSTGRES_BASELINE_URL"));
        properties.add("spring.datasource.username", () -> System.getenv("DB_USER"));
        properties.add("spring.datasource.password", () -> System.getenv("DB_PASSWORD"));
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        properties.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    AgenceRepository agences;
    @Autowired
    VehiculeRepository vehicules;
    @Autowired
    VoyageurRepository voyageurs;
    @Autowired
    TrajetRepository trajets;
    @Autowired ReservationRepository reservations;
    @Autowired ReservationService reservationService;
    @Autowired PaiementService paiementService;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired AdministrateurRepository administrateurs;

    @Test
    void agencyTypesRemainCompatibleWithLegacySqlValues() {
        Voyageur voyageur = voyageur();
        voyageur.setTypeVoyageur(TypeVoyageur.LOCAL);
        voyageurs.saveAndFlush(voyageur);
        assertThat(jdbc.queryForObject("SELECT type_membre FROM membres WHERE id = ?", String.class, voyageur.getId()))
                .isEqualTo("SITE");
        assertThat(voyageurs.findById(voyageur.getId()).orElseThrow().getTypeVoyageur()).isEqualTo(TypeVoyageur.LOCAL);
        Administrateur admin = administrateurs.saveAndFlush(Administrateur.builder()
                .matricule(UUID.randomUUID().toString()).nom("Baseline").prenom("Agency")
                .email(UUID.randomUUID() + "@example.test").passwordHash("not-used-for-login")
                .typeAdministrateur(TypeAdministrateur.AGENCE).build());
        assertThat(jdbc.queryForObject("SELECT type_administrateur FROM administrateurs WHERE id = ?", String.class, admin.getId()))
                .isEqualTo("SITE");
        assertThat(administrateurs.findById(admin.getId()).orElseThrow().getTypeAdministrateur())
                .isEqualTo(TypeAdministrateur.AGENCE);
    }

    private Voyageur voyageur() {
        return voyageurs.saveAndFlush(Voyageur.builder().matricule(UUID.randomUUID().toString())
                .nom("Baseline").prenom("Test").typeVoyageur(TypeVoyageur.VIP).solde(0.0).build());
    }

    @Test
    void postgresEnforcesUniqueVoyageurAndReservationForeignKeys() {
        Voyageur first = voyageur();
        assertThatThrownBy(() -> voyageurs.saveAndFlush(Voyageur.builder().matricule(first.getMatricule())
                .nom("Duplicate").prenom("Test").typeVoyageur(TypeVoyageur.VIP).solde(0.0).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).execute(status -> {
            Reservation invalid = Reservation.builder().voyageur(first)
                    .trajet(trajets.getReferenceById(Long.MAX_VALUE)).statut(StatutReservation.EN_ATTENTE).build();
            return reservations.saveAndFlush(invalid);
        })).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Trajet trajetVide() {
        Agence agence = agences.saveAndFlush(Agence.builder().nom("Validation Mons").adresse("Test")
                .heureOuverture(LocalTime.MIN).heureFermeture(LocalTime.MAX)
                .dureeTrajetMinutes(90).dureeEntreTrajetMinutes(15).anneeCivile(2030).build());
        Vehicule vehicule = vehicules.saveAndFlush(Vehicule.builder().nom("Voiture test").agence(agence).build());
        Voyageur chauffeur = voyageur();
        chauffeur.setPeutProposerTrajet(true);
        voyageurs.saveAndFlush(chauffeur);
        LocalDateTime debut = LocalDateTime.now().plusDays(3);
        return trajets.saveAndFlush(Trajet.builder().vehicule(vehicule).chauffeur(chauffeur)
                .villeDepart(VilleBelge.MONS).villeArrivee(VilleBelge.BRUXELLES)
                .dateDebut(debut).dateFin(debut.plusMinutes(90)).typeTrajet(TypeTrajet.PUBLIC)
                .statut(StatutTrajet.PLANIFIE).nbPassagersActuels(0).prixTotal(100.0).prixParPassager(25.0).build());
    }

    private Reservation demande(Trajet trajet) {
        Long client = voyageur().getId();
        return reservationService.create(trajet.getId(), client, client);
    }

    @Test
    void quatrePaiementsPuisAnnulationLiberentUnePlaceSansFacturerLeChauffeur() {
        Trajet trajet = trajetVide();
        List<Reservation> demandes = java.util.stream.IntStream.range(0, 5).mapToObj(i -> demande(trajet)).toList();
        assertThat(trajets.findById(trajet.getId()).orElseThrow().getNbPassagersActuels()).isZero();
        for (int i = 0; i < 4; i++) paiementService.pay(demandes.get(i).getId(), demandes.get(i).getVoyageur().getId());
        assertThat(trajets.findById(trajet.getId()).orElseThrow().getNbPassagersActuels()).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT sum(p.montant) FROM paiements p JOIN reservations r ON r.id = p.reservation_id WHERE r.match_id = ? AND p.statut = 'PAYE'",
                java.math.BigDecimal.class, trajet.getId())).isEqualByComparingTo("100.00");
        Reservation cinquieme = demandes.get(4);
        assertThatThrownBy(() -> paiementService.pay(cinquieme.getId(), cinquieme.getVoyageur().getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("complet");
        assertThat(reservations.findById(cinquieme.getId()).orElseThrow().getStatut()).isEqualTo(StatutReservation.EN_ATTENTE);
        reservationService.cancel(demandes.get(0).getId());
        assertThat(trajets.findById(trajet.getId()).orElseThrow().getNbPassagersActuels()).isEqualTo(3);
        paiementService.pay(cinquieme.getId(), cinquieme.getVoyageur().getId());
        assertThat(trajets.findById(trajet.getId()).orElseThrow().getNbPassagersActuels()).isEqualTo(4);
        assertThat(reservations.findByTrajetId(trajet.getId())).hasSize(5);
        assertThat(reservations.existsByTrajetIdAndVoyageurId(trajet.getId(), trajet.getChauffeur().getId())).isFalse();
    }

    @Test
    void uneDemandePostgresExpireSansModifierLesPaiementsHistoriques() {
        Trajet trajet = trajetVide();
        Reservation r = demande(trajet);
        jdbc.update("UPDATE reservations SET created_at = ? WHERE id = ?",
                java.sql.Timestamp.valueOf(LocalDateTime.now().minusHours(1)), r.getId());
        reservationService.expirePendingReservations();
        assertThat(reservations.findById(r.getId()).orElseThrow().getStatut()).isEqualTo(StatutReservation.EXPIREE);
        assertThat(trajets.findById(trajet.getId()).orElseThrow().getNbPassagersActuels()).isZero();
        assertThat(jdbc.queryForObject("SELECT statut FROM paiements WHERE reservation_id = ?", String.class, r.getId()))
                .isEqualTo("ANNULE");
        assertThat(voyageurs.findById(trajet.getChauffeur().getId()).orElseThrow().getSolde()).isZero();
    }

    @Test
    void concurrentReservationsMustNotSellTheLastPlaceTwice() throws Exception {
        Trajet trajet = trajetVide();
        for (int i = 0; i < 3; i++) {
            Reservation r = demande(trajet);
            paiementService.pay(r.getId(), r.getVoyageur().getId());
        }
        Reservation premier = demande(trajet);
        Reservation second = demande(trajet);
        CyclicBarrier readBarrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> a = executor.submit(() -> payer(trajet.getId(), premier, readBarrier));
            Future<Boolean> b = executor.submit(() -> payer(trajet.getId(), second, readBarrier));
            int successes = (a.get(30, TimeUnit.SECONDS) ? 1 : 0) + (b.get(30, TimeUnit.SECONDS) ? 1 : 0);
            assertThat(successes).as("un seul paiement peut confirmer la dernière place").isEqualTo(1);
            assertThat(reservations.findByTrajetId(trajet.getId()).stream()
                    .filter(r -> r.getStatut() == StatutReservation.CONFIRMEE)).hasSize(4);
            assertThat(trajets.findById(trajet.getId()).orElseThrow().getNbPassagersActuels()).isEqualTo(4);
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean payer(Long trajetId, Reservation demande, CyclicBarrier barrier) {
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                trajets.findById(trajetId).orElseThrow();
                try { barrier.await(10, TimeUnit.SECONDS); }
                catch (Exception ex) { throw new IllegalStateException(ex); }
                paiementService.pay(demande.getId(), demande.getVoyageur().getId());
            });
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }
}
