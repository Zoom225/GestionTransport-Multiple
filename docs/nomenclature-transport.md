# Nomenclature Transport

Branche : `refactor/transport-nomenclature`.

Ce chantier répond à la demande de renommage transversal. Il dépend du socle
de la PR #8, sans modifier la branche d'audit ni fusionner cette PR.

| Ancien concept | Nom du code Transport |
| --- | --- |
| Site | Agence |
| Terrain | Vehicule |
| Match | Trajet |
| Membre / Member | Voyageur |
| Joueur / Player | Passager |
| JourFermeture | IndisponibiliteService |
| Penalite / Penalty | Restriction |

Les noms des classes, propriétés, services, DTO, routes REST, types Angular,
traductions et tests doivent être cohérents. Réservation et paiement restent
des concepts valides dans le transport.

## Protection des données existantes

Le renommage du code ne vaut pas conversion des anciennes données sportives.
Les tables et colonnes historiques restent explicitement mappées tant qu'une
migration avec sauvegarde, qualification des données et validation du modèle
Transport n'est pas approuvée. Les tests PostgreSQL doivent être dirigés vers
une base de validation distincte.

Les noms physiques hérités qui restent nécessaires à la compatibilité sont
des exceptions documentées, comme les références conservées dans l'audit initial.
Ne pas renommer une base, un volume ou un compte PostgreSQL par substitution.

## Limite métier

Ce renommage ne valide pas les règles RM01–RM30. Les hypothèses héritées sur
la capacité, les tarifs et les durées doivent encore être remplacées par les
concepts et règles de transport validés. Une ancienne ressource sportive ne
devient pas automatiquement un véhicule exploitable par ce seul renommage.

Le diff local préexistant de `pom.xml` (`artifactId=industri`) reste préservé.

## Modifications réalisées

- Package Java `com.transport.gestiontransport` et entrée `GestionTransportApplication`.
- Classes, propriétés, associations JPA, requêtes JPQL, repositories, services,
  DTO, mappers, contrôleurs, enums, scheduler et tests renommés.
- Routes REST `/api/agences`, `/api/vehicules`, `/api/trajets`, `/api/voyageurs`,
  `/api/indisponibilites-service`, et sous-routes de réservation adaptées.
- Propriétés JSON : agenceId, vehiculeId, trajetId, voyageurId, typeVoyageur,
  typeTrajet, nbPassagersActuels, prixParPassager, dureeTrajetMinutes.
- Pages, fichiers, dossiers, types, imports, services HTTP, guards, sessions,
  traductions français/anglais et marque Angular adaptés. Espace `/voyageur`.
- Type de compte AGENCE et autorité Spring VOYAGEUR. Les convertisseurs JPA
  maintiennent la valeur SQL SITE pour les comptes existants.
- GroupId Maven com.transport, nom GestionTransport-Multiple et description
  Transport, expressément autorisés ; artifactId local industri conservé hors commit.

## Exceptions intentionnelles

Les tables sites, terrains, matches, membres, penalites, jours_fermeture et leurs
colonnes historiques restent dans les annotations JPA. Les autres tables restent
également inchangées. Les convertisseurs contiennent SITE comme valeur physique
de compatibilité. Ces exceptions évitent une création de colonnes ou une perte
de données par renommage implicite ; elles nécessiteront une migration validée.

Le nom de base, le compte et les identifiants du volume/conteneur Docker existants
restent conservés. L'artifactId commité et le module IDE associé restent ceux du
socle tant que le diff local de l'artifactId n'est pas intégré volontairement.
Les audits et l'ancienne documentation sont explicitement historiques ; le
[guide d'exécution Transport](transport-execution.md) décrit les contrats courants.
Les noms techniques de bibliothèques (requestMatchers, anyMatch, matches,
ArgumentMatchers, MatChipsModule, etc.) ne sont pas des concepts métier à renommer.

Déployer frontend/backend ensemble et se reconnecter : les contrats REST, noms
JSON et clés de session ont changé. Aucune compatibilité des anciens clients
n'est revendiquée. Les données historiques ne sont pas automatiquement qualifiées
comme véhicules ou trajets exploitables dans le futur modèle Transport.

## Vérifications exécutées

- Compilation IDE des symboles Java réussie ; Maven compile et teste Java 21.
- Suite Maven avec PostgreSQL isolé : 112 tests, 0 échec, 0 erreur, 0 ignoré.
  Inclut concurrence sur la dernière place, unicité, clé étrangère et compatibilité
  AGENCE ↔ SITE des comptes voyageurs et administrateurs.
- Angular : build de production réussi et 58 tests réussis dans 12 fichiers.
- Spring Boot démarré sur PostgreSQL 15.19 avec ddl-auto=validate sur la base
  HTTP préexistante de validation, sans initialisation de démonstration.
- Swagger 200 après redirection ; OpenAPI 200, 35 chemins sans les anciennes
  routes métier ; agences/véhicules/trajets publics 200 ; accès voyageurs protégé
  403 ; connexion invalide 400. Base HTTP : sites et membres toujours vides.
- Journaux locaux transport-*.log sous target/. Aucun secret JWT imprimé.

Échecs intermédiaires corrigés : référence dynamique CreateMatchRequest manquée
par l'IDE, puis noms techniques requestMatchers/ArgumentMatchers/anyMatch/allMatch
altérés par la transformation textuelle. Les tests et builds finaux précités
ont été exécutés après ces corrections. Les logs initiaux restent conservés.

La revue automatique a d'abord refusé les transformations globales. Le demandeur
a ensuite expressément autorisé les transformations contrôlées, puis les trois
métadonnées du pom.xml. La réécriture intégrale du README a été refusée : seules
son introduction et ses références ont été adaptées, sans supprimer son historique.
