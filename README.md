# GestionTransport-Multiple

Plateforme académique de covoiturage entre villes belges, construite sur l'application existante
avec **Java 21, Spring Boot, Angular et PostgreSQL 15**.

**Un trajet = un chauffeur déjà associé + quatre clients maximum.**
Le chauffeur ne réserve aucune place. Chaque client paie **25 €** ; quatre clients payants
représentent **100 €**. Les places inoccupées ne sont facturées à personne.
Les paiements et remboursements sont **simulés**, sans prestataire bancaire.

## Fonctionnement métier

- Départ et destination sont deux villes belges distinctes, choisies dans la liste centrale
  du backend. Bruxelles, Mons, Charleroi, Namur, Liège, Anvers, Gand, Louvain, Nivelles et
  Wavre sont proposés. Les retours sont des trajets indépendants ; Bruxelles n'est pas obligatoire.
- Un Voyageur autorisé à proposer un trajet devient automatiquement son chauffeur depuis
  son authentification. Un administrateur sélectionne un chauffeur existant dans son périmètre.
  Aucun chauffeur librement substituable depuis le navigateur.
  La création d'un voyageur par un administrateur demande un mot de passe initial personnel
  de 12 caractères minimum. Aucun mot de passe commun n'est attribué aux nouveaux comptes.
- Le véhicule et le chauffeur ne peuvent pas effectuer deux trajets incompatibles. Les horaires,
  durées estimées et indisponibilités configurés de l'agence sont conservés.
  Un véhicule marqué indisponible dans l'administration ne peut pas recevoir de nouveau trajet.
  Les anciennes ressources sont indisponibles par défaut et nécessitent une qualification
  explicite comme véhicules, sans modification de leur nom, identifiant ou données historiques.
- Un trajet PUBLIC peut être recherché ; les voyageurs locaux restent limités à leur zone.
  Un trajet PRIVE nécessite une invitation. La publication à H-24 exige le consentement enregistré.
- Une demande EN_ATTENTE et son paiement associé ne bloquent **aucune place**. Le premier
  paiement accepté confirme une place disponible, sous transaction et verrou PostgreSQL.
  À quatre confirmations, le trajet est COMPLET ; une cinquième confirmation est refusée.
- Une demande impayée expire après le délai configurable (30 minutes par défaut).
  Une annulation autorisée libère une place confirmée et marque son paiement REMBOURSE.
  Les réservations annulées ou expirées et les paiements restent dans l'historique.
- Une restriction active ou une dette empêche de réserver. Une restriction expirée cesse
  automatiquement de bloquer, sans suppression de son historique. La durée de référence est
  configurable, sept jours par défaut. L'ajout est protégé contre les doublons.
  Les déclenchements non définis (absence, abus, annulation tardive) restent des décisions
  métier à valider ; aucune sanction pour des places inoccupées n'est inventée.

| Catégorie | Matricule      | Fenêtre de réservation | Zone                          |
| --------- | -------------- | ---------------------- | ----------------------------- |
| VIP       | G + 4 chiffres | Jusqu'à J-21           | Toutes les agences autorisées |
| Local     | S + 5 chiffres | Jusqu'à J-14           | Agence autorisée              |
| Libre     | L + 5 chiffres | Jusqu'à J-5            | Trajets publics autorisés     |

Les fenêtres concernent les clients qui réservent, pas la date de création d'un trajet par son chauffeur.

## Architecture conservée

```text
src/main/java/com/transport/gestiontransport/
  controller/       API REST, validation et identité authentifiée
  dto/              Requêtes et réponses API
  mapper/           Conversion entre entités et DTO
  service/impl/     Règles métier, transactions et verrouillages
  repository/       Persistance et requêtes Spring Data JPA
  entity/           Agence, Vehicule, Trajet, Voyageur, Reservation,
                    Paiement, Restriction, IndisponibiliteService, Administrateur
  config/           Sécurité JWT, audit des dates, Swagger
  scheduler/        Publication consentie et expiration
frontend/src/app/
  core/             Services HTTP, sessions, guards, interceptor, internationalisation
  features/         Accueil, espace voyageur et administration
  shared/           Modèles, recherche et cartes réutilisables
```

