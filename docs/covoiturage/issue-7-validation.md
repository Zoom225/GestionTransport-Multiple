# Issue #7 — Intégration et validation

## Correction des configurations IntelliJ — issue #7

Branche : `fix/issue-7-intellij-transport`, depuis `develop` à `b818392`.
Le signalement sur DataInitializer a conduit à vérifier aussi les configurations
locales ignorées par Git : le lancement Spring Boot visait encore l'ancienne
classe principale et une configuration JUnit ciblait l'ancien package.

- Classe principale locale corrigée : `com.transport.gestiontransport.GestionTransportApplication`.
- Configuration JUnit locale corrigée : package `com.transport.gestiontransport`,
  filtre et références de configuration cohérents. Module local `industri` conservé.
- Les deux dossiers historiques vides sous `src/main/java/com` et `src/test/java/com`
  sont supprimés après contrôle de leur contenu ; aucune classe ni aucun test supprimé.
- `.run/Unnamed.run.xml`, lié au nom de module `padelmultiple`, est remplacé par
  `.run/GestionTransport-Multiple.run.xml` : lancement Maven `spring-boot:run`,
  indépendant de l'artifactId local. Aucun secret ajouté à cette configuration.
- README : classe de lancement, package des tests et utilisation de la configuration partagée.

Les réglages personnels de `.idea/workspace.xml` ne sont pas publiés ; leur sauvegarde
locale reste dans `.local-validation/intellij-refactoring`. Le diff de `pom.xml`
(`industri`) est conservé hors commit. Aucune donnée PostgreSQL ni règle métier modifiée.

Validation exécutée : reconstruction IntelliJ réussie ; puis
`mvn -B -Dtest=DataInitializerTest clean test` réussi, **91 sources Java et 21 sources
de tests recompilées**, **4 tests réussis**, zéro échec/erreur/ignoré. Aucun build IDE
simultané à Maven. Les avertissements MockBean préexistants restent non bloquants.
Recherche finale : aucune référence à l'ancien package dans `src`, `.idea` et `.run`.

### Complément : variable JWT absente dans le lancement IntelliJ

Le journal transmis ensuite montre `Could not resolve placeholder 'JWT_SECRET'` :
le lancement utilise bien la classe Transport mais la configuration Spring Boot
personnelle n'a aucune variable JWT. Le script `scripts/configure-intellij-jwt.ps1`
ajoute une clé aléatoire uniquement si elle manque dans cette configuration locale,
après vérification que le fichier et sa sauvegarde sont ignorés par Git. Une clé
existante est préservée et une clé trop courte est signalée. Aucune valeur n'est affichée,
aucun secret n'est ajouté au dépôt ou à `application.properties`.

Vérifications exécutées : deux exécutions du script, clé inchangée au second passage ;
compilation IntelliJ réussie ; lancement réel de **GestionTransportApplication** via
l'IDE, avec les seules variables de base/port remplacées temporairement pour le test.
Base isolée `transport_demo_jwt_20261112213009`, serveur PostgreSQL de validation
sur 5541, `DEMO_DATA_ENABLED=false`, port HTTP 18081 : démarrage Spring Boot réussi
en 18 secondes, **OpenAPI et Swagger UI HTTP 200**. La clé JWT utilisée vient bien
de la configuration IntelliJ corrigée. Instance de vérification arrêtée après contrôle ;
aucune modification de la base métier.

## Finalisation soutenance du 12 novembre 2026 — PR #18

Branche : `feature/issue-4-soutenance-finalisation`, depuis `develop` après fusion
de la PR #17 (`679c0c3`). Issues #4 (restrictions), #6 (administration Angular)
et #7 (validation/livraison). `master` n'a pas été modifiée directement.

Les résultats des sections suivantes sur la PR #17 sont conservés comme historique.
Pour la PR #18, les logs de [CI 43277688472](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/43277688472)
sur `715f02c181a755cdc379b2ecfa45185d88561d6a` établissent :

