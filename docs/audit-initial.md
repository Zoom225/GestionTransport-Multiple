# Audit initial — GestionTransport-Multiple

Issue #1 — phase 0 — 11 novembre 2026.

## Git et modifications préexistantes

État initial : master à 688190e, seule modification locale dans pom.xml :
artifactId padelmultiple remplacé par industri. Contenu préservé, non inclus dans
les commits de baseline. git fetch origin exécuté. Branche de travail :
chore/issue-1-audit-baseline, issue de la branche distante existante à 509b630.
PR #8 confirmée ouverte en brouillon et ciblant develop. Aucun changement métier
Transport ni phase dépendante engagé ; aucune fusion effectuée.

## Architecture examinée

- Java 21.0.9, Spring Boot 3.5.0, JPA, validation Jakarta, Security, JWT jjwt
  0.12.5, springdoc 2.8.8, Lombok 1.18.36. Versions conservées.
- 86 sources Java : controllers, services/interfaces et implémentations,
  repositories, DTO request/response, mappers, entités et enums, exceptions
  centralisées. AuthController délègue à AuthService ; SecurityConfig définit
  les rôles MEMBER/ADMIN et les endpoints anonymes ; le filtre JWT est enregistré.
  AdminAuthorizationService assure des contrôles de périmètre métier.
- Transactions présentes dans les services ; MatchRepository.findByIdForUpdate
  utilise PESSIMISTIC_WRITE. Aucune preuve de concurrence PostgreSQL dans cet audit.
- GlobalExceptionHandler masque le détail des erreurs génériques côté client,
  mais utilise printStackTrace ; validation et exceptions métier existent.
- Angular 21, Material, Tailwind 3, TypeScript 5.9, Vitest 4 : routes, guards,
  sessions membre/admin, intercepteur HTTP enregistré dans app.config.ts,
  services REST et traductions présents. Le build et les tests doivent compléter
  l'examen statique ; présence de fichiers seule ne prouve pas le fonctionnement.
- PostgreSQL 15 dans Docker Compose, port 5440, volume padel-db-data.
  Pas de conteneur backend/frontend dans ce Compose. ddl-auto=update conservé
  pour l'application ; aucune migration Flyway/Liquibase introduite.
- Maven installé 3.9.15 ; wrapper configuré pour 3.9.16, non exécuté.
  Node 24.19.0, npm local 11.17.0 (packageManager déclare 11.12.1).

## Résultats exécutés

| Commande | Résultat réel |
| --- | --- |
| git status, git diff -- pom.xml, git fetch origin | Exécutés avant changement de branche ; diff conservé |
| mvn -B -DskipTests compile | BUILD SUCCESS, 86 sources, release 21 |
| mvn -B test avant correction | 109 tests, 0 échec, 1 erreur : contextLoads, connexion PostgreSQL localhost:5440 refusée |
| mvn -B test après correction | BUILD SUCCESS, 109 tests, 0 échec, 0 erreur, 0 ignoré ; 2 min 50 s |
| docker version | Client 29.4.2 ; erreur : Docker Desktop is unable to start |
| docker compose config --quiet | Exécuté avec succès ; ne valide pas le démarrage |
| npm ci | Installation initiale sans résultat après plusieurs minutes, arrêtée |
| npm run build / npm test pendant installation incomplète | ng / vitest introuvables ; résultats non valides pour juger le code |

Les logs locaux baseline-*.log restent dans target/ (ignoré par Git). Les tests
JUnit/Mockito, MockMvc et DataJpaTest passent après isolation. Ils ne valident
ni une application Transport, ni les particularités PostgreSQL, ni le parcours
navigateur complet.

## Corrections de baseline

1. src/test/resources/application.properties isole TOUS les tests sur H2 mémoire,
   avec une clé JWT réservée aux tests et l'initialiseur désactivé. Les tests ne
   nécessitent plus une base locale susceptible de contenir des données métier.
2. DataInitializer devient opt-in via app.demo-data.enabled. Son exécution
   automatique pouvait créer des matchs et réécrire des identifiants existants.
   Son comportement explicite reste inchangé pour la démonstration.
3. jwt.secret provient désormais de JWT_SECRET, sans secret par défaut. DB_URL,
   DB_USER et DB_PASSWORD peuvent être fournis par environnement. Les valeurs
   PostgreSQL locales de développement restent compatibles avec Compose.
4. .gitignore exclut les fichiers .env et les logs locaux.
5. docs/baseline-execution.md décrit les prérequis et les limites.

## Sécurité et données

Aucun fichier *.log ou .env suivi selon git ls-files. Une clé JWT était versionnée
ainsi que des comptes de démonstration et des identifiants PostgreSQL publics.
La clé retirée du fichier courant demeure dans l'historique : rotation nécessaire
sur tout environnement concerné ; aucune réécriture d'historique effectuée.
Les mots de passe de démonstration restent dans l'initialiseur désormais opt-in.
Ne jamais activer cette option sur une base métier. Aucun volume supprimé,
aucune commande de migration ou d'effacement lancée ; PostgreSQL inaccessible.