Backend monolithique Spring Boot 3.5.0, Spring Security/JWT, JPA, validation et exceptions centralisées.
Frontend Angular 21, Angular Material, Tailwind CSS et formulaires réactifs.
Les versions du `pom.xml` et du verrou npm sont conservées. Le diff local de `pom.xml`,
notamment l'artifactId `industri`, reste préservé hors commit.

## Prérequis

- Java/JDK **21**.
- Docker Desktop avec moteur Linux opérationnel.
- Node.js **24** et npm **11** pour les versions Angular présentes.
- Git. Maven est fourni par le wrapper du dépôt.

Commandes PowerShell, depuis la racine du projet :

```powershell
java -version
node --version
npm --version
docker version
docker info
git status
```

## 1. PostgreSQL et Docker

```powershell
docker compose config --quiet
docker compose up -d
docker compose ps
docker compose logs --tail=100 postgres
```

Configuration locale de développement :

```text
Base         : transportService
Utilisateur  : transport
Mot de passe : transport
Port hôte    : 5440
Port interne : 5432
Version      : PostgreSQL 15
```

Le conteneur `padel-db` et le volume historique `padel-db-data` gardent leurs identités
pour conserver les données. Changer le nom du volume créerait un autre stockage.
Les variables POSTGRES\_\* initialisent uniquement une base neuve : elles ne renomment
pas un rôle ou une base existante.

Les migrations locales déjà réalisées sont documentées dans
[Exécution Transport](docs/transport-execution.md), avec les scripts
[sauvegarde/renommage du rôle](scripts/migrate-dev-db-user.ps1) et
[renommage de la base](scripts/rename-dev-database.ps1).
Ne pas les rejouer sur une installation déjà migrée.

Vérifier le serveur sans écrire de données :

```powershell
docker exec padel-db pg_isready -U transport -d transportService
docker exec padel-db psql -U transport -d transportService -c "SELECT version();"
docker volume ls
```

**Ne pas supprimer le volume ni utiliser `docker compose down -v`.**
Pour arrêter les services en conservant le stockage : `docker compose stop`.

## 2. Données existantes et schéma

Le mécanisme initial `spring.jpa.hibernate.ddl-auto=update` est conservé.
Un démarrage peut donc ajouter des colonnes : effectuer une sauvegarde avant d'utiliser
une base métier existante. Aucun traitement ne remplace les anciens montants.

Exemple de sauvegarde PostgreSQL, sans redirection binaire PowerShell :

```powershell
New-Item -ItemType Directory -Force .local-validation/backups | Out-Null
$archiveTransport = "transportService_$(Get-Date -Format yyyyMMdd_HHmmss).dump"
docker exec padel-db pg_dump -U transport -Fc -f "/tmp/$archiveTransport" transportService
docker cp "padel-db:/tmp/$archiveTransport" ".local-validation/backups/$archiveTransport"
```

Le statut EXPIREE nécessite le script SQL
[issue-4-reservation-expiration.sql](scripts/migrations/issue-4-reservation-expiration.sql)
sur un schéma historique. Après sauvegarde et revue, appliquer explicitement à la base choisie :

```powershell
Get-Content -Raw -Encoding UTF8 scripts/migrations/issue-4-reservation-expiration.sql |
  docker exec -i padel-db psql -U transport -d transportService -X -v ON_ERROR_STOP=1
```

Ce script modifie uniquement le CHECK des statuts, sans réécrire de réservation ou de paiement.
Pendant la validation du projet, il a été appliqué uniquement à la base de test isolée.

Le script [invariants PostgreSQL](scripts/migrations/issue-7-invariants-covoiturage.sql)
ajoute les index de recherche/créneau, l'unicité d'une demande active et le contrôle
des villes, horaires et quatre places pour les trajets qualifiés. Même procédure
explicite après sauvegarde ; il refuse les doublons existants, sans les supprimer.
Il est aussi exécuté par les tests PostgreSQL sur leur base isolée.