| Contrôle effectivement exécuté en CI | Résultat |
| --- | --- |
| Java 21, `bash ./mvnw -B verify` | 155 tests, zéro échec, zéro erreur, zéro ignoré ; build réussi |
| PostgreSQL 15 isolé | 15 tests réels inclus, contraintes, concurrence, absence administrative, dette et restrictions |
| Node 24, `npm ci` | Installation réussie depuis le verrou npm |
| Angular 21.2.26, `npm test` | 63 tests réussis |
| Angular, `npm run build` | Build de production réussi |

Localement, `mvn -B clean verify` a aussi réussi avec 154 tests avant l'ajout du
contrôle BCrypt (155 ensuite en CI). Les relances locales sur le port 5440 ont
rencontré `SocketTimeoutException: Read timed out`, puis une disparition du conteneur
Docker et `Connection refused`. Le volume historique est resté présent. La remise
en service par `docker compose up -d` a réutilisé `padellaoutt_padel-db-data` ; les
huit paiements métier étaient toujours présents. Aucune suppression ni réinitialisation.
Une instance PostgreSQL de validation distincte, sans volume métier, est ensuite
utilisée sur le port 5541. Ces incidents ne sont pas présentés comme des tests réussis.

### Résultats locaux finaux

- `mvn -B verify`, URL `jdbc:postgresql://127.0.0.1:5541/issue1_validation_soutenance` :
  **155 tests, dont 15 PostgreSQL, zéro échec/erreur/ignoré**, build réussi.
- Frontend : **`npm ci`, `npm run build` et 63 tests `npm test` réussis** avec le
  verrou de correctifs Angular 21.2.26, sans modifier les versions majeures.
- `node scripts/validation-covoiturage.cjs` : réussi sur
  `transport_demo_soutenance_20261112192048`, backend 18081 et build Angular 4201.
  JWT global/local/chauffeur et trois catégories, privé/invitations/consentement,
  cinq demandes sans retenue de place, quatre paiements de 25 € (100 €), deux
  paiements concurrents pour la dernière place, annulation/remboursement et réutilisation,
  conflits chauffeur/véhicule et périmètre d'agence vérifiés via REST.
- Navigateur Edge : réservation et paiement depuis Angular, confirmation et trois
  places restantes contrôlées par REST, profil, historique, administration/statistiques,
  annulation chauffeur et remboursement conservé. **Aucune erreur JavaScript**.
- **Neuf contrôles responsive réussis**, sans débordement horizontal : accueil,
  réservations authentifiées et administration, chacun à **375, 768 et 1440 px**.
  Capture d'accueil actualisée dans `docs/images/accueil-transport-demonstration.png`.
  Rapport complet et captures supplémentaires dans `target/browser-validation`, hors Git.
- Compilation IntelliJ réussie, sans compilation Maven concurrente. Aucun fichier
  `.log`, `.env`, `target/*` ou `frontend/dist/*` suivi par Git ; recherche de signatures
  de clés privées et jetons courants sans résultat dans les sources/documentation.
- Redémarrage dev sur la même base isolée non vide avec un autre secret de démonstration :
  initialisation ignorée, empreinte des identifiants/hachages de mots de passe inchangée.
  **`/v3/api-docs` HTTP 200 et `/swagger-ui.html` HTTP 200** après redirection vérifiés.

La première tentative de démonstration a révélé un mot de passe d'environnement
de plus de 72 octets, refusé par BCrypt. L'initialiseur refuse désormais ce cas
avant toute insertion ; un test le vérifie. Générer par exemple 32 octets aléatoires
encodés en Base64 pour DEMO_PASSWORD, séparément des 64 octets de JWT_SECRET.

### Sécurité et dépendances

L'audit initial npm signalait 51 alertes, dont deux critiques et 21 hautes ; sept
concernaient les dépendances de production. Les correctifs restent dans Angular 21
(21.2.26) et les versions majeures déjà déclarées pour les outils. Aucune nouvelle
bibliothèque fonctionnelle. Le verrou a été résolu puis vérifié par `npm ci` en CI,
sans `--force` ni `--legacy-peer-deps`. `npm audit --omit=dev --json` retourne zéro
vulnérabilité de production. L'audit complet laisse 16 alertes modérées sur l'outillage :
ne pas les confondre avec zéro risque global, ni exposer un serveur de développement
sur Internet. Les migrations SQL sont additives ; aucune donnée historique recalculée.

