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
@org.junit.jupiter.api.TestInstance(org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "POSTGRES_BASELINE_URL", matches = ".*/issue1_validation_[a-z0-9_]+")
class PostgresBaselineTest {
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
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
    @Autowired com.transport.gestiontransport.service.VoyageurService voyageurService;
    @Autowired RestrictionRepository restrictions;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired AdministrateurRepository administrateurs;

    @org.junit.jupiter.api.BeforeAll
    void appliquerLesInvariantsSurLaBasePostgresIsolee() throws java.io.IOException {
        jdbc.execute(java.nio.file.Files.readString(java.nio.file.Path.of("scripts/migrations/issue-3-invariants-transport.sql")));
    }

    @Test
    void postgresRefuseUneCinquiemePlaceEtUneSecondeDemandeActiveMemeHorsService() {
        Trajet trajet = trajetVide();
        Reservation demande = demande(trajet);
        assertThatThrownBy(() -> jdbc.update("UPDATE trajets SET nb_passagers_actuels = 5 WHERE id = ?", trajet.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO reservations(trajet_id,voyageur_id,statut) VALUES (?,?,'EN_ATTENTE')",
                trajet.getId(), demande.getVoyageur().getId())).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(trajets.findById(trajet.getId()).orElseThrow().getNbPassagersActuels()).isZero();
    }

    @Test
    void agencyTypesRemainCompatibleWithLegacySqlValues() {
        Voyageur voyageur = voyageur();
        voyageur.setTypeVoyageur(TypeVoyageur.LOCAL);
        voyageurs.saveAndFlush(voyageur);
        assertThat(jdbc.queryForObject("SELECT type_voyageur FROM voyageurs WHERE id = ?", String.class, voyageur.getId()))
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
    void restrictionsEvenementiellesIdempotentesEtHistoriqueExpireConserve() {
        Voyageur client = voyageur();
        String source = "absence:test:" + UUID.randomUUID();
        voyageurService.addRestrictionPourEvenement(client.getId(), source, "Absence déclarée et vérifiée");
        voyageurService.addRestrictionPourEvenement(client.getId(), source, "Autre texte à ignorer");
        assertThat(voyageurService.getRestrictions(client.getId())).hasSize(1);
        Restriction restriction = voyageurService.getRestrictions(client.getId()).getFirst();
        assertThat(restriction.getDateDebut()).isEqualTo(java.time.LocalDate.now());
        assertThat(restriction.getDateFin()).isEqualTo(java.time.LocalDate.now().plusDays(7));
        assertThat(voyageurService.hasActiveRestriction(client.getId())).isTrue();
        restriction.setDateFin(java.time.LocalDate.now());
        restrictions.saveAndFlush(restriction);
        assertThat(voyageurService.hasActiveRestriction(client.getId())).isFalse();
        assertThat(voyageurService.getRestrictions(client.getId())).hasSize(1);
        restriction.setDateDebut(java.time.LocalDate.now().plusDays(1));
        restriction.setDateFin(java.time.LocalDate.now().plusDays(8));
        restrictions.saveAndFlush(restriction);
        assertThat(voyageurService.hasActiveRestriction(client.getId())).isFalse();
    }

    @Test
    void restrictionsExpireesSontLeveesEtUneRestrictionActiveNestPasDupliquee() {
        Voyageur voyageur = voyageur();
        restrictions.saveAndFlush(Restriction.builder().voyageur(voyageur).dateFin(java.time.LocalDate.now().minusDays(1))
                .motif("Ancienne restriction de test").build());
        assertThat(voyageurService.hasActiveRestriction(voyageur.getId())).isFalse();
        voyageurService.addRestriction(voyageur.getId());
        voyageurService.addRestriction(voyageur.getId());
        assertThat(voyageurService.hasActiveRestriction(voyageur.getId())).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM restrictions WHERE voyageur_id = ?", Long.class, voyageur.getId())).isEqualTo(2L);
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
                .heureOuverture(LocalTime.MIN).heureFermeture(LocalTime.of(23, 59))
                .dureeTrajetMinutes(90).dureeEntreTrajetMinutes(15).anneeCivile(2030).build());
        Vehicule vehicule = vehicules.saveAndFlush(Vehicule.builder().nom("Voiture test").agence(agence).disponible(true).build());
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
    void deuxRestrictionsConcurrentesPourLeMemeFaitNeCreentQuUneLigne() throws Exception {
        Voyageur client = voyageur();
        String source = "absence:concurrence:" + UUID.randomUUID();
        CyclicBarrier barriere = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Void> action = () -> {
                barriere.await(10, TimeUnit.SECONDS);
                voyageurService.addRestrictionPourEvenement(client.getId(), source, "Même fait vérifié");
                return null;
            };
            Future<Void> a = executor.submit(action);
            Future<Void> b = executor.submit(action);
            a.get(30, TimeUnit.SECONDS);
            b.get(30, TimeUnit.SECONDS);
            assertThat(voyageurService.getRestrictions(client.getId())).hasSize(1);
        } finally { executor.shutdownNow(); }
    }

