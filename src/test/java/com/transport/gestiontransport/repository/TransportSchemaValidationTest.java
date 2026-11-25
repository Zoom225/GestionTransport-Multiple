package com.transport.gestiontransport.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/** Lecture seule d'une copie restaurée et migrée ; Hibernate doit valider sans créer de tables. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfEnvironmentVariable(named = "TRANSPORT_SCHEMA_URL", matches = ".*/transport_schema_copy_[a-z0-9_]+")
class TransportSchemaValidationTest {
    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getenv("TRANSPORT_SCHEMA_URL"));
        properties.add("spring.datasource.username", () -> System.getenv("DB_USER"));
        properties.add("spring.datasource.password", () -> System.getenv("DB_PASSWORD"));
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        properties.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired TrajetRepository trajets;
    @Autowired VoyageurRepository voyageurs;
    @Autowired ReservationRepository reservations;
    @Autowired PaiementRepository paiements;
    @Autowired RestrictionRepository restrictions;

    @Test
    void lesMappingsTransportLisEntLesDonneesRestaureesSansTablesParalleles() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_tables WHERE schemaname='public' AND tablename IN ('sites','terrains','matches','membres','penalites','jours_fermeture')", Long.class)).isZero();
        assertThat(trajets.count()).isEqualTo(jdbc.queryForObject("SELECT count(*) FROM trajets", Long.class));
        assertThat(voyageurs.count()).isEqualTo(jdbc.queryForObject("SELECT count(*) FROM voyageurs", Long.class));
        assertThat(reservations.count()).isEqualTo(jdbc.queryForObject("SELECT count(*) FROM reservations", Long.class));
        assertThat(paiements.count()).isEqualTo(jdbc.queryForObject("SELECT count(*) FROM paiements", Long.class));
        assertThat(restrictions.count()).isEqualTo(jdbc.queryForObject("SELECT count(*) FROM restrictions", Long.class));
    }

    @Test
    void lesRelationsChauffeurVehiculeReservationEtPaiementRestentLisibles() {
        trajets.findAll().forEach(trajet -> {
            assertThat(trajet.getChauffeur()).isNotNull();
            assertThat(trajet.getVehicule().getAgence()).isNotNull();
        });
        reservations.findAll().forEach(reservation -> {
            assertThat(reservation.getVoyageur()).isNotNull();
            assertThat(reservation.getTrajet().getChauffeur()).isNotNull();
        });
        paiements.findAll().forEach(paiement -> assertThat(paiement.getReservation()).isNotNull());
    }
}