### Trame de soutenance

1. Présenter l'architecture Controller → Service → Repository et les DTO/mappers,
   puis Angular (services API, guard, interceptor) et PostgreSQL. Voir le README.
2. Expliquer chauffeur obligatoire, quatre clients, prix 25 €, catégories et privé
   avec consentement de publication. Voir `analyse-metier.md`.
3. Montrer le diagramme et les FK/index/contraintes dans `modele-donnees.md`.
4. Démarrer Docker, une base dev isolée, Spring Boot puis Angular avec les commandes
   PowerShell du README ; ouvrir Swagger et effectuer le parcours de réservation.
5. Montrer paiement simulé, historique, remboursement, statistiques et restrictions.
6. Expliquer la transaction de paiement et le verrou de la dernière place, puis
   les tests PostgreSQL concurrents et la CI. Aucun service bancaire réel.

Les branches métier ciblent `develop`. Une PR distincte de `develop` vers `master`
est requise pour la livraison, avec CI sur cette cible et sans fusion automatique.

## Historique de la PR #17

Branche : `feature/issue-7-integration-validation`, depuis develop après la PR #16.
Pull request : [#17](https://github.com/Zoom225/GestionTransport-Multiple/pull/17), vers develop.

## Corrections d'intégration

- Démonstration existante simplifiée : profil dev, activation explicite, base isolée
  vide et mot de passe fourni par l'environnement ; aucune migration de comptes.
- Permission chauffeur, capacité fixe, règles de paiement et historique conservés.
  Le chauffeur peut annuler son trajet depuis son profil et rembourser les clients.
- Véhicule indisponible : refus côté backend, case administrative et exclusion
  des véhicules proposés dans le formulaire de création.
  Les ressources historiques sont indisponibles par défaut ; aucune ancienne
  ressource sportive n'est automatiquement mise en service comme voiture.
- `requesterId` n'est plus obligatoire : le demandeur effectif vient du JWT.
- Les trajets historiques non qualifiés ou portant un ancien tarif ne sont pas
  revendus au nouveau prix ; aucun ancien montant n'est remplacé.
- Dates et villes dans les réservations, textes FR/EN du nouvel accueil et des
  composants partagés, focus clavier visible et réduction des animations.
- Création d'un voyageur avec mot de passe initial personnel ; aucun mot de passe
  commun attribué par défaut, aucun mot de passe existant réécrit.
- Restrictions : durée configurable de sept jours, contrôle des dates d'expiration
  et protection contre les doublons sous verrou du voyageur.
- SQL explicite : index, unicité des demandes actives, villes distinctes,
  horaires et maximum de quatre clients ; refus de migration si doublons existants.
- CI ajoutée pour Java 21/PostgreSQL 15 et Node 24/Angular.
- README complet : fonctionnement, couches, Docker, sauvegarde, Maven, Angular,
  variables, démonstration isolée, tests, Swagger et traçabilité des PR.

Les durées réelles de trajet restent les valeurs configurées de l'agence.
Aucune géolocalisation, route routière, entité Conducteur, nouvelle couche de
réservation ou intégration bancaire n'a été ajoutée.

## Vérifications locales exécutées

| Contrôle | Résultat |
| --- | --- |
| Maven `verify` avec PostgreSQL opt-in | 142 tests, zéro échec, zéro test ignoré ; JAR généré |
| PostgreSQL 15 isolé | 11 tests réels incluant MockMvc, SQL, paiement et concurrence |
| IntelliJ | Compilation réussie ; avertissements MockBean existants |
| Angular / Vitest | 62 tests réussis |
| Angular production | Build réussi |
| Responsive de l'accueil | 375, 768, 1440 px, aucune largeur excédentaire |
| Swagger/OpenAPI/villes/recherche | HTTP 200 sur l'instance HTTP isolée |
| Parcours complet JWT et navigateur | Réussi sur transport_demo_validation_20261112172429 |
| CI GitHub | [Exécution réussie](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/43257061819) ; état du dernier commit sur la PR |

Bases utilisées pour les tests : `issue1_validation_pg_20261111`,
`issue1_validation_http_20261111` et bases `transport_demo_validation_<horodatage>`.
Les bases de tests ne sont ni vidées ni supprimées à la fin.

Le parcours réel a confirmé les connexions JWT global/local/chauffeur et les trois
catégories ; le chauffeur automatique ; les refus de création incohérente ; les
invitations privées ; le refus de publication sans consentement ; cinq demandes
sans retenue de place ; quatre paiements de 25 € ; la concurrence sur la dernière
place ; le remboursement et la réutilisation d'une place ; les conflits chauffeur,
le véhicule indisponible et le périmètre administrateur local.

Dans le navigateur : accueil et cartes avec de vrais trajets de démonstration,
changement FR/EN, connexion voyageur, profil, réservations, connexion administrateur,
statistiques et annulation d'un trajet par son chauffeur avec remboursement.
Réservation et paiement ont aussi été exécutés depuis les boutons Angular :
HTTP 201, paiement HTTP 200 de 25 €, confirmation et trois places restantes.
Les recettes globales du jeu de test sont contrôlées à 150 € avant le dernier
remboursement, dont 100 € pour les quatre clients du trajet complet.
Neuf mesures responsive réussies : accueil, réservations et administration aux
largeurs 375, 768 et 1440 px. Aucune erreur JavaScript.
Navigation clavier testée : tabulation jusqu'au lien administrateur, ouverture par
Entrée et contour de focus visible de 3 px.

Un premier essai de bout en bout a refusé une réservation sans requesterId : le
DTO demandait un champ ignoré par le contrôleur. Correction et test PostgreSQL/REST
ajoutés. Les échecs d'isolation de langue des tests Angular sont aussi corrigés.
Ces essais échoués ne sont pas présentés comme des validations réussies.
Un empaquetage a aussi été bloqué par le JAR de validation encore ouvert sous
Windows ; après arrêt de cette seule JVM, `verify` a réussi avec les 142 tests.

## Préservation de la base métier

Aucune commande de modification métier n'a ciblé `transportService` pendant ces
validations. Comparaison des compteurs avec la sauvegarde avant renommage :

| Table historique | Avant | Après |
| --- | --- | --- |
| administrateurs | 3 | 3 |
| jours_fermeture | 3 | 3 |
| matches | 4 | 4 |
| membres | 6 | 6 |
| paiements | 8 | 8 |
| penalites | 1 | 1 |
| reservations | 8 | 8 |
| sites | 2 | 2 |
| terrains | 5 | 5 |

Le conteneur et le volume historiques sont conservés. Le diff local de pom.xml
avec artifactId industri reste exclu des commits.

## Reproduire le parcours navigateur

`scripts/validation-covoiturage.cjs` est opt-in. Il exige une base de démonstration
isolée **neuve**, une instance backend dev déjà démarrée avec les données de
démonstration et son mot de passe d'environnement. Il crée des fixtures et contrôle
les parcours ; ne pas l'utiliser sur la base métier.

Outillage facultatif, installé seulement dans target, sans modifier package.json :

```powershell
npm install --prefix target/browser-validation --no-save --package-lock=false playwright@1.56.1
$env:VALIDATION_DATABASE = '<nom-de-la-base-isolee>'
$env:VALIDATION_API_URL = 'http://127.0.0.1:8080'
$env:VALIDATION_FRONTEND_URL = 'http://127.0.0.1:4200'
# DEMO_PASSWORD doit correspondre au mot de passe de cette instance de démonstration.
node scripts/validation-covoiturage.cjs
```

Le frontend doit servir le build réel et transmettre `/api` à cette instance de
validation. Le chemin Edge est configurable par EDGE_PATH.
Les ports 8080/4200 correspondent aux commandes backend dev et npm start du README ;
les validations exécutées ici utilisaient les ports isolés 18081/4201.
Le rapport et les captures sont écrits dans target/browser-validation, hors Git.
Le script ne journalise aucun JWT ni mot de passe.

## Décisions restant métier

Les seuils d'annulation tardive, d'absence et d'annulation abusive ne sont pas
définis par l'énoncé retenu. Ils ne déclenchent pas une sanction inventée.
La durée et l'application des restrictions validées sont disponibles ; les règles
de déclenchement complémentaires demandent une validation métier explicite.
