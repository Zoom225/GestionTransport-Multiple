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