## Points à traiter avant validation et suite

- Rétablir Docker Desktop puis vérifier PostgreSQL, démarrage REST et Swagger
  sur une base de validation distincte sauvegardée.
- Terminer l'installation reproductible et valider Angular.
- Faire valider la PR #8 avant toute fusion et avant la phase 1.
- Les règles Padel (quatre joueurs, prix 60, durées) restent volontairement dans
  le socle : aucune RM01–RM30 Transport n'est déclarée implémentée en phase 0.
- Concevoir les migrations et règles Transport dans #2, puis les tester dans
  les phases #3–#7. Aucun terrain ne doit devenir un véhicule par renommage.

## Bilan frontend final

- Une tentative intermédiaire npm ci a échoué avec ENOTEMPTY pendant que
  l'installation initiale était encore active. L'installation initiale a été
  arrêtée avant la dernière tentative exécutée seule.
- npm.cmd ci --fetch-retries=0 --fetch-timeout=30000 --no-audit --no-fund :
  535 paquets installés en 5 minutes. Avertissement allow-scripts pour
  @parcel/watcher, esbuild, lmdb et msgpackr-extract ; aucun script supplémentaire
  autorisé. Ces avertissements n'ont pas empêché les validations suivantes.
- npm.cmd run build : bundle Angular généré, 502,02 kB initiaux ; sortie
  frontend/dist/frontend. Aucun changement de budget ni de dépendance.
- npm.cmd test : 12 fichiers et 58 tests réussis, durée 68,11 secondes.
- git diff -- frontend/package-lock.json : aucun changement.

La validation Angular demandée est donc réalisée. Restent bloqués le démarrage
Docker/PostgreSQL et la vérification HTTP REST/Swagger sur cette base. Phase 0
non validée globalement ; PR à conserver en brouillon, phase 1 non commencée.

## Reprise technique — Docker, PostgreSQL et HTTP (11 novembre 2026)

### Diagnostic initial exact

| Commande | Observation |
| --- | --- |
| docker version | Client 29.4.2 ; Error response from daemon: Docker Desktop is unable to start |
| docker info | Même erreur côté Server |
| docker context ls | desktop-linux actif, endpoint npipe dockerDesktopLinuxEngine ; contexte default également présent |
| Get-Service com.docker.service | Stopped |
| wsl --status | Distribution par défaut docker-desktop, version par défaut 2 |
| wsl --list --verbose | docker-desktop, Stopped, version 2 |
| wsl --version | WSL 3.0.5.0, noyau 6.18.58.1-1 |
| docker desktop status | stopped |

Les services WslService, vmcompute et hns sont Running. CIM indique
HypervisorPresent=True, mais VirtualizationFirmwareEnabled=False et SLAT=False.
Ces indicateurs ne prouvent pas une virtualisation désactivée puisque le moteur
Linux a ensuite démarré. bcdedit /enum a répondu : « Impossible d'ouvrir le magasin
des données de configuration de démarrage. Accès refusé. » Aucune élévation tentée.

Le journal host/monitor.log contient :
- wsl.exe --version failed: exit status 1 (stdout: "", stderr: "") ;
- starting engine: engine linux/wsl failed to start: checking preconditions: WSL update required.

L'état backend contenait wslUpdateRequired=true, malgré une commande WSL lisible
lors du diagnostic. Le journal précédent contient aussi un échec de bootstrap
DockerDesktop/Wsl/ExecError ; les traces historiques mélangent plusieurs dates.
On identifie donc le blocage de précondition, sans prouver la cause initiale
de l'échec de détection WSL. docker desktop start répondait « already running »
alors que le moteur était arrêté. docker desktop restart a rétabli le moteur :
Docker Desktop 4.72.0, Engine Linux 29.4.2. Aucun redémarrage Windows, aucune
mise à jour ni réinitialisation Docker/WSL, aucune modification du BIOS/BCD.

### Volume et PostgreSQL

- Volume padellaoutt_padel-db-data inspecté : volume local créé le 4 août 2026,
  métadonnées Compose cohérentes avec le dépôt. Son contenu n'a pas été monté
  manuellement ni effacé.
- docker compose config, up -d, ps et logs exécutés. Image postgres:15 téléchargée.
- PostgreSQL 15.19 sain ; 5440 côté hôte vers 5432 dans le conteneur.
- Log : « PostgreSQL Database directory appears to contain a database; Skipping initialization ».
- Base préexistante padelService : neuf tables publiques. Aucun démarrage Hibernate
  ni test dirigé vers cette base. Aucun down -v, DROP ou effacement effectué.
- Bases explicitement créées : issue1_validation_pg_20261111 et
  issue1_validation_http_20261111 ; réservées aux fixtures et au schéma de validation.
  Elles sont conservées et contiennent les données de ces contrôles.

### Démarrage et contrôles HTTP

