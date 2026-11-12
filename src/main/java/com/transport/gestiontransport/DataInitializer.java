package com.transport.gestiontransport;

import com.transport.gestiontransport.entity.Administrateur;
import com.transport.gestiontransport.entity.IndisponibiliteService;
import com.transport.gestiontransport.entity.Trajet;
import com.transport.gestiontransport.entity.Voyageur;
import com.transport.gestiontransport.entity.Paiement;
import com.transport.gestiontransport.entity.Restriction;
import com.transport.gestiontransport.entity.Reservation;
import com.transport.gestiontransport.entity.Agence;
import com.transport.gestiontransport.entity.Vehicule;
import com.transport.gestiontransport.entity.enums.StatutTrajet;
import com.transport.gestiontransport.entity.enums.StatutPaiement;
import com.transport.gestiontransport.entity.enums.StatutReservation;
import com.transport.gestiontransport.entity.enums.TypeAdministrateur;
import com.transport.gestiontransport.entity.enums.TypeTrajet;
import com.transport.gestiontransport.entity.enums.TypeVoyageur;
import com.transport.gestiontransport.repository.AdministrateurRepository;
import com.transport.gestiontransport.repository.IndisponibiliteServiceRepository;
import com.transport.gestiontransport.repository.TrajetRepository;
import com.transport.gestiontransport.repository.VoyageurRepository;
import com.transport.gestiontransport.repository.PaiementRepository;
import com.transport.gestiontransport.repository.RestrictionRepository;
import com.transport.gestiontransport.repository.ReservationRepository;
import com.transport.gestiontransport.repository.AgenceRepository;
import com.transport.gestiontransport.repository.VehiculeRepository;
import com.transport.gestiontransport.service.TrajetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String DEMO_VOYAGEUR_PASSWORD = "Voyageur1234!";
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

    @Override
    public void run(String... args) {
        migrateLegacyVoyageurCredentials();

        if (agenceRepository.count() > 0) {
            if (trajetRepository.count() == 0) {
                log.info("DataInitializer : base de donnees deja initialisee, ajout des trajets d'exemple...");
                seedTrajetsFromExistingData();
            }
            ensureRestrictedDemoVoyageur();
            ensureDemoVoyageursCredentials();
            log.info("DataInitializer : base de donnees deja initialisee, initialisation ignoree...");
            return;
        }

        log.info("DataInitializer : initialisation de la base de donnees...");

        Agence agenceLyon = agenceRepository.save(Agence.builder()
                .nom("Agence Lyon")
                .adresse("12 rue de la Republique, Lyon")
                .heureOuverture(LocalTime.of(8, 0))
                .heureFermeture(LocalTime.of(22, 0))
                .dureeTrajetMinutes(90)
                .dureeEntreTrajetMinutes(15)
                .anneeCivile(2025)
                .build());

        Agence agenceParis = agenceRepository.save(Agence.builder()
                .nom("Agence Paris")
                .adresse("45 avenue des Champs, Paris")
                .heureOuverture(LocalTime.of(7, 0))
                .heureFermeture(LocalTime.of(23, 0))
                .dureeTrajetMinutes(90)
                .dureeEntreTrajetMinutes(15)
                .anneeCivile(2025)
                .build());

        log.info("DataInitializer : {} agences crees", agenceRepository.count());
////-------------------Vehicules--------------------//
        Vehicule courtA = vehiculeRepository.save(Vehicule.builder().nom("Court A").agence(agenceLyon).prix(60.0).build());
        Vehicule courtB = vehiculeRepository.save(Vehicule.builder().nom("Court B").agence(agenceLyon).prix(60.0).build());
        Vehicule courtC = vehiculeRepository.save(Vehicule.builder().nom("Court C").agence(agenceLyon).prix(60.0).build());
        Vehicule court1 = vehiculeRepository.save(Vehicule.builder().nom("Court 1").agence(agenceParis).prix(60.0).build());
        vehiculeRepository.save(Vehicule.builder().nom("Court 2").agence(agenceParis).prix(60.0).build());

        log.info("DataInitializer : {} vehicules crees", vehiculeRepository.count());
//-----------------------Jours de fermeture--------------------//
        indisponibiliteServiceRepository.save(IndisponibiliteService.builder()
                .date(LocalDate.of(2025, 12, 25))
                .raison("Jour de Noel")
                .global(true)
                .agence(null)
                .build());

        indisponibiliteServiceRepository.save(IndisponibiliteService.builder()
                .date(LocalDate.of(2025, 1, 1))
                .raison("Jour de l'An")
                .global(true)
                .agence(null)
                .build());

        indisponibiliteServiceRepository.save(IndisponibiliteService.builder()
                .date(LocalDate.of(2025, 7, 14))
                .raison("Fete nationale - maintenance de cette agence de Lyon")
                .global(false)
                .agence(agenceLyon)
                .build());

        log.info("DataInitializer : jours de fermeture crees");
//-----------------------Voyageurs--------------------//
        String defaultVoyageurPasswordHash = passwordEncoder.encode(DEMO_VOYAGEUR_PASSWORD);

        Voyageur lucas = voyageurRepository.save(Voyageur.builder()
                .matricule("G1001")
                .nom("Martin")
                .prenom("Lucas")
                .email("lucas.martin@gmail.com")
                .typeVoyageur(TypeVoyageur.VIP)
                .solde(0.0)
                .agence(null)
                .passwordHash(defaultVoyageurPasswordHash)
                .build());

        Voyageur emma = voyageurRepository.save(Voyageur.builder()
                .matricule("G1002")
                .nom("Dubois")
                .prenom("Emma")
                .email("emma.dubois@gmail.com")
                .typeVoyageur(TypeVoyageur.VIP)
                .solde(0.0)
                .agence(null)
                .passwordHash(defaultVoyageurPasswordHash)
                .build());

        Voyageur tom = voyageurRepository.save(Voyageur.builder()
                .matricule("S10001")
                .nom("Bernard")
                .prenom("Tom")
                .email("tom.bernard@gmail.com")
                .typeVoyageur(TypeVoyageur.LOCAL)
                .solde(0.0)
                .agence(agenceLyon)
                .passwordHash(defaultVoyageurPasswordHash)
                .build());

        Voyageur sarah = voyageurRepository.save(Voyageur.builder()
                .matricule("S10002")
                .nom("Leroy")
                .prenom("Sarah")
                .email("sarah.leroy@gmail.com")
                .typeVoyageur(TypeVoyageur.LOCAL)
                .solde(0.0)
                .agence(agenceParis)
                .passwordHash(defaultVoyageurPasswordHash)
                .build());

        Voyageur alex = voyageurRepository.save(Voyageur.builder()
                .matricule("L10001")
                .nom("Petit")
                .prenom("Alex")
                .email("alex.petit@gmail.com")
                .typeVoyageur(TypeVoyageur.LIBRE)
                .solde(0.0)
                .agence(null)
                .passwordHash(defaultVoyageurPasswordHash)
                .build());

        Voyageur restreint = voyageurRepository.save(Voyageur.builder()
                .matricule("L10002")
                .nom("Penalise")
                .prenom("Demo")
                .email("demo.restreint@gmail.com")
                .typeVoyageur(TypeVoyageur.LIBRE)
                .solde(0.0)
                .agence(null)
                .passwordHash(defaultVoyageurPasswordHash)
                .build());

        log.info("DataInitializer : {} voyageurs crees", voyageurRepository.count());

        seedActiveRestriction(restreint);
        seedTrajets(courtA, courtB, courtC, court1, lucas, emma, tom, sarah, alex);
//-----------------------Administrateurs--------------------//
        administrateurRepository.save(Administrateur.builder()
                .matricule("ADMIN001")
                .nom("Admin")
                .prenom("Global")
                .email("admin@transport.com")
                .passwordHash(passwordEncoder.encode("Admin1234!"))
                .typeAdministrateur(TypeAdministrateur.GLOBAL)
                .agence(null)
                .build());

        administrateurRepository.save(Administrateur.builder()
                .matricule("ADMIN002")
                .nom("Admin")
                .prenom("Lyon")
                .email("admin.lyon@transport.com")
                .passwordHash(passwordEncoder.encode("Admin1234!"))
                .typeAdministrateur(TypeAdministrateur.AGENCE)
                .agence(agenceLyon)
                .build());

        administrateurRepository.save(Administrateur.builder()
                .matricule("ADMIN003")
                .nom("Admin")
                .prenom("Paris")
                .email("admin.paris@transport.com")
                .passwordHash(passwordEncoder.encode("Admin1234!"))
                .typeAdministrateur(TypeAdministrateur.AGENCE)
                .agence(agenceParis)
                .build());

        log.info("DataInitializer : {} administrateurs crees", administrateurRepository.count());
        log.info("DataInitializer : initialisation terminee avec succes !");
    }

    private void migrateLegacyVoyageurCredentials() {
        List<Voyageur> legacyVoyageurs = voyageurRepository.findAll().stream()
                .filter(voyageur -> !StringUtils.hasText(voyageur.getPasswordHash()))
                .toList();

        if (legacyVoyageurs.isEmpty()) {
            return;
        }

        legacyVoyageurs.forEach(voyageur -> voyageur.setPasswordHash(passwordEncoder.encode(DEMO_VOYAGEUR_PASSWORD)));
        voyageurRepository.saveAll(legacyVoyageurs);

        log.warn("DataInitializer : {} compte(s) voyageur legacy ont recu le mot de passe par defaut", legacyVoyageurs.size());
    }

    private void seedTrajetsFromExistingData() {
        Vehicule courtA = requireSeedVehicule("Court A");
        Vehicule courtB = requireSeedVehicule("Court B");
        Vehicule courtC = requireSeedVehicule("Court C");
        Vehicule court1 = requireSeedVehicule("Court 1");

        Voyageur lucas = requireSeedVoyageur("G1001");
        Voyageur emma = requireSeedVoyageur("G1002");
        Voyageur tom = requireSeedVoyageur("S10001");
        Voyageur sarah = requireSeedVoyageur("S10002");
        Voyageur alex = requireSeedVoyageur("L10001");

        seedTrajets(courtA, courtB, courtC, court1, lucas, emma, tom, sarah, alex);
    }

    private Vehicule requireSeedVehicule(String nom) {
        return vehiculeRepository.findAll().stream()
                .filter(vehicule -> vehicule.getNom().equals(nom))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Vehicule seed introuvable : " + nom));
    }

    private Voyageur requireSeedVoyageur(String matricule) {
        return voyageurRepository.findByMatriculeIgnoreCase(matricule)
                .orElseThrow(() -> new IllegalStateException("Voyageur seed introuvable : " + matricule));
    }

    private void ensureRestrictedDemoVoyageur() {
        Voyageur voyageur = voyageurRepository.findByMatriculeIgnoreCase("L10002")
                .orElseGet(() -> voyageurRepository.save(Voyageur.builder()
                        .matricule("L10002")
                        .nom("Penalise")
                        .prenom("Demo")
                        .email("demo.restreint@gmail.com")
                        .typeVoyageur(TypeVoyageur.LIBRE)
                        .solde(0.0)
                        .agence(null)
                        .passwordHash(passwordEncoder.encode(DEMO_VOYAGEUR_PASSWORD))
                        .build()));

        seedActiveRestriction(voyageur);
    }

    private void ensureDemoVoyageursCredentials() {
        updateDemoVoyageurCredentials("G1001", "lucas.martin@gmail.com");
        updateDemoVoyageurCredentials("G1002", "emma.dubois@gmail.com");
        updateDemoVoyageurCredentials("S10001", "tom.bernard@gmail.com");
        updateDemoVoyageurCredentials("S10002", "sarah.leroy@gmail.com");
        updateDemoVoyageurCredentials("L10001", "alex.petit@gmail.com");
        updateDemoVoyageurCredentials("L10002", "demo.restreint@gmail.com");
    }

    private void updateDemoVoyageurCredentials(String matricule, String email) {
        voyageurRepository.findByMatriculeIgnoreCase(matricule).ifPresent(voyageur -> {
            voyageur.setEmail(email);
            voyageur.setPasswordHash(passwordEncoder.encode(DEMO_VOYAGEUR_PASSWORD));
            voyageurRepository.save(voyageur);
        });
    }

    private void seedActiveRestriction(Voyageur voyageur) {
        if (restrictionRepository.existsByVoyageurIdAndDateFinAfter(voyageur.getId(), LocalDate.now())) {
            return;
        }

        restrictionRepository.save(Restriction.builder()
                .voyageur(voyageur)
                .dateFin(LocalDate.now().plusWeeks(1))
                .motif("Voyageur de demonstration restreint")
                .build());
    }

    private void seedTrajets(
            Vehicule courtA,
            Vehicule courtB,
            Vehicule courtC,
            Vehicule court1,
            Voyageur lucas,
            Voyageur emma,
            Voyageur tom,
            Voyageur sarah,
            Voyageur alex
    ) {
        Trajet publicLyon = createSeedTrajet(courtA, lucas, LocalDate.now().plusDays(2).atTime(10, 0),
                TypeTrajet.PUBLIC, StatutTrajet.PLANIFIE, 0);
        createSeedReservation(publicLyon, lucas, StatutReservation.EN_ATTENTE, StatutPaiement.EN_ATTENTE);

        Trajet privateLyon = createSeedTrajet(courtB, tom, LocalDate.now().plusDays(3).atTime(14, 0),
                TypeTrajet.PRIVE, StatutTrajet.PLANIFIE, 1);
        createSeedReservation(privateLyon, tom, StatutReservation.CONFIRMEE, StatutPaiement.PAYE);

        Trajet publicParis = createSeedTrajet(court1, sarah, LocalDate.now().plusDays(4).atTime(18, 0),
                TypeTrajet.PUBLIC, StatutTrajet.PLANIFIE, 2);
        createSeedReservation(publicParis, sarah, StatutReservation.CONFIRMEE, StatutPaiement.PAYE);
        createSeedReservation(publicParis, emma, StatutReservation.CONFIRMEE, StatutPaiement.PAYE);

        Trajet completeLyon = createSeedTrajet(courtC, lucas, LocalDate.now().plusDays(5).atTime(16, 0),
                TypeTrajet.PUBLIC, StatutTrajet.COMPLET, 4);
        createSeedReservation(completeLyon, lucas, StatutReservation.CONFIRMEE, StatutPaiement.PAYE);
        createSeedReservation(completeLyon, emma, StatutReservation.CONFIRMEE, StatutPaiement.PAYE);
        createSeedReservation(completeLyon, tom, StatutReservation.CONFIRMEE, StatutPaiement.PAYE);
        createSeedReservation(completeLyon, alex, StatutReservation.CONFIRMEE, StatutPaiement.PAYE);

        log.info("DataInitializer : {} trajets crees", trajetRepository.count());
    }

    private Trajet createSeedTrajet(
            Vehicule vehicule,
            Voyageur organisateur,
            LocalDateTime dateDebut,
            TypeTrajet typeTrajet,
            StatutTrajet statut,
            int nbPassagersActuels
    ) {
        return trajetRepository.save(Trajet.builder()
                .vehicule(vehicule)
                .chauffeur(organisateur)
                .dateDebut(dateDebut)
                .dateFin(dateDebut.plusMinutes(90))
                .typeTrajet(typeTrajet)
                .statut(statut)
                .nbPassagersActuels(nbPassagersActuels)
                .prixTotal(TrajetService.PRIX_TRAJET_EUROS)
                .prixParPassager(TrajetService.PRIX_TRAJET_EUROS / TrajetService.REQUIRED_PASSAGERS_PER_TRAJET)
                .build());
    }

    private void createSeedReservation(
            Trajet trajet,
            Voyageur voyageur,
            StatutReservation statutReservation,
            StatutPaiement statutPaiement
    ) {
        Reservation reservation = reservationRepository.save(Reservation.builder()
                .trajet(trajet)
                .voyageur(voyageur)
                .statut(statutReservation)
                .build());

        Paiement paiement = paiementRepository.save(Paiement.builder()
                .reservation(reservation)
                .montant(trajet.getPrixParPassager())
                .statut(statutPaiement)
                .datePaiement(statutPaiement == StatutPaiement.PAYE ? LocalDateTime.now() : null)
                .build());

        reservation.setPaiement(paiement);
    }
}
