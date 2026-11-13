# Issue #3 — modèle simple de covoiturage

## Harmonisation SQL Transport — novembre 2026

Branche `refactor/issue-3-schema-transport` depuis develop b818392, indépendante
du chantier frontend #22. Aucun changement de DTO, API, règles tarifaires,
réservation, paiement, catégories ou autorisations. Les huit entités dont les
mappings physiques changeaient sont adaptées ; Paiement et ses relations sont
conservés. Voir [diagrammes à jour](modele-donnees.md).

### Migration explicite

`scripts/migrations/issue-3-schema-transport.sql` renomme six tables et quinze
colonnes, dans une transaction avec timeout de verrouillage. Il refuse une
coexistence source/cible, une colonne absente ou une fonction SQL utilisateur
contenant une référence textuelle historique. Il est idempotent. Les FK, PK,
identités, séquences, index et valeurs restent les mêmes objets PostgreSQL.
Les noms historiques de séquences et de contraintes opaques restent conservés.
Les convertisseurs VIP/LOCAL/LIBRE ↔ GLOBAL/SITE/LIBRE ne changent pas.

`issue-3-invariants-transport.sql` reprend les contraintes des scripts antérieurs
pour les noms Transport : demande active unique, source de restriction unique,
statuts de réservation, contrôle des villes/dates/compteur, disponibilité par
défaut désactivée. Les scripts historiques sont conservés et ne doivent pas être
rejoués après renommage. Sur une ancienne installation, examiner les invariants
déjà présents avant l'application explicite de ce second script.

L'application conserve la stratégie `ddl-auto=update`, mais un contrôle avant
Hibernate refuse tout schéma historique. Une migration validée est nécessaire :
il ne faut pas créer des tables vides à côté des données. La vérification de
la copie restaurée utilise `ddl-auto=validate` sans génération de tables.

### Sauvegarde et essais réellement exécutés

- Sauvegarde custom `pg_dump` de transportService, restauration `pg_restore`
  sans erreur sur `transport_schema_copy_20261112234218` (PostgreSQL 15, conteneur
  de validation 5541). Aucun renommage exécuté sur transportService.
- Renommage exécuté deux fois ; neuf empreintes complètes identiques avant/après,
  même nombre de contraintes et d'index. Huit paiements historiques conservés.
- Invariants complémentaires validés sur cette copie ; empreintes des données
  toujours identiques, index unique des demandes actives et CHECK validés.
- Script PowerShell reproductible testé à partir de la sauvegarde initiale :
  nouvelle restauration `transport_schema_copy_20261113181001595`, migration,
  comparaison des neuf empreintes et seconde exécution sans effet réussies.
- `mvn -B verify` final : **160 tests réussis**, aucun échec, erreur ou ignoré,
  dont 15 tests PostgreSQL et deux tests JPA sur copie restaurée avec `validate`.
  Trois tests du garde couvrent PostgreSQL historique, PostgreSQL neuf/migré et H2.
- Lancement réel du jar sur une autre copie historique : refus avant Hibernate,
  neuf tables historiques, zéro table cible créée. Aucun secret dans les preuves.
- Un premier ajout du garde avait une collision de noms de beans, détectée par
  deux erreurs de contexte ; corrigée avant la suite finale réussie. Aucun test supprimé.
- CI étendue : copie PostgreSQL isolée, fixture historique dédiée, migration,
  empreintes, idempotence, refus d'un état ambigu, validation JPA.
- Build Angular et 63 tests du develop stable réussis sur cette branche, sans
  modification frontend. Le chantier premium séparé #22 conserve ses 72 tests.
  Compilation IntelliJ réussie après un premier délai d'attente dépassé.
- Intégration réelle du nouveau backend et des assets premium : base isolée
  transport_demo_premium_20261113181820, onze contrôles fonctionnels réussis,
  douze contrôles responsive 375/768/1440 et aucune erreur JavaScript. Connexions
  GLOBAL/AGENCE et refus hors périmètre, recherche, réservation, paiement25,
  confirmation/trois places, quatre paiements100€, concurrence, remboursement et
  historique ont été rejoués avec PostgreSQL et Spring Boot réels.

| Données de la sauvegarde | Lignes conservées |
| --- | ---: |
| Agences | 2 |
| Véhicules historiques, sans qualification automatique | 5 |
| Voyageurs | 6 |
| Administrateurs | 3 |
| Trajets historiques | 4 |
| Réservations | 8 |
| Paiements | 8 |
| Restrictions | 1 |
| Indisponibilités | 3 |

L'entité Vehicule ne contient actuellement ni immatriculation, ni marque/modèle.
Aucun champ, plaque ou index d'unicité fictif n'a été ajouté. Les prix historiques
restent de type flottant en SQL ; aucune conversion NUMERIC ou réévaluation des
montants n'est incluse dans cette migration de noms.