    @Test
    void apiAbsenceExigeUnAdministrateurDeLaBonneAgenceEtConserveLaDeclaration() throws Exception {
        Trajet trajet = trajetVide();
        Reservation reservation = demande(trajet);
        paiementService.pay(reservation.getId(), reservation.getVoyageur().getId());
        Administrateur admin = administrateurs.saveAndFlush(Administrateur.builder().matricule(UUID.randomUUID().toString())
                .nom("Validation").prenom("Administrateur").email(UUID.randomUUID() + "@example.test")
                .passwordHash("not-used-for-login").typeAdministrateur(TypeAdministrateur.AGENCE)
                .agence(trajet.getVehicule().getAgence()).build());
        String endpoint = "/api/reservations/" + reservation.getId() + "/absence";
        var post = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(endpoint);
        mvc.perform(post.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("client").roles("VOYAGEUR"))
                .contentType("application/json").content("{\"motif\":\"Absence vérifiée\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(endpoint)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(admin.getEmail()).roles("ADMIN"))
                .contentType("application/json").content("{\"motif\":\"Absence vérifiée\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        jdbc.update("UPDATE trajets SET date_debut = ?, date_fin = ? WHERE id = ?",
                java.sql.Timestamp.valueOf(LocalDateTime.now().minusHours(2)), java.sql.Timestamp.valueOf(LocalDateTime.now().minusMinutes(30)), trajet.getId());
        for (int i = 0; i < 2; i++) {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(endpoint)
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(admin.getEmail()).roles("ADMIN"))
                    .contentType("application/json").content("{\"motif\":\"Absence vérifiée\"}"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.motifAbsence").value("Absence vérifiée"));
        }
        assertThat(jdbc.queryForObject("SELECT absence_admin_id FROM reservations WHERE id = ?", Long.class, reservation.getId())).isEqualTo(admin.getId());
        assertThat(voyageurService.getRestrictions(reservation.getVoyageur().getId())).isEmpty();
        Administrateur autre = administrateurs.saveAndFlush(Administrateur.builder().matricule(UUID.randomUUID().toString())
                .nom("Autre").prenom("Agence").email(UUID.randomUUID() + "@example.test")
                .passwordHash("not-used-for-login").typeAdministrateur(TypeAdministrateur.AGENCE).build());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(endpoint)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(autre.getEmail()).roles("ADMIN"))
                .contentType("application/json").content("{\"motif\":\"Non autorisé\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/voyageurs/" + reservation.getVoyageur().getId() + "/restrictions")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("client").roles("VOYAGEUR")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void detteExigibleBloqueLePaiementSansConfondreLesDemandesEnAttente() {
        Trajet trajet = trajetVide();
        Reservation demande = demande(trajet);
        Long clientId = demande.getVoyageur().getId();
        assertThat(voyageurService.hasOutstandingBalance(clientId)).isFalse();
        jdbc.update("UPDATE voyageurs SET solde = 25 WHERE id = ?", clientId);
        assertThatThrownBy(() -> paiementService.pay(demande.getId(), clientId)).isInstanceOf(BusinessException.class).hasMessageContaining("impayé");
        assertThat(jdbc.queryForObject("SELECT statut FROM paiements WHERE reservation_id = ?", String.class, demande.getId())).isEqualTo("EN_ATTENTE");
        assertThat(voyageurService.getRestrictions(clientId)).isEmpty();
    }

    @Test
    void quatrePaiementsPuisAnnulationLiberentUnePlaceSansFacturerLeChauffeur() {
        Trajet trajet = trajetVide();
        List<Reservation> demandes = java.util.stream.IntStream.range(0, 5).mapToObj(i -> demande(trajet)).toList();
        assertThat(trajets.findById(trajet.getId()).orElseThrow().getNbPassagersActuels()).isZero();
        for (int i = 0; i < 4; i++) paiementService.pay(demandes.get(i).getId(), demandes.get(i).getVoyageur().getId());
        assertThat(trajets.findById(trajet.getId()).orElseThrow().getNbPassagersActuels()).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT sum(p.montant) FROM paiements p JOIN reservations r ON r.id = p.reservation_id WHERE r.trajet_id = ? AND p.statut = 'PAYE'",
                java.math.BigDecimal.class, trajet.getId())).isEqualByComparingTo("100.00");
        Reservation cinquieme = demandes.get(4);
        assertThatThrownBy(() -> paiementService.pay(cinquieme.getId(), cinquieme.getVoyageur().getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("complet");
        assertThat(reservations.findById(cinquieme.getId()).orElseThrow().getStatut()).isEqualTo(StatutReservation.EN_ATTENTE);
        assertThatThrownBy(() -> reservationService.cancelForUser(demandes.get(0).getId(), voyageur().getId()))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        reservationService.cancelForUser(demandes.get(0).getId(), trajet.getChauffeur().getId());
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

    @Test
    void apiCreeLeTrajetAvecLeChauffeurAuthentifieEtProtegeLePrive() throws Exception {
        Trajet existant = trajetVide();
        Voyageur chauffeur = voyageurs.findById(existant.getChauffeur().getId()).orElseThrow();
        var request = new com.transport.gestiontransport.dto.request.CreateTrajetRequest(existant.getVehicule().getId(),
                LocalDateTime.now().plusDays(4).withHour(10).withMinute(0), "PRIVE", VilleBelge.MONS, VilleBelge.BRUXELLES, false, null);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/trajets")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(chauffeur.getMatricule()).roles("VOYAGEUR"))
                .contentType("application/json").content(json.writeValueAsString(request)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.chauffeurId").value(chauffeur.getId()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.placesRestantes").value(4))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.villeDepart").value("MONS"));
        existant.setTypeTrajet(TypeTrajet.PRIVE);
        trajets.saveAndFlush(existant);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/trajets/" + existant.getId())
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(voyageur().getMatricule()).roles("VOYAGEUR")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/trajets")
                .contentType("application/json").content(json.writeValueAsString(request)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void uneRessourceHistoriqueNestPasMiseEnServiceAutomatiquement() {
        Trajet trajet = trajetVide();
        Long id = jdbc.queryForObject("INSERT INTO vehicules(nom, agence_id) VALUES ('Ressource historique à qualifier', ?) RETURNING id",
                Long.class, trajet.getVehicule().getAgence().getId());
        assertThat(jdbc.queryForObject("SELECT disponible FROM vehicules WHERE id = ?", Boolean.class, id)).isFalse();
    }

    @Test
    void apiReserveSansIdentifiantDemandeurFourniParLeNavigateur() throws Exception {
        Trajet trajet = trajetVide();
        Voyageur client = voyageur();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/reservations")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(client.getMatricule()).roles("VOYAGEUR"))
                .contentType("application/json").content(json.writeValueAsString(java.util.Map.of("trajetId", trajet.getId(), "voyageurId", client.getId()))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.paiement.montant").value(25));
    }

    @Test
    void apiRechercheMonsBruxellesEtExposeLaListeDesVilles() throws Exception {
        trajetVide();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/trajets/public")
                .param("depart", "MONS").param("arrivee", "BRUXELLES"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].villeDepart").value("MONS"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/trajets/villes"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].code").value("BRUXELLES"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/trajets/public").param("depart", "INCONNU"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
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
