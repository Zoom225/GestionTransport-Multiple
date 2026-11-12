package com.transport.gestiontransport;

import com.transport.gestiontransport.entity.*;
import com.transport.gestiontransport.entity.enums.*;
import com.transport.gestiontransport.exception.BusinessException;
import com.transport.gestiontransport.repository.*;
import com.transport.gestiontransport.service.ReservationService;
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

    @Test
    void concurrentReservationsMustNotSellTheLastPlaceTwice() throws Exception {
        Agence agence = agences.saveAndFlush(Agence.builder().nom("Baseline").adresse("Test")
                .heureOuverture(LocalTime.MIN).heureFermeture(LocalTime.MAX)
                .dureeTrajetMinutes(90).dureeEntreTrajetMinutes(15).anneeCivile(2030).build());
        Vehicule vehicule = vehicules.saveAndFlush(Vehicule.builder().nom("Test").agence(agence).prix(60.0).build());
        Voyageur owner = voyageur();
        LocalDateTime start = LocalDateTime.now().plusDays(10);
        Trajet trajet = trajets.saveAndFlush(Trajet.builder().vehicule(vehicule).chauffeur(owner)
                .dateDebut(start).dateFin(start.plusMinutes(90)).typeTrajet(TypeTrajet.PUBLIC)
                .statut(StatutTrajet.PLANIFIE).nbPassagersActuels(3).prixTotal(60.0).prixParPassager(15.0).build());
        Long first = voyageur().getId();
        Long second = voyageur().getId();
        // Two real transactions have read the same last-place state before either writes.
        CyclicBarrier readBarrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> a = executor.submit(() -> reserve(trajet.getId(), first, readBarrier));
            Future<Boolean> b = executor.submit(() -> reserve(trajet.getId(), second, readBarrier));
            int successes = (a.get(30, TimeUnit.SECONDS) ? 1 : 0) + (b.get(30, TimeUnit.SECONDS) ? 1 : 0);
            assertThat(successes).as("only one request can reserve the last available place").isEqualTo(1);
            assertThat(reservations.findByTrajetId(trajet.getId())).hasSize(1);
            assertThat(trajets.findById(trajet.getId()).orElseThrow().getNbPassagersActuels()).isEqualTo(4);
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean reserve(Long trajetId, Long voyageurId, CyclicBarrier barrier) {
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                trajets.findById(trajetId).orElseThrow();
                try { barrier.await(10, TimeUnit.SECONDS); }
                catch (Exception ex) { throw new IllegalStateException(ex); }
                reservationService.create(trajetId, voyageurId, voyageurId);
            });
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }
}