### Incident de rechargement IntelliJ et état de la base métier

L'instance IntelliJ de ce projet restée active sur le port 8080 a rechargé les
classes compilées avec DevTools et `ddl-auto=update`, avant l'ajout du garde.
Des tables Transport vides et des colonnes de mapping parallèles ont ainsi été
générées dans transportService. L'instance a été arrêtée après identification
de sa classe principale et de son workspace, sans arrêter d'autre application.
Le migrateur a refusé cette coexistence lors de la seconde sauvegarde restaurée.
Une sauvegarde supplémentaire de cet état est conservée localement hors Git.

Les neuf empreintes des **colonnes historiques d'origine** de transportService
ont été comparées à la première sauvegarde : données identiques, y compris les
huit paiements, comptes, mots de passe et identifiants. Aucune suppression,
fusion de tables, restauration ou migration n'a été appliquée à la base métier.
Les tables vides et colonnes ajoutées restent en place ; leur régularisation et
la migration métier demandent une revue puis une autorisation explicite distincte.
Ne pas relancer une ancienne version sans garde en mode update sur cette base.
Arrêter Spring Boot/DevTools et l'auto-compilation avant tout changement de branche
qui modifie les mappings SQL. Les dumps sont confidentiels et ignorés par Git.

### Commandes PowerShell

```powershell
# Application/IDE arrêté. Restaurer la sauvegarde initiale dans une NOUVELLE copie :
.\scripts\valider-schema-transport.ps1 -BackupFile C:\chemin\sauvegarde.dump -DatabaseUser transport
# Ou sauvegarder/restaurer la source actuelle : l'état ambigu sera refusé, jamais fusionné.
.\scripts\valider-schema-transport.ps1 -SourceContainer padel-db -SourceDatabase transportService -DatabaseUser transport

$env:DB_USER='transport'
$env:DB_PASSWORD='<mot de passe de la base de validation>'
$env:TRANSPORT_SCHEMA_URL='jdbc:postgresql://127.0.0.1:5541/transport_schema_copy_<identifiant>'
mvn -B -Dtest=TransportSchemaValidationTest test
# POSTGRES_BASELINE_URL doit désigner une autre base de test issue1_validation_... neuve :
$env:POSTGRES_BASELINE_URL='jdbc:postgresql://127.0.0.1:5541/issue1_validation_<identifiant>'
mvn -B verify
```

La fixture inverse de `src/test/resources/schema-historique-fixture.sql` est
limitée aux copies nommées de validation ; ce n'est pas une procédure de retour
arrière métier. Une base neuve utilise les nouveaux mappings et les invariants
Transport ; une base existante exige sauvegarde, restauration vérifiée et revue.

## Historique de l'étape initiale

Branche : feature/issue-3-domain-persistence, depuis develop 058ddc2 après
intégration revue et testée des PR #8, #9, #10, #12 et #11.

La relation organisateur devient explicitement chauffeur dans Trajet, avec
Voyageur et la même colonne obligatoire organisateur_id. Les associations,
requêtes JPA et références Java sont adaptées sans table Conducteur.
VilleBelge centralise les dix villes et leurs libellés, sans données de trajets
obligatoires. Les colonnes de villes restent nullables pour les anciens trajets,
qui devront être qualifiés avant d'être proposés à la réservation.

Trajet porte le maximum central de quatre clients et une validation du compteur
entre zéro et quatre. La publication automatique exige un consentement explicite,
désactivé par défaut. Voyageur possède un droit de proposition désactivé par
défaut pour les comptes existants. L'audit JPA des dates existantes est activé
pour permettre le calcul fiable de l'échéance des demandes.

Les catégories sont VIP, LOCAL et LIBRE dans Java et Angular. Le convertisseur
conserve GLOBAL et SITE dans la base ; le type AGENCE de l'administrateur reste
distinct et inchangé. L'adaptation minimale des types et formulaires Angular
accompagne ce contrat, sans nouveau composant ni système de rôles.

## Vérifications exécutées

- Maven Java 21 : compilation puis 115 tests, aucun échec, erreur ou test ignoré.
- PostgreSQL : tests opt-in sur issue1_validation_pg_20261111 uniquement.
- Trois tests de persistance ajoutés : chauffeur/villes/audit/mapping VIP,
  chauffeur absent refusé, compteur supérieur à quatre refusé.
- Angular : 58 tests réussis et build de production réussi.
- Aucun lancement Hibernate ni écriture sur transportService. Aucun ancien
  paiement, compte ou trajet métier modifié ; diff local pom.xml conservé.

Cette étape prépare le modèle. L'issue #4 doit encore supprimer la réservation
automatique du chauffeur, déplacer la prise de place au paiement et traiter
expiration/annulation sous le même verrou. Les nouveaux droits et villes seront
contrôlés par les services et exposés par les DTO existants dans l'étape suivante.
