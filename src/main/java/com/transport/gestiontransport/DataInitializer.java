package com.transport.gestiontransport;

import com.transport.gestiontransport.entity.*;
import com.transport.gestiontransport.entity.enums.*;
import com.transport.gestiontransport.repository.*;
import com.transport.gestiontransport.service.TrajetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

/** Démonstration explicite, exclusivement sur une base de développement vide. */
@Slf4j
@Component
@Profile("dev")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final AgenceRepository agenceRepository;
    private final VehiculeRepository vehiculeRepository;
    private final VoyageurRepository voyageurRepository;
    private final AdministrateurRepository administrateurRepository;
    private final IndisponibiliteServiceRepository indisponibiliteServiceRepository;
    private final TrajetRepository trajetRepository;
    private final ReservationRepository reservationRepository;
    private final PaiementRepository paiementRepository;
    private final RestrictionRepository restrictionRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.datasource.url}") private String databaseUrl;
    @Value("${DEMO_PASSWORD:}") private String demoPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (agenceRepository.count() + vehiculeRepository.count() + voyageurRepository.count()
                + administrateurRepository.count() + trajetRepository.count() + reservationRepository.count()
                + paiementRepository.count() + restrictionRepository.count() + indisponibiliteServiceRepository.count() > 0) {
            log.info("Démonstration ignorée : la base contient déjà des données. Aucun compte ni paiement modifié.");
            return;
        }
        if (databaseUrl == null || !databaseUrl.matches("jdbc:postgresql://[^/]+/(issue1_validation_[a-z0-9_]+|transport_demo_[a-z0-9_]+)(\\?.*)?")) {
            throw new IllegalStateException("La démonstration exige une base isolée explicitement nommée.");
        }
        if (demoPassword == null || demoPassword.length() < 12 || demoPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("DEMO_PASSWORD doit contenir au moins 12 caractères et au maximum 72 octets UTF-8 pour BCrypt.");
        }
        Agence agence = agenceRepository.save(Agence.builder().nom("Agence Bruxelles — DÉMONSTRATION")
                .adresse("Bruxelles, Belgique").heureOuverture(LocalTime.of(6, 0)).heureFermeture(LocalTime.of(23, 0))
                .dureeTrajetMinutes(90).dureeEntreTrajetMinutes(15).anneeCivile(LocalDate.now().getYear()).build());
        Vehicule voiture = vehiculeRepository.save(Vehicule.builder().nom("Voiture DÉMONSTRATION").agence(agence).disponible(true).build());
        String hash = passwordEncoder.encode(demoPassword);
        Voyageur chauffeur = voyageurRepository.save(Voyageur.builder().matricule("G1001").nom("Démo").prenom("Alice")
                .email("chauffeur@example.test").passwordHash(hash).typeVoyageur(TypeVoyageur.VIP)
                .solde(0.0).peutProposerTrajet(true).agence(agence).build());
        voyageurRepository.save(Voyageur.builder().matricule("G1002").nom("Démo").prenom("Client VIP")
                .email("vip@example.test").passwordHash(hash).typeVoyageur(TypeVoyageur.VIP)
                .solde(0.0).agence(agence).build());
        voyageurRepository.save(Voyageur.builder().matricule("S10001").nom("Démo").prenom("Client local")
                .email("local@example.test").passwordHash(hash).typeVoyageur(TypeVoyageur.LOCAL)
                .solde(0.0).agence(agence).build());
        voyageurRepository.save(Voyageur.builder().matricule("L10001").nom("Démo").prenom("Client libre")
                .email("libre@example.test").passwordHash(hash).typeVoyageur(TypeVoyageur.LIBRE)
                .solde(0.0).agence(agence).build());
        administrateurRepository.save(Administrateur.builder().matricule("AD1001").nom("Démo").prenom("Admin global")
                .email("admin@example.test").passwordHash(hash).typeAdministrateur(TypeAdministrateur.GLOBAL).build());
        administrateurRepository.save(Administrateur.builder().matricule("AD1002").nom("Démo").prenom("Admin local")
                .email("agence@example.test").passwordHash(hash).typeAdministrateur(TypeAdministrateur.AGENCE).agence(agence).build());
        LocalDate jour = LocalDate.now().plusDays(2);
        VilleBelge[][] parcours = {
                {VilleBelge.MONS, VilleBelge.BRUXELLES}, {VilleBelge.BRUXELLES, VilleBelge.MONS},
                {VilleBelge.CHARLEROI, VilleBelge.BRUXELLES}, {VilleBelge.NAMUR, VilleBelge.BRUXELLES},
                {VilleBelge.LIEGE, VilleBelge.BRUXELLES}, {VilleBelge.BRUXELLES, VilleBelge.ANVERS},
                {VilleBelge.GAND, VilleBelge.BRUXELLES}, {VilleBelge.LOUVAIN, VilleBelge.BRUXELLES}};
        for (int i = 0; i < parcours.length; i++) {
            java.time.LocalDateTime depart = jour.atTime(7 + 2 * i, 0);
            trajetRepository.save(Trajet.builder().chauffeur(chauffeur).vehicule(voiture)
                .villeDepart(parcours[i][0]).villeArrivee(parcours[i][1])
                .dateDebut(depart).dateFin(depart.plusMinutes(90)).typeTrajet(i == 1 || i == 7 ? TypeTrajet.PRIVE : TypeTrajet.PUBLIC)
                .statut(StatutTrajet.PLANIFIE).nbPassagersActuels(0)
                .prixTotal(TrajetService.PRIX_TRAJET_EUROS.doubleValue())
                .prixParPassager(TrajetService.PRIX_CLIENT_EUROS.doubleValue()).build());
        }
        log.info("Démonstration créée sur base isolée vide : aucune réservation ni aucun paiement de chauffeur.");
    }
}
