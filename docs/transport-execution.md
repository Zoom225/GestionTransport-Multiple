# Exécution Transport

Java 21 et versions du socle conservées. Package Java :
`com.transport.gestiontransport`, entrée `GestionTransportApplication`.

Cette étape adapte les noms du code et des API ; les concepts transport complets
(conducteurs, itinéraires, capacités, tarifs) restent à modéliser et valider.

## Backend et données

Exécuter `docker compose up -d`, puis `docker compose ps`.
PostgreSQL 15 est exposé sur le port 5440. Définir DB_URL, DB_USER, DB_PASSWORD
et JWT_SECRET avant `mvn spring-boot:run`. Utiliser une clé JWT aléatoire d'au
moins 32 octets. Conserver DEMO_DATA_ENABLED=false sur une base métier.

ddl-auto=update reste inchangé et peut modifier le schéma : utiliser une base
isolée ou sauvegardée. Les tables, colonnes et valeurs SQL historiques sont
explicitement conservées dans les mappings de compatibilité. La valeur Java/JSON
AGENCE correspond encore à la valeur SQL historique SITE, via des convertisseurs.
Ne pas renommer la base, les comptes ou les volumes par substitution.

Le pom.xml et son artifactId local industri sont protégés ; utiliser Maven pour
le démarrage sans supposer le nom du JAR. Les données sportives préexistantes
ne sont pas qualifiées comme données de transport par ce seul renommage.

## Contrats actuels

Swagger : `/swagger-ui.html`. OpenAPI : `/v3/api-docs`.

- `/api/agences`, `/api/vehicules`, `/api/trajets`, `/api/voyageurs`.
- `/api/reservations`, `/api/paiements`, `/api/indisponibilites-service`.
- Connexion administrateur : `/api/auth/login`.
- Connexion voyageur : `/api/voyageurs/login`.

Exemples de propriétés JSON : vehiculeId, agenceId, trajetId, voyageurId,
typeTrajet, typeVoyageur, nbPassagersActuels, prixParPassager.
Le rôle Spring de voyageur est VOYAGEUR, le type de gestionnaire est AGENCE.

Déployer ensemble backend/frontend. Les anciennes routes, anciens contrats et
anciennes sessions du navigateur ne sont pas repris : se reconnecter après
mise à jour. Une nouvelle clé JWT invalide aussi les anciens jetons.

## Frontend et vérification

Depuis frontend : `npm ci`, `npm start`. Le proxy REST vise le backend sur 8080.
L'espace voyageur est `/voyageur` ; l'administration expose `/admin/voyageurs`,
`/admin/trajets`, `/admin/agences`, `/admin/vehicules`, `/admin/indisponibilites`.

Backend : `mvn -B test`. Les tests standards utilisent H2 ; les tests PostgreSQL
sont activés avec POSTGRES_BASELINE_URL ciblant exclusivement une base isolée
nommée issue1_validation_<suffixe>, et DB_USER/DB_PASSWORD définis.

Frontend : `npm run build`, puis `npm test`.
Voir aussi [protocole de baseline](baseline-execution.md).
