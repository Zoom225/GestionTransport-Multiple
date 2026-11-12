# Issue #7 — Intégration et validation

Branche : `feature/issue-7-integration-validation`, depuis develop après la PR #16.

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
| Parcours complet JWT et navigateur | Réussi sur transport_demo_validation_20261112165214 |
| CI GitHub | Workflow versionné ; état d'exécution consultable sur la PR |

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
$env:VALIDATION_API_URL = 'http://127.0.0.1:18081'
$env:VALIDATION_FRONTEND_URL = 'http://127.0.0.1:4201'
# DEMO_PASSWORD doit correspondre au mot de passe de cette instance de démonstration.
node scripts/validation-covoiturage.cjs
```

Le frontend doit servir le build réel et transmettre `/api` à cette instance de
validation. Le chemin Edge est configurable par EDGE_PATH.
Le rapport et les captures sont écrits dans target/browser-validation, hors Git.
Le script ne journalise aucun JWT ni mot de passe.

## Décisions restant métier

Les seuils d'annulation tardive, d'absence et d'annulation abusive ne sont pas
définis par l'énoncé retenu. Ils ne déclenchent pas une sanction inventée.
La durée et l'application des restrictions validées sont disponibles ; les règles
de déclenchement complémentaires demandent une validation métier explicite.
