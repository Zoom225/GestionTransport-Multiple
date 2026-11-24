package com.transport.gestiontransport;

import com.transport.gestiontransport.config.JwtConfig;
import com.transport.gestiontransport.entity.*;
import com.transport.gestiontransport.entity.enums.*;
import com.transport.gestiontransport.repository.*;
import com.transport.gestiontransport.service.*;
import com.transport.gestiontransport.service.impl.VoyageurEligibilityService;
import com.transport.gestiontransport.dto.request.CreateTrajetRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SuspensionParticipationTest.HeureTest.class)
class SuspensionParticipationTest {
    static class Horloge extends Clock {
        Instant instant = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return Clock.fixed(instant, zone); }
        public Instant instant() { return instant; }
    }
    @TestConfiguration static class HeureTest {
        @Bean @Primary Horloge horlogeTest() { return new Horloge(); }
    }
    @Autowired Horloge clock;
    @Autowired VoyageurRepository voyageurs;
    @Autowired AgenceRepository agences;
    @Autowired VehiculeRepository vehicules;
    @Autowired TrajetRepository trajets;
    @Autowired RestrictionRepository restrictions;
    @Autowired ReservationRepository reservations;
    @Autowired PaiementRepository paiements;
    @Autowired TrajetService trajetService;
    @Autowired ReservationService reservationService;
    @Autowired PaiementService paiementService;
    @Autowired VoyageurEligibilityService eligibility;
    @Autowired MockMvc mvc;
    @Autowired JwtConfig jwtConfig;
    @Autowired ObjectMapper json;
    Voyageur client, chauffeur;
    Vehicule voiture;
    Trajet prive, publicTrajet;
    Instant debut;

    @BeforeEach void preparer() {
        clock.instant = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        debut = clock.instant;
        client = voyageur(); chauffeur = voyageur();
        Agence agence = agences.saveAndFlush(Agence.builder().nom("Suspension test").adresse("Test")
                .heureOuverture(LocalTime.MIN).heureFermeture(LocalTime.of(23,59))
                .dureeTrajetMinutes(90).dureeEntreTrajetMinutes(15).anneeCivile(2026).build());
        voiture = vehicules.saveAndFlush(Vehicule.builder().nom("Voiture suspension").agence(agence).disponible(true).build());
        prive = trajet(client, TypeTrajet.PRIVE, LocalDateTime.now().plusHours(12));
        publicTrajet = trajet(chauffeur, TypeTrajet.PUBLIC, LocalDateTime.now().plusDays(10));
    }
    Voyageur voyageur() {
        return voyageurs.saveAndFlush(Voyageur.builder().matricule(UUID.randomUUID().toString())
                .nom("Test").prenom("Participation").typeVoyageur(TypeVoyageur.VIP).solde(0.0).peutProposerTrajet(true).build());
    }
    Trajet trajet(Voyageur organisateur, TypeTrajet type, LocalDateTime depart) {
        return trajets.saveAndFlush(Trajet.builder().organisateur(organisateur).chauffeur(chauffeur).vehicule(voiture)
                .typeTrajet(type).statut(StatutTrajet.PLANIFIE).villeDepart(VilleBelge.MONS).villeArrivee(VilleBelge.BRUXELLES)
                .dateDebut(depart).dateFin(depart.plusMinutes(90)).nbPassagersActuels(0).prixParPassager(25.0).prixTotal(100.0).build());
    }
    void sanctionner() { trajetService.imputerPenaliteTrajetPriveIncomplet(prive.getId()); }
    void regler() { trajetService.payerPenalite(prive.getId(), client.getId()); }
    Restriction sanction() { return restrictions.findByVoyageurIdOrderByDateFinDescIdDesc(client.getId()).getFirst(); }
    CreateTrajetRequest demandeCreation(String type) {
        return new CreateTrajetRequest(voiture.getId(), LocalDateTime.now().plusDays(10).withHour(10), type,
                VilleBelge.MONS, VilleBelge.BRUXELLES, false, null);
    }

    @Test
    void jwtAuthentifieLeVoyageurEtDistingueLesRefus() throws Exception {
        String tokenVoyageur = jwtConfig.generateToken(client.getMatricule(), "VIP");
        mvc.perform(get("/api/voyageurs/me").header("Authorization", "Bearer " + tokenVoyageur))
                .andExpect(status().isOk());
        mvc.perform(get("/api/trajets").header("Authorization", "Bearer " + tokenVoyageur))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/voyageurs/me").header("Authorization", "Bearer format-invalide"))
                .andExpect(status().isUnauthorized());
        String tokenRoleIncoherent = jwtConfig.generateToken(client.getMatricule(), "LOCAL");
        mvc.perform(get("/api/voyageurs/me").header("Authorization", "Bearer " + tokenRoleIncoherent))
                .andExpect(status().isUnauthorized());
    }

    @Test void T01_detteCreee() { sanctionner(); assertThat(voyageurs.findById(client.getId()).orElseThrow().getSolde()).isEqualTo(100.0); }
    @Test void T02_suspensionExactement168HeuresEtOrigineConservee() {
        sanctionner(); Restriction r = sanction();
        assertThat(r.getDebutSuspension()).isEqualTo(debut);
        assertThat(r.getFinSuspension()).isEqualTo(debut.plus(Duration.ofHours(168)));
        assertThat(r.getTrajetOrigine().getId()).isEqualTo(prive.getId());
        assertThat(r.getMontantPenalite()).isEqualByComparingTo("100.00");
    }
    @Test void T03_creationPriveeRefusee() {
        sanctionner(); assertThatThrownBy(() -> trajetService.createTrajet(demandeCreation("PRIVE"), client.getMatricule())).hasMessageContaining("suspendu");
    }
    @Test void T04_creationPubliqueRefusee() {
        sanctionner(); assertThatThrownBy(() -> trajetService.createTrajet(demandeCreation("PUBLIC"), client.getMatricule())).hasMessageContaining("suspendu");
    }
    @Test void T05_inscriptionRefusee() {
        sanctionner(); assertThatThrownBy(() -> reservationService.create(publicTrajet.getId(), client.getId(), client.getId())).hasMessageContaining("suspendu");
    }
    @Test void T06_invitationParAutreOrganisateurRefuseeSansDetailsFinanciers() {
        sanctionner(); Voyageur autre = voyageur();
        Trajet invite = trajet(autre, TypeTrajet.PRIVE, LocalDateTime.now().plusDays(10));
        assertThatThrownBy(() -> reservationService.createForInvites(invite.getId(), autre.getId(), List.of(client.getMatricule())))
                .hasMessageContaining("Ce voyageur").hasMessageNotContaining("100").hasMessageNotContaining("solde");
    }
    @Test void T19_chauffeurAffectePeutInviterViaEndpointPrive() throws Exception {
        Voyageur invite = voyageur();
        mvc.perform(post("/api/reservations/private-invites")
                .with(user(chauffeur.getMatricule()).roles("VOYAGEUR"))
                .contentType("application/json").content(json.writeValueAsString(Map.of(
                        "trajetId", prive.getId(), "inviteeMatricules", List.of(invite.getMatricule())))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$[0].voyageurId").value(invite.getId()));
    }
    @Test void T07_paiementEtConfirmationRefuses() {
        Reservation r = reservationService.create(publicTrajet.getId(), client.getId(), client.getId());
        sanctionner();
        assertThatThrownBy(() -> paiementService.pay(r.getId(), client.getId())).hasMessageContaining("suspendu");
        Paiement p = paiements.findByReservationId(r.getId()).orElseThrow(); p.setStatut(StatutPaiement.PAYE); paiements.saveAndFlush(p);
        assertThatThrownBy(() -> reservationService.confirm(r.getId())).hasMessageContaining("suspendu");
        assertThat(reservations.findById(r.getId()).orElseThrow().getStatut()).isEqualTo(StatutReservation.EN_ATTENTE);
    }
    @Test void T08_paiementAnticipeNeChangePasFin() {
        sanctionner(); Instant fin = sanction().getFinSuspension(); regler();
        assertThat(eligibility.statut(client.getId()).peutParticiper()).isFalse();
        assertThat(sanction().getFinSuspension()).isEqualTo(fin);
    }
    @Test void T09_expirationAvecDetteBloque() {
        sanctionner(); clock.instant = debut.plus(Duration.ofHours(168));
        assertThat(eligibility.statut(client.getId()).code()).isEqualTo("PENALITE_IMPAYEE");
    }
    @Test void T10_expirationAvecDetteRegleeAutoriseSansRedemarrage() {
        sanctionner(); regler(); clock.instant = debut.plus(Duration.ofHours(168));
        assertThat(eligibility.statut(client.getId()).peutParticiper()).isTrue();
        assertThat(reservationService.create(publicTrajet.getId(), client.getId(), client.getId()).getStatut()).isEqualTo(StatutReservation.EN_ATTENTE);
    }
    @Test void T11_borneExclusiveExacte() {
        sanctionner(); regler(); clock.instant = debut.plus(Duration.ofHours(168)).minusNanos(1000);
        assertThat(eligibility.statut(client.getId()).suspensionActive()).isTrue();
        clock.instant = debut.plus(Duration.ofHours(168));
        assertThat(eligibility.statut(client.getId()).suspensionActive()).isFalse();
    }
    @Test void T12_reexecutionIdempotente() {
        sanctionner(); sanctionner();
        assertThat(restrictions.findByVoyageurIdOrderByDateFinDescIdDesc(client.getId())).hasSize(1);
        assertThat(voyageurs.findById(client.getId()).orElseThrow().getSolde()).isEqualTo(100.0);
    }
    @Test void T13_deuxSanctionsConserventHistoriqueEtFinLaPlusTardive() {
        sanctionner(); clock.instant = debut.plusSeconds(3600);
        Trajet second = trajet(client, TypeTrajet.PRIVE, LocalDateTime.now().plusHours(13));
        trajetService.imputerPenaliteTrajetPriveIncomplet(second.getId());
        assertThat(restrictions.findByVoyageurIdOrderByDateFinDescIdDesc(client.getId())).hasSize(2);
        assertThat(eligibility.statut(client.getId()).finSuspension()).isEqualTo(clock.instant.plus(Duration.ofHours(168)));
        assertThat(voyageurs.findById(client.getId()).orElseThrow().getSolde()).isEqualTo(200.0);
    }
    @Test void T14_messageFrancaisEtCodeStables() {
        sanctionner(); regler();
        assertThat(eligibility.statut(client.getId()).code()).isEqualTo("VOYAGEUR_SUSPENDU");
        assertThat(eligibility.statut(client.getId()).message()).contains("pénalité de 100 € est réglée", "suspension reste toutefois active", "jusqu'au");
    }
    @Test void T15_consultationEtReglementAccessibles() throws Exception {
        sanctionner();
        mvc.perform(get("/api/voyageurs/me").with(user(client.getMatricule()).roles("VOYAGEUR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participation.suspensionActive").value(true))
                .andExpect(jsonPath("$.participation.debutSuspension").value(debut.toString()))
                .andExpect(jsonPath("$.participation.finSuspension").value(debut.plus(Duration.ofHours(168)).toString()))
                .andExpect(jsonPath("$.participation.soldeDu").value(100))
                .andExpect(jsonPath("$.participation.detteBloquante").value(true))
                .andExpect(jsonPath("$.participation.peutCreerMatch").value(false))
                .andExpect(jsonPath("$.participation.peutRejoindreMatch").value(false))
                .andExpect(jsonPath("$.participation.motifRestriction").isNotEmpty());
        mvc.perform(get("/api/voyageurs/me/sanctions").with(user(client.getMatricule()).roles("VOYAGEUR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trajetId").value(prive.getId()))
                .andExpect(jsonPath("$[0].motif").value("Suspension automatique de 168 heures : trajet privé incomplet"));
        mvc.perform(post("/api/trajets/" + prive.getId() + "/penalite/pay").with(user(client.getMatricule()).roles("VOYAGEUR")))
                .andExpect(status().isNoContent());
        assertThat(voyageurs.findById(client.getId()).orElseThrow().getSolde()).isZero();
        assertThat(eligibility.statut(client.getId()).suspensionActive()).isTrue();
        mvc.perform(post("/api/trajets/" + prive.getId() + "/penalite/pay").with(user(client.getMatricule()).roles("VOYAGEUR")))
                .andExpect(status().isNoContent());
        clock.instant = debut.plus(Duration.ofHours(168));
        mvc.perform(get("/api/voyageurs/me").with(user(client.getMatricule()).roles("VOYAGEUR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participation.suspensionActive").value(false))
                .andExpect(jsonPath("$.participation.peutRejoindreMatch").value(true));
    }
    @Test void T18_paiementHTTPReserveAuProprietaireEtHistoriquePrive() throws Exception {
        sanctionner(); Voyageur autre = voyageur();
        String url = "/api/trajets/" + prive.getId() + "/penalite/pay";
        mvc.perform(post(url).with(user(autre.getMatricule()).roles("VOYAGEUR"))).andExpect(status().isForbidden());
        mvc.perform(post(url).with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(post(url)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/voyageurs/me/sanctions").with(user(autre.getMatricule()).roles("VOYAGEUR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }
    @Test void T16_endpointsCreationEtInscriptionNonContournables() throws Exception {
        sanctionner();
        for (String type : List.of("PRIVE", "PUBLIC")) {
            mvc.perform(post("/api/trajets").with(user(client.getMatricule()).roles("VOYAGEUR"))
                    .contentType("application/json").content(json.writeValueAsString(demandeCreation(type))))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VOYAGEUR_SUSPENDU"));
        }
        mvc.perform(post("/api/reservations").with(user(client.getMatricule()).roles("VOYAGEUR"))
                .contentType("application/json").content(json.writeValueAsString(Map.of("trajetId",publicTrajet.getId(),"voyageurId",client.getId()))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VOYAGEUR_SUSPENDU"));
    }

    @Test void parcoursDemonstrationDeLEcheanceALaReactivation() throws Exception {
        Voyageur organisateurScenario = voyageur();
        var demande = new CreateTrajetRequest(voiture.getId(), LocalDate.now().plusDays(2).atTime(10, 0),
                "PRIVE", VilleBelge.MONS, VilleBelge.BRUXELLES, false, null);
        var trajetCree = trajetService.createTrajet(demande, organisateurScenario.getMatricule());
        clock.instant = demande.trajetDate().minusHours(24).plusSeconds(1)
                .atZone(ZoneId.systemDefault()).toInstant();
        Instant imputation = clock.instant;
        trajetService.checkAndConvertExpiredPrivateTrajets();
        trajetService.checkAndConvertExpiredPrivateTrajets();
        assertThat(eligibility.statut(organisateurScenario.getId()).soldeDu()).isEqualByComparingTo("100");
        assertThat(eligibility.statut(organisateurScenario.getId()).finSuspension()).isEqualTo(imputation.plus(Duration.ofHours(168)));
        assertThat(restrictions.findByVoyageurIdOrderByDateFinDescIdDesc(organisateurScenario.getId())).hasSize(1);

        var principal = user(organisateurScenario.getMatricule()).roles("VOYAGEUR");
        for (String type : List.of("PRIVE", "PUBLIC")) {
            mvc.perform(post("/api/trajets").with(principal).contentType("application/json")
                    .content(json.writeValueAsString(demandeCreation(type))))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VOYAGEUR_SUSPENDU"));
        }
        var organisateur = voyageur();
        Trajet autreMatch = trajet(organisateur, TypeTrajet.PRIVE, LocalDateTime.now().plusDays(12));
        String invitation = json.writeValueAsString(Map.of("trajetId", autreMatch.getId(),
                "inviteeMatricules", List.of(organisateurScenario.getMatricule())));
        for (Voyageur auteur : List.of(organisateur, chauffeur)) {
            mvc.perform(post("/api/reservations/private-invites").with(user(auteur.getMatricule()).roles("VOYAGEUR"))
                    .contentType("application/json").content(invitation))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VOYAGEUR_NON_ELIGIBLE"));
        }
        mvc.perform(post("/api/reservations/private-invites").with(user(voyageur().getMatricule()).roles("VOYAGEUR"))
                .contentType("application/json").content(invitation)).andExpect(status().isForbidden());

        mvc.perform(post("/api/trajets/" + trajetCree.id() + "/penalite/pay").with(principal))
                .andExpect(status().isNoContent());
        String inscription = json.writeValueAsString(Map.of("trajetId", publicTrajet.getId(), "voyageurId", organisateurScenario.getId()));
        mvc.perform(post("/api/reservations").with(principal).contentType("application/json").content(inscription))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VOYAGEUR_SUSPENDU"));
        clock.instant = imputation.plus(Duration.ofHours(168));
        mvc.perform(get("/api/voyageurs/me").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participation.peutCreerMatch").value(true));
        mvc.perform(post("/api/reservations").with(principal).contentType("application/json").content(inscription))
                .andExpect(status().isCreated());
    }
}
