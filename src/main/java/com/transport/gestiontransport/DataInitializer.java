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

/**
 * COMPTES DE VALIDATION — identifiants et mots de passe pour les essais locaux.
 * <pre>
 * +----------------------+-------------------------+----------------+
 * | Compte               | Identifiant             | Mot de passe   |
 * +----------------------+-------------------------+----------------+
 * | Admin global         | admin@transp.com        | Admin1234!     |
 * | Admin local Lyon     | admin.lyon@transp.com   | Admin1234!     |
 * | Admin local Paris    | admin.paris@transp.com  | Admin1234!     |
 * | Voyageur VIP         | V1001                   | Voyage1234!    |
 * | Voyageur VIP         | V1002                   | Voyage1234!    |
 * | Voyageur local       | L10001                  | Voyage1234!    |
 * | Voyageur local       | L10002                  | Voyage1234!    |
 * | Voyageur libre       | F10001                  | Voyage1234!    |
 * | Voyageur libre       | F10002                  | Voyage1234!    |
 * +----------------------+-------------------------+----------------+
 * </pre>
 * Copie locale configurée : les neuf comptes ci-dessus sont disponibles.
 * Base dev vide : création de V1001, V1002, L10001, F10001 et des trois admins.
 * Les mots de passe ci-dessus sont les valeurs de validation par défaut ;
 * DEMO_PASSWORD ou les variables DEMO_ADMIN_PASSWORD/DEMO_VOYAGEUR_PASSWORD
 * peuvent les remplacer lors de la préparation d'une démonstration.
 * Initialisation désactivée par défaut, jamais sur la base métier réelle.
 * Mise à jour des comptes existants uniquement sur copie locale et demande explicite.
 */
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
    @Value("${DEMO_ADMIN_PASSWORD:Admin1234!}") private String demoAdminPassword = "Admin1234!";
    @Value("${DEMO_VOYAGEUR_PASSWORD:Voyage1234!}") private String demoVoyageurPassword = "Voyage1234!";
    @Value("${DEMO_UPDATE_CREDENTIALS:false}") private boolean updateCredentials;

    @Override
    @Transactional
    public void run(String... args) {
        if (agenceRepository.count() + vehiculeRepository.count() + voyageurRepository.count()
                + administrateurRepository.count() + trajetRepository.count() + reservationRepository.count()
                + paiementRepository.count() + restrictionRepository.count() + indisponibiliteServiceRepository.count() > 0) {
            if (updateCredentials) {
                updateValidationCredentials();
                return;
            }
            log.info("Démonstration ignorée : la base contient déjà des données. Aucun compte ni paiement modifié.");
            return;
        }
        if (databaseUrl == null || !databaseUrl.matches("jdbc:postgresql://[^/]+/(issue1_validation_[a-z0-9_]+|transport_demo_[a-z0-9_]+)(\\?.*)?")) {
            throw new IllegalStateException("La démonstration exige une base isolée explicitement nommée.");
        }
        if (demoPassword != null && !demoPassword.isBlank()
                && (demoPassword.length() < 12 || demoPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)) {
            throw new IllegalStateException("DEMO_PASSWORD doit contenir au moins 12 caractères et au maximum 72 octets UTF-8 pour BCrypt.");
        }
        String adminPassword = demoPassword != null && !demoPassword.isBlank() ? demoPassword : demoAdminPassword;
        String voyageurPassword = demoPassword != null && !demoPassword.isBlank() ? demoPassword : demoVoyageurPassword;
        for (String password : new String[]{adminPassword, voyageurPassword}) {
            if (password == null || password.length() < 10 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
                throw new IllegalStateException("Les mots de passe de démonstration exigent au moins 10 caractères et au maximum 72 octets UTF-8.");
            }
        }
        Agence agence = agenceRepository.save(Agence.builder().nom("Agence Bruxelles — DÉMONSTRATION")
                .adresse("Bruxelles, Belgique").heureOuverture(LocalTime.of(6, 0)).heureFermeture(LocalTime.of(23, 0))
                .dureeTrajetMinutes(90).dureeEntreTrajetMinutes(15).anneeCivile(LocalDate.now().getYear()).build());
        Vehicule voiture = vehiculeRepository.save(Vehicule.builder().nom("Voiture DÉMONSTRATION").agence(agence).disponible(true).build());
        String hash = passwordEncoder.encode(voyageurPassword);
        String adminHash = passwordEncoder.encode(adminPassword);
        Voyageur chauffeur = voyageurRepository.save(Voyageur.builder().matricule("V1001").nom("Démo").prenom("Alice")
                .email("chauffeur@example.test").passwordHash(hash).typeVoyageur(TypeVoyageur.VIP)
                .solde(0.0).peutProposerTrajet(true).agence(agence).build());
        voyageurRepository.save(Voyageur.builder().matricule("V1002").nom("Démo").prenom("Client VIP")
                .email("vip@example.test").passwordHash(hash).typeVoyageur(TypeVoyageur.VIP)
                .solde(0.0).agence(agence).build());
        voyageurRepository.save(Voyageur.builder().matricule("L10001").nom("Démo").prenom("Client local")
                .email("local@example.test").passwordHash(hash).typeVoyageur(TypeVoyageur.LOCAL)
                .solde(0.0).agence(agence).build());
        voyageurRepository.save(Voyageur.builder().matricule("F10001").nom("Démo").prenom("Client libre")
                .email("libre@example.test").passwordHash(hash).typeVoyageur(TypeVoyageur.LIBRE)
                .solde(0.0).agence(agence).build());
        administrateurRepository.save(Administrateur.builder().matricule("AD1001").nom("Démo").prenom("Admin global")
                .email("admin@transp.com").passwordHash(adminHash).typeAdministrateur(TypeAdministrateur.GLOBAL).build());
        administrateurRepository.save(Administrateur.builder().matricule("AD1002").nom("Démo").prenom("Admin local")
                .email("admin.lyon@transp.com").passwordHash(adminHash).typeAdministrateur(TypeAdministrateur.AGENCE).agence(agence).build());
        administrateurRepository.save(Administrateur.builder().matricule("AD1003").nom("Démo").prenom("Admin local 2")
                .email("admin.paris@transp.com").passwordHash(adminHash).typeAdministrateur(TypeAdministrateur.AGENCE).agence(agence).build());
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

    private void updateValidationCredentials() {
        if (databaseUrl == null || !databaseUrl.matches("jdbc:postgresql://(localhost|127\\.0\\.0\\.1):\\d+/(transport_schema_copy_[a-z0-9_]+|transport_demo_[a-z0-9_]+)")) {
            throw new IllegalStateException("Mise à jour des comptes autorisée uniquement sur une copie locale de validation.");
        }
        java.util.Map<String, String> emails = java.util.Map.of(
                "admin@padel.com", "admin@transp.com", "admin@transp.com", "admin@transp.com",
                "admin.lyon@padel.com", "admin.lyon@transp.com", "admin.lyon@transp.com", "admin.lyon@transp.com",
                "admin.paris@padel.com", "admin.paris@transp.com", "admin.paris@transp.com", "admin.paris@transp.com");
        var admins = administrateurRepository.findAll().stream()
                .filter(a -> emails.containsKey(a.getEmail())).toList();
        if (admins.size() != 3 || admins.stream().map(a -> emails.get(a.getEmail())).distinct().count() != 3) {
            throw new IllegalStateException("Les trois comptes administrateur attendus doivent être présents sans doublon.");
        }
        var voyageurs = voyageurRepository.findAll();
        java.util.Map<Long, String> matricules = new java.util.HashMap<>();
        for (Voyageur voyageur : voyageurs) {
            String ancien = voyageur.getMatricule();
            int chiffres = voyageur.getTypeVoyageur() == TypeVoyageur.VIP ? 4 : 5;
            if (ancien == null || !ancien.matches("[A-Z]\\d{" + chiffres + "}")) {
                throw new IllegalStateException("Matricule historique à examiner avant la mise à jour des comptes.");
            }
            String prefixe = switch (voyageur.getTypeVoyageur()) {
                case VIP -> "V";
                case LOCAL -> "L";
                case LIBRE -> "F";
            };
            matricules.put(voyageur.getId(), prefixe + ancien.substring(1));
        }
        if (matricules.values().stream().distinct().count() != voyageurs.size()) {
            throw new IllegalStateException("Les nouveaux matricules créeraient un doublon : aucune mise à jour effectuée.");
        }
        // Libérer les anciens L des voyageurs libres avant de les affecter aux locaux.
        if (voyageurs.stream().anyMatch(v -> !v.getMatricule().equals(matricules.get(v.getId())))) {
            voyageurs.forEach(v -> v.setMatricule("__validation_" + v.getId()));
            voyageurRepository.saveAllAndFlush(voyageurs);
            voyageurs.forEach(v -> v.setMatricule(matricules.get(v.getId())));
        }
        for (Voyageur voyageur : voyageurs) {
            if (!passwordMatches(demoVoyageurPassword, voyageur.getPasswordHash())) {
                voyageur.setPasswordHash(passwordEncoder.encode(demoVoyageurPassword));
            }
        }
        voyageurRepository.saveAllAndFlush(voyageurs);
        for (Administrateur admin : admins) {
            admin.setEmail(emails.get(admin.getEmail()));
            if (!passwordMatches(demoAdminPassword, admin.getPasswordHash())) {
                admin.setPasswordHash(passwordEncoder.encode(demoAdminPassword));
            }
        }
        administrateurRepository.saveAllAndFlush(admins);
        log.info("Comptes de validation mis à jour : identifiants et mots de passe uniquement, relations et historiques conservés.");
    }

    private boolean passwordMatches(String password, String hash) {
        return hash != null && !hash.isBlank() && passwordEncoder.matches(password, hash);
    }
}