Les cinq variables DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET, DEMO_DATA_ENABLED
étaient absentes de l'environnement initial. Pour le serveur de validation,
connexion explicitement définie vers la base HTTP isolée ; nouvelle clé JWT de
48 octets générés par RNG cryptographique, fournie par environnement et jamais
imprimée. DEMO_DATA_ENABLED=false ; port 18080 ; profil par défaut.
ddl-auto=update conservé et exécuté exclusivement sur la base neuve isolée.

Spring Boot démarre et Hikari se connecte à PostgreSQL 15.19. Tomcat écoute
18080. Les tables sites et membres de la base HTTP restent vides : aucune
initialisation de démonstration. Contrôles exécutés par curl :

| Requête | HTTP et résultat |
| --- | --- |
| GET /swagger-ui.html | 302, puis 200 après redirection |
| GET /v3/api-docs | 200, document OpenAPI 3.1.0 |
| GET /api/sites | 200, lecture via repository sur PostgreSQL |
| GET /api/membres sans JWT | 403 |
| POST /api/auth/login avec {} | 400, erreurs email et password obligatoires |

La réponse de validation ne contient pas de trace technique mais son message
global présente un défaut d'encodage hérité ; les messages des champs sont
lisibles. À corriger lors du travail API (#5). Les logs de démarrage indiquent
les avertissements Hibernate de dialecte explicite et open-in-view ; aucune
preuve de paiement externe ou de parcours navigateur complet n'est revendiquée.

### Test PostgreSQL et correction de concurrence

PostgresBaselineTest, opt-in et limité aux noms issue1_validation_<suffixe>,
utilise le vrai driver PostgreSQL et les vrais services/repositories. L'unicité
matricule et la clé étrangère reservations.match_id sont réellement vérifiées
(SQLSTATE 23505 et 23503 attendus). Le test orchestre deux transactions ayant lu
le même état avant leurs réservations, sans mock du service métier.

Première exécution : 2 tests, 1 échec, 0 erreur. Deux réservations réussissaient
pour la dernière place (expected 1, but was 2). Le compteur pouvait masquer la
sur-réservation par perte de mise à jour.

Correction : ReservationServiceImpl rafraîchit le match sous PESSIMISTIC_WRITE
avant de vérifier capacité et doublon. Cela recharge aussi une entité déjà lue
dans la transaction appelante ; le verrou dure jusqu'au commit/rollback.
La règle Padel existante n'est pas modifiée. Aucune entité ni migration Transport
introduite. Le test Mockito reçoit un EntityManager simulé ; la régression de
concurrence est vérifiée sur PostgreSQL réel.

La première tentative de correction (refresh avec mode de verrou en un seul
appel) a encore échoué dans la suite complète : 111 tests, 1 échec, 0 erreur.
La correction finale acquiert explicitement le verrou avec EntityManager.lock,
puis recharge l'état avec EntityManager.refresh, avant tout contrôle métier.
Le scénario ciblé PostgreSQL a alors réussi : 2 tests, 0 échec, 0 erreur.

Après compilation de cette correction, les contrôles HTTP ont été rejoués :
Swagger 200 après redirection, OpenAPI 200 (35 chemins), sites 200 avec [],
/api/membres sans JWT 403, avec JWT invalide 403, connexion invalide 400.
Le processus DevTools a redémarré lors des recompilations ; une requête pendant
ce redémarrage a renvoyé 000, puis les contrôles finaux ci-dessus ont tous répondu.
Ces contrôles restent limités à la base isolée sans comptes de démonstration.

## Bilan final de cette reprise

Commande finale : mvn -B test avec POSTGRES_BASELINE_URL pointant exclusivement
vers issue1_validation_pg_20261111. Résultat : BUILD SUCCESS, 111 tests,
0 échec, 0 erreur, 0 ignoré, durée 52,492 secondes. Les 109 tests initiaux et
les deux nouveaux tests PostgreSQL ont tous été exécutés dans cette suite.
Les compilations Maven des sources Java 21 et des tests ont été exécutées.
Les 58 tests Angular et son build réussis lors de la première session restent
les derniers résultats frontend ; non rejoués ici, aucun fichier frontend changé.

Les contrôles techniques restant bloqués lors du premier bilan sont désormais
réalisés. La validation de la PR par revue reste attendue avant fusion ; aucune
fusion et aucune branche phase 1 créée. Le défaut d'encodage de message hérité
et les limites de couverture décrits plus haut restent documentés.

Fichiers de cette reprise : docs/audit-initial.md, docs/baseline-execution.md,
ReservationServiceImpl.java, ReservationServiceTest.java, PostgresBaselineTest.java.
Règle du socle concernée : disponibilité de la dernière place sous concurrence.
Aucune RM Transport n'est déclarée livrée. Le diff local pom.xml artifactId=industri
reste conservé et exclu des commits. Les bases de validation restent distinctes
de padelService (toujours neuf tables) ; sites et membres restent vides sur la
base HTTP. Le serveur temporaire de contrôle est arrêté à la fin ; PostgreSQL
Compose est conservé sain et les volumes restent présents.
