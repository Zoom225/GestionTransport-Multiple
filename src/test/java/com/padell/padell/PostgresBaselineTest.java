package com.padell.padell;

import com.padell.padell.entity.*;
import com.padell.padell.entity.enums.*;
import com.padell.padell.exception.BusinessException;
import com.padell.padell.repository.*;
import com.padell.padell.service.ReservationService;
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

    @Autowired SiteRepository sites;
    @Autowired TerrainRepository terrains;
    @Autowired MembreRepository membres;
    @Autowired MatchRepository matches;
    @Autowired ReservationRepository reservations;
    @Autowired ReservationService reservationService;
    @Autowired PlatformTransactionManager transactionManager;

    private Membre member() {
        return membres.saveAndFlush(Membre.builder().matricule(UUID.randomUUID().toString())
                .nom("Baseline").prenom("Test").typeMembre(TypeMembre.GLOBAL).solde(0.0).build());
    }

    @Test
    void postgresEnforcesUniqueMemberAndReservationForeignKeys() {
        Membre first = member();
        assertThatThrownBy(() -> membres.saveAndFlush(Membre.builder().matricule(first.getMatricule())
                .nom("Duplicate").prenom("Test").typeMembre(TypeMembre.GLOBAL).solde(0.0).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).execute(status -> {
            Reservation invalid = Reservation.builder().membre(first)
                    .match(matches.getReferenceById(Long.MAX_VALUE)).statut(StatutReservation.EN_ATTENTE).build();
            return reservations.saveAndFlush(invalid);
        })).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void concurrentReservationsMustNotSellTheLastPlaceTwice() throws Exception {
        Site site = sites.saveAndFlush(Site.builder().nom("Baseline").adresse("Test")
                .heureOuverture(LocalTime.MIN).heureFermeture(LocalTime.MAX)
                .dureeMatchMinutes(90).dureeEntreMatchMinutes(15).anneeCivile(2030).build());
        Terrain terrain = terrains.saveAndFlush(Terrain.builder().nom("Test").site(site).prix(60.0).build());
        Membre owner = member();
        LocalDateTime start = LocalDateTime.now().plusDays(10);
        Match match = matches.saveAndFlush(Match.builder().terrain(terrain).organisateur(owner)
                .dateDebut(start).dateFin(start.plusMinutes(90)).typeMatch(TypeMatch.PUBLIC)
                .statut(StatutMatch.PLANIFIE).nbJoueursActuels(3).prixTotal(60.0).prixParJoueur(15.0).build());
        Long first = member().getId();
        Long second = member().getId();
        // Two real transactions have read the same last-place state before either writes.
        CyclicBarrier readBarrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> a = executor.submit(() -> reserve(match.getId(), first, readBarrier));
            Future<Boolean> b = executor.submit(() -> reserve(match.getId(), second, readBarrier));
            int successes = (a.get(30, TimeUnit.SECONDS) ? 1 : 0) + (b.get(30, TimeUnit.SECONDS) ? 1 : 0);
            assertThat(successes).as("only one request can reserve the last available place").isEqualTo(1);
            assertThat(reservations.findByMatchId(match.getId())).hasSize(1);
            assertThat(matches.findById(match.getId()).orElseThrow().getNbJoueursActuels()).isEqualTo(4);
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean reserve(Long matchId, Long memberId, CyclicBarrier barrier) {
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                matches.findById(matchId).orElseThrow();
                try { barrier.await(10, TimeUnit.SECONDS); }
                catch (Exception ex) { throw new IllegalStateException(ex); }
                reservationService.create(matchId, memberId, memberId);
            });
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }
}