Les noms SQL historiques sont conservés pour éviter une migration destructive.
Les catégories VIP/LOCAL utilisent encore les valeurs SQL GLOBAL/SITE via le convertisseur existant.
Les anciens trajets sans villes renseignées ne sont pas vendus comme trajets de covoiturage :
ils restent dans l'historique, en attente d'une qualification explicite.
Aucune ville, date de création ou nouvelle tarification n'est inventée pour ces anciennes lignes.

## 3. Compiler et démarrer le backend

Compilation, tests et JAR :

```powershell
.\mvnw.cmd -B verify
```

Sans `POSTGRES_BASELINE_URL`, les tests usuels utilisent H2 et les tests PostgreSQL opt-in sont ignorés.
La compilation ne prouve pas le démarrage HTTP : ce dernier fait l'objet des vérifications décrites plus bas.

Créer une clé JWT neuve dans l'environnement, sans l'afficher :

```powershell
$octetsJwtTransport = New-Object byte[] 48
$generateurTransport = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try { $generateurTransport.GetBytes($octetsJwtTransport) }
finally { $generateurTransport.Dispose() }
$env:JWT_SECRET = [Convert]::ToBase64String($octetsJwtTransport)
$env:DB_URL = 'jdbc:postgresql://localhost:5440/transportService'
$env:DB_USER = 'transport'
$env:DB_PASSWORD = 'transport'
$env:DEMO_DATA_ENABLED = 'false'
.\mvnw.cmd spring-boot:run
```

Conserver `DEMO_DATA_ENABLED=false` sur toute base métier.
Pour une présentation, privilégier la base de démonstration isolée décrite ci-dessous.

| Variable                | Usage                                                         | Défaut                                 |
| ----------------------- | ------------------------------------------------------------- | -------------------------------------- |
| JWT_SECRET              | Clé JWT, au moins 32 octets, obligatoire                      | Aucun                                  |
| DB_URL                  | Connexion PostgreSQL                                          | localhost:5440/transportService        |
| DB_USER / DB_PASSWORD   | Identifiants du serveur                                       | transport / transport en développement |
| DEMO_DATA_ENABLED       | Activation explicite de la démonstration                      | false                                  |
| DEMO_PASSWORD           | Mot de passe démo, min. 12 caractères, max. 72 octets UTF-8 | Aucun                                  |
| RESERVATION_PAYMENT_TTL | Délai ISO-8601 de paiement                                    | PT30M                                  |
| RESTRICTION_DAYS        | Durée d'une restriction validée                               | 7                                      |
| SERVER_PORT             | Port HTTP Spring Boot                                         | 8080                                   |

## 4. Installer, tester et lancer Angular

Dans un autre terminal :

```powershell
cd frontend
npm ci
npm test
npm run build
npm start
```

- Interface : [http://localhost:4200](http://localhost:4200).
- Le proxy existant transmet `/api` à `http://localhost:8080`.
- Build de production : `frontend/dist/frontend/browser`.
- L'interceptor JWT et les guards existants sont réutilisés.
- Les nouveaux textes de recherche, cartes et accueil utilisent l'internationalisation FR/EN existante.
- Références responsive : 375, 768 et 1440 px.

[Aperçu de l'accueil](docs/images/accueil-transport-demonstration.png), capturé sur une
base de démonstration isolée : ces données ne constituent pas une offre de trajets en production.

Aucune bibliothèque UI supplémentaire, carte géographique, géolocalisation ou intégration bancaire n'est ajoutée.

## 5. Démonstration sur base isolée vide

Le générateur existant est limité au **profil dev**, au flag explicite et à une base vide nommée
`transport_demo_<suffixe>` ou `issue1_validation_<suffixe>`. Il refuse `transportService`,
ne migre aucun compte et ne modifie aucune base déjà renseignée.

Créer une nouvelle base dédiée, sans supprimer un stockage existant :

```powershell
$baseDemoTransport = 'transport_demo_' + (Get-Date -Format yyyyMMddHHmmss)
docker exec padel-db createdb -U transport $baseDemoTransport
$env:DB_URL = "jdbc:postgresql://localhost:5440/$baseDemoTransport"
$env:DB_USER = 'transport'
$env:DB_PASSWORD = 'transport'
$env:DEMO_DATA_ENABLED = 'true'
$secretDemoTransport = Read-Host 'Mot de passe de démonstration (min. 12 caractères, max. 72 octets UTF-8)' -AsSecureString
$pointeurDemoTransport = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secretDemoTransport)
try { $env:DEMO_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointeurDemoTransport) }
finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointeurDemoTransport) }
# Définir JWT_SECRET comme dans la section backend.
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Comptes créés uniquement dans cette base de démonstration :

| Connexion      | Identifiant         | Fonction                                  |
| -------------- | ------------------- | ----------------------------------------- |
| Voyageur       | G1001               | Chauffeur autorisé                        |
| Voyageur       | G1002               | Client VIP                                |
| Voyageur       | S10001              | Client local                              |
| Voyageur       | L10001              | Client libre                              |
| Administrateur | admin@example.test  | Administrateur global                     |
| Administrateur | agence@example.test | Gestionnaire de l'agence de démonstration |

Le mot de passe est celui fourni par `DEMO_PASSWORD`.
Les huit trajets de démonstration proposent chacun quatre places,
sans réservation du chauffeur. Ils ne sont jamais insérés automatiquement dans la base métier.

Parcours de présentation :

1. Se connecter comme client et rechercher Mons → Bruxelles.
2. Créer une demande, puis payer depuis « Mes réservations » : une place est confirmée.
3. Consulter les paiements et l'historique ; annuler une réservation autorisée.
4. Se connecter comme G1001 et proposer un trajet avec ses villes et son véhicule.
5. Créer un trajet privé, inviter des clients et choisir explicitement la publication H-24.
6. Se connecter comme administrateur, gérer les voyageurs autorisés à conduire,
   créer un trajet avec chauffeur sélectionné et consulter les statistiques du périmètre autorisé.

## 6. Tests PostgreSQL réels

Créer une base de test distincte de la base métier et de la démonstration :

```powershell
docker exec padel-db createdb -U transport issue1_validation_tests
$env:POSTGRES_BASELINE_URL = 'jdbc:postgresql://localhost:5440/issue1_validation_tests'
$env:DB_USER = 'transport'
$env:DB_PASSWORD = 'transport'
.\mvnw.cmd -B verify
Remove-Item Env:POSTGRES_BASELINE_URL
```

Les tests opt-in refusent une URL dont le nom ne suit pas `issue1_validation_<suffixe>`.
Ils ajoutent leurs fixtures sans vider la base après exécution. Une base neuve évite
les différences de contraintes d'un ancien schéma de test.

Couverture : chauffeur authentifié, refus sans chauffeur, villes différentes,
chauffeur exclu des places, quatre confirmations, cinquième refusée, concurrence
sur la dernière place, paiement 25 €, recettes 100 €, annulation/remboursement,
expiration, catégories J-21/J-14/J-5, agence locale, droits administrateurs,
accès privé, recherche, contraintes PostgreSQL et restrictions.

La CI GitHub ajoute PostgreSQL 15 isolé, Java 21, tests Maven, Node 24,
`npm ci`, tests Angular et build de production. Les résultats de CI doivent être
consultés sur la PR ; une configuration de workflow ne constitue pas une exécution réussie.

## 7. API REST et Swagger

- Swagger : [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).
- OpenAPI : [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs).
- Villes : `GET /api/trajets/villes`.
- Recherche : `GET /api/trajets/public?depart=MONS&arrivee=BRUXELLES&date=2026-12-01`.
  Adapter la date au jour de démonstration.
- Connexion voyageur : `POST /api/voyageurs/login`.
- Connexion administrateur : `POST /api/auth/login`.
- Création : `POST /api/trajets`.
- Demande : `POST /api/reservations`.
- Paiement simulé : `POST /api/paiements/reservation/{reservationId}/pay`.
- Historique : endpoints voyageur de réservations et paiements, protégés par JWT.

Erreurs de formulaire : 400 ; accès refusé : 403 ; ressource absente : 404 ;
conflit de persistance : 409. Les réponses ne présentent pas de détails SQL sensibles.

## 8. Traçabilité GitHub et documentation

Toutes les PR ciblent `develop`. Les branches fonctionnelles partent du dernier
`develop` validé. Aucune modification métier n'est committée dans `master`.

| Issue | Branche / travail                           | Pull request                                                                                                                           |
| ----- | ------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| #1    | chore/issue-1-audit-baseline                | [#8](https://github.com/Zoom225/GestionTransport-Multiple/pull/8)                                                                      |
| #1    | Nomenclature et paramètres locaux Transport | [#9](https://github.com/Zoom225/GestionTransport-Multiple/pull/9), [#12](https://github.com/Zoom225/GestionTransport-Multiple/pull/12) |
| #4    | Tarif 100 € / 25 €                          | [#10](https://github.com/Zoom225/GestionTransport-Multiple/pull/10)                                                                    |
| #2    | feature/issue-2-analyse-transport           | [#11](https://github.com/Zoom225/GestionTransport-Multiple/pull/11)                                                                    |
| #3    | feature/issue-3-domain-persistence          | [#13](https://github.com/Zoom225/GestionTransport-Multiple/pull/13)                                                                    |
| #4    | feature/issue-4-transport-services          | [#14](https://github.com/Zoom225/GestionTransport-Multiple/pull/14)                                                                    |
| #5    | feature/issue-5-transport-api               | [#15](https://github.com/Zoom225/GestionTransport-Multiple/pull/15)                                                                    |
| #6    | feature/issue-6-transport-frontend          | [#16](https://github.com/Zoom225/GestionTransport-Multiple/pull/16)                                                                    |
| #7 | feature/issue-7-integration-validation | [#17](https://github.com/Zoom225/GestionTransport-Multiple/pull/17) |
| #4, #6, #7 | feature/issue-4-soutenance-finalisation | [#18](https://github.com/Zoom225/GestionTransport-Multiple/pull/18) |

Documents à lire pour présenter le projet :

- [Analyse métier et cas d'utilisation](docs/covoiturage/analyse-metier.md).
- [Modèle relationnel, diagrammes et migration](docs/covoiturage/modele-donnees.md).
- [Modèle JPA simple](docs/covoiturage/issue-3-modele-simple.md).
- [Réservation, paiement et concurrence](docs/covoiturage/issue-4-reservation-paiement.md).
- [API et sécurité](docs/covoiturage/issue-5-api-securite.md).
- [Interface et responsive](docs/covoiturage/issue-6-interface.md).
- [Validation finale et préservation des données](docs/covoiturage/issue-7-validation.md).
- [Audit initial historique](docs/audit-initial.md) et [exécutions de baseline](docs/baseline-execution.md).

## Candidat de soutenance — finalisation issues #4 et #7

Dernière validation : **155 tests backend dont 15 PostgreSQL**, **63 tests Angular**,
builds réussis et [CI réussie](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/43277688472).
Le parcours réel de réservation/paiement/remboursement et neuf contrôles responsive
à 375, 768 et 1440 px ont réussi sur une base de démonstration isolée.

Les archives locales sont conservées dans `.local-validation/`, hors Git et hors
`target` : `mvn clean` ne les supprime pas. Le diff local de `pom.xml` reste hors commit.

Les huit parcours de démonstration sont créés à J+2, de 07 h à 21 h, avec un seul
chauffeur et véhicule, 90 minutes par trajet et aucune réservation initiale : Mons,
Bruxelles, Charleroi, Namur, Liège, Anvers, Gand et Louvain. Six trajets sont publics,
deux privés. Un redémarrage sur une base non vide ne réécrit aucun compte ni paiement.
Le profil `dev`, `DEMO_DATA_ENABLED=true`, une base isolée nommée `transport_demo_*`
et `DEMO_PASSWORD` de 12 caractères minimum et 72 octets UTF-8 maximum (BCrypt)
sont tous requis. Aucun chargement en production.

### Restrictions : fonctionnement et décisions métier

Une restriction conserve son motif, sa période `[dateDebut, dateFin[` et une référence
unique de fait déclencheur. La date de début historique reste inconnue si elle n'était
pas enregistrée. Le verrou du voyageur et l'unicité SQL empêchent les doublons, y compris
en concurrence. Une restriction expirée cesse automatiquement de bloquer la réservation,
sans suppression de son historique. La durée de référence est `RESTRICTION_DAYS=7`.

| Variable | Valeur par défaut | Activation explicite |
| --- | --- | --- |
| LATE_CANCELLATION_ENABLED | false | Annulation par le client d'une réservation confirmée avant le départ |
| LATE_CANCELLATION_HOURS | 0 | Seuil positif à valider avant activation |
| NO_SHOW_PENALTY_ENABLED | false | Absence explicitement déclarée par un administrateur autorisé |
| DRIVER_CANCELLATION_PENALTY_ENABLED | false | Annulations de trajets par leur chauffeur |
| DRIVER_CANCELLATION_THRESHOLD | 0 | Nombre d'annulations à valider |
| DRIVER_CANCELLATION_WINDOW_DAYS | 0 | Fenêtre d'observation positive à valider |

Une politique activée sans seuil valide empêche le démarrage. Aucun montant de sanction
n'est ajouté. Une demande en attente ou expirée ne crée pas automatiquement une dette.
Un solde exigible positif bloque toujours réservation et paiement de confirmation.

- Administration des trajets : `POST /api/reservations/{id}/absence`, JSON
  `{ "motif": "Absence injustifiée vérifiée" }`. Réservation payée et confirmée,
  départ passé, trajet non annulé, administrateur du bon périmètre requis. La date,
  le motif et l'administrateur déclarant sont conservés ; une répétition ne les écrase pas.
- Administration des voyageurs : `GET /api/voyageurs/{id}/restrictions` retourne
  l'historique et l'état actif calculé, selon le périmètre d'agence de l'administrateur.
- Migration additive : [issue-4-restrictions-soutenance.sql](scripts/migrations/issue-4-restrictions-soutenance.sql).
  Revue et sauvegarde requises avant application sur une base existante ; aucun paiement réécrit.

### Démonstration devant le jury

Suivre les commandes Docker, backend et frontend des sections 1 à 6. Ouvrir Swagger,
puis rechercher Mons → Bruxelles dans Angular. Se connecter avec le client VIP de la
base isolée, réserver et payer **25 € simulés** : trois places restent disponibles.
Quatre paiements donnent **100 €** et aucune cinquième confirmation. Une annulation
autorisée conserve l'historique, rembourse le paiement simulé et libère la place.
L'administration présente les réservations, recettes et restrictions selon les droits.
Les commandes et preuves de validation figurent dans
[issue-7-validation.md](docs/covoiturage/issue-7-validation.md).

La CI contrôle désormais les PR vers `develop` **et `master`**. La livraison vers
`master` se fait par PR distincte après validation de `develop`, sans fusion automatique.

## Dépannage

- IntelliJ : utiliser la configuration partagée **GestionTransport-Multiple**
  (`.run/GestionTransport-Multiple.run.xml`), qui exécute `spring-boot:run` via Maven
  sans dépendre du nom local du module. Configurer les variables DB/JWT comme ci-dessus.
  Pour une configuration Spring Boot personnelle, la classe principale est
  `com.transport.gestiontransport.GestionTransportApplication` ; les tests se trouvent
  dans `com.transport.gestiontransport`. Après un renommage, vérifier ces deux champs
  dans **Run → Edit Configurations**, puis recharger le projet Maven.

- Sous Windows, arrêter une instance lancée avec `java -jar` avant de régénérer
  son JAR : le processus peut empêcher Maven de renommer le fichier pendant l'empaquetage.

- Docker inaccessible : vérifier `docker version`, `docker info`, `docker context ls`
  et `wsl --status`. Ne pas réinitialiser Docker ni supprimer ses volumes.
- Erreur de connexion PostgreSQL : vérifier le port 5440, les variables DB\_\* et l'état healthy.
- Backend arrêté : vérifier Java 21 et JWT_SECRET ; les tests unitaires seuls ne prouvent pas le démarrage.
- Recherche vide : seuls les trajets futurs, qualifiés avec deux villes et encore disponibles sont proposés.
- Création refusée : vérifier la permission chauffeur, le véhicule, l'agence, les horaires et chevauchements.
- Paiement refusé : actualiser la disponibilité, vérifier le délai, la catégorie, la zone et les restrictions.
- Ancien montant ou trajet non qualifié : régularisation métier explicite, sans réécriture automatique des paiements.
