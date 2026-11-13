# Modèle de covoiturage — issues #2 et #3

Le chantier `refactor/issue-3-schema-transport` réutilise les neuf entités métier
existantes. Les mappings Transport et la migration sont vérifiés sur copie ;
leur application à la base métier reste soumise à validation distincte.
Aucun nouveau service d'utilisateurs ni table Conducteur, Zone, Route ou Billet.

## Classes et cardinalités

```mermaid
classDiagram
    class Voyageur {
        Long id
        String matricule
        TypeVoyageur categorie
        boolean peutProposerTrajet
    }
    class Vehicule {
        Long id
        String nom
        boolean disponible
    }
    class Trajet {
        Long id
        VilleBelge depart
        VilleBelge destination
        LocalDateTime dateDebut
        LocalDateTime dateFin
        int maximumClientsQuatre
        Double prixParPassager
        TypeTrajet type
        boolean publicationAutomatiqueAutorisee
    }
    class Reservation {
        Long id
        LocalDateTime createdAt
        StatutReservation statut
    }
    class Paiement {
        Decimal montant
        Instant datePaiement
        StatutPaiement statut
    }
    class Restriction {
        String motif
        LocalDate dateDebut
        LocalDate dateFinExclusive
        String sourceEvenementUnique
    }
    Agence "0..1" <-- "0..*" Voyageur : rattachement
    Agence "1" <-- "0..*" Vehicule : rattachement
    Agence "0..1" <-- "0..*" Administrateur : perimetre
    Voyageur "1" <-- "0..*" Trajet : conducteur
    Vehicule "1" <-- "0..*" Trajet : utilise
    Voyageur "1" <-- "0..*" Reservation : passager
    Trajet "1" <-- "0..*" Reservation : concerne
    Reservation "1" -- "0..1" Paiement : simulation
    Voyageur "1" <-- "0..*" Restriction : concerne
    Administrateur "0..1" <-- "0..*" Reservation : declareAbsence
    Agence "0..1" <-- "0..*" IndisponibiliteService : perimetre
```

Le diagramme exprime le modèle logique, pas des noms de champs à dupliquer.
Les références de faits sont des clés stables `absence:id`, `annulation-client:id`
ou `annulation-chauffeur:id` ; elles ne constituent pas deux nouvelles associations JPA.
La réservation conserve `absence_declaree_le`, `motif_absence` et sa FK nullable
`absence_admin_id`. Le trajet conserve la date effective de son annulation.
Par exemple, dateDebut/dateFin existants portent le départ et l'arrivée estimée.
L'association organisateur actuelle devient le conducteur autorisé : un seul
lien JPA, pas deux liens contenant le même utilisateur.

Le tarif total de référence est 100 €, le prix client 25 €, le maximum quatre
clients. Ce maximum est une règle centrale, pas une colonne supplémentaire par
véhicule ou trajet ; le champ du diagramme est conceptuel. Le chauffeur est exclu.
Le paiement conserve son montant historique, sans recalcul rétroactif.

## Modèle relationnel et correspondance physique

Le renommage SQL conserve les objets, identifiants, lignes, contraintes et index.
Un garde avant Hibernate refuse un schéma historique ou ambigu : `update` ne
doit jamais créer de tables parallèles à la place d'une migration explicite.

| Table cible après migration | Origine et entité réutilisée |
| --- | --- |
| agences | sites → Agence : nom, adresse, horaires et intervalles |
| voyageurs | membres → Voyageur : matricule unique, identité, mot de passe, catégorie, agence éventuelle et droit de proposer |
| administrateurs | Administrateur : comptes distincts déjà présents, type global/agence et agence éventuelle conservés |
| vehicules | terrains → Vehicule : nom et disponibilité ; aucune plaque, marque ou modèle inventés |
| trajets | matches → Trajet : vehicule_id et chauffeur_id obligatoires, villes et consentement ; maximum clients centralisé à quatre |
| reservations | Même table : trajet_id, voyageur_id et date de création ; échéance calculée, une place par réservation |
| paiements | Paiement : réservation, montant, date et statut ; historique remboursé conservé |
| restrictions | penalites → Restriction : voyageur_id, date_debut, date_fin, motif, source_evenement |
| indisponibilites_service | jours_fermeture → IndisponibiliteService : date, raison, périmètre global/agence |

Les champs Java/JSON cibles utilisent VIP, LOCAL, LIBRE. Le convertisseur
TypeVoyageur existant conserve : VIP ↔ GLOBAL, LOCAL ↔ SITE,
LIBRE ↔ LIBRE. Le type AGENCE des administrateurs reste inchangé dans le code
et continue à correspondre à SITE en SQL. Une substitution globale serait erronée.

VilleBelge est une liste centrale de codes et libellés exposée au frontend,
comprenant Bruxelles, Mons, Charleroi, Namur, Liège, Anvers, Gand, Louvain,
Nivelles et Wavre. Les exemples de liaisons sont des enregistrements de
démonstration, pas une liste de couples interdits/autorisés codée en dur.

## Contraintes et index à vérifier sur PostgreSQL

### Diagramme entité-association du schéma cible

```mermaid
erDiagram
    agences ||--o{ vehicules : agence_id
    agences o|--o{ voyageurs : agence_id
    agences o|--o{ administrateurs : agence_id
    agences o|--o{ indisponibilites_service : agence_id
    voyageurs ||--o{ trajets : chauffeur_id
    vehicules ||--o{ trajets : vehicule_id
    trajets ||--o{ reservations : trajet_id
    voyageurs ||--o{ reservations : voyageur_id
    reservations ||--o| paiements : reservation_id_unique
    voyageurs ||--o{ restrictions : voyageur_id
    administrateurs o|--o{ reservations : absence_admin_id
    agences {
      bigint id PK
    }
    vehicules {
      bigint id PK
      bigint agence_id FK
    }
    voyageurs {
      bigint id PK
      varchar matricule UK
      bigint agence_id FK
      varchar type_voyageur
    }
    administrateurs {
      bigint id PK
      varchar matricule UK
      varchar email UK
      bigint agence_id FK
    }
    trajets {
      bigint id PK
      bigint chauffeur_id FK
      bigint vehicule_id FK
      timestamp date_debut
      timestamp date_fin
    }
    reservations {
      bigint id PK
      bigint trajet_id FK
      bigint voyageur_id FK
      bigint absence_admin_id FK
    }
    paiements {
      bigint id PK
      bigint reservation_id FK,UK
      float montant
    }
    restrictions {
      bigint id PK
      bigint voyageur_id FK
      varchar source_evenement UK
    }
    indisponibilites_service {
      bigint id PK
      bigint agence_id FK
    }
```

Les FK nullable sont indiquées par `o|`. L'unicité partielle des demandes actives
porte sur `(trajet_id,voyageur_id)` pour EN_ATTENTE/CONFIRMEE ; plusieurs anciennes
réservations annulées restent possibles. Les noms opaques des contraintes et
des séquences historiques sont conservés : le renommage de leurs tables/colonnes
préserve les dépendances PostgreSQL. La clé de chaque table reste `id`.
Le montant SQL flottant historique n'est pas converti : les nouveaux calculs
restent centralisés en BigDecimal dans les services, sans réécrire les encaissements.

- FK conducteur et véhicule obligatoires pour tout nouveau trajet ; conducteur
  autorisé vérifié par le service, distinct des réservations passagers.
- L'entité actuelle ne possède pas d'immatriculation : aucune contrainte fictive
  n'est ajoutée. Sa qualification et une éventuelle unicité restent un chantier
  métier distinct ; pas de plaque inventée pour une ressource sportive.
- Quatre confirmations clients maximum sous transaction/verrou. Le chauffeur
  ne crée pas de réservation et ne peut pas acheter une place de son propre trajet.
- Départ différent de destination ; arrivée strictement après départ ; prix
  positif ou nul selon politique validée, jamais un montant négatif.
- Une seule demande active par paire trajet/voyageur, avec index unique partiel
  sur les statuts actifs. Les annulations/expirations restent historisées et une
  nouvelle demande peut être créée après résolution de l'ancienne.
- Paiement unique par réservation dans cette simulation, pour réutiliser le
  OneToOne actuel ; une tentative répétée ne crée pas un deuxième débit.
- Index de recherche sur départ/destination/date/type/statut et de contrôle
  sur conducteur/créneau et véhicule/créneau. Les verrous métier restent requis.
- Un fait/cause ne génère qu'une restriction ; index d'idempotence à définir
  selon le lien réservation/trajet, sans empêcher deux faits distincts légitimes.

Les statuts actuels sont conservés autant que possible. EXPIREE est nécessaire
pour distinguer expiration et annulation volontaire. Les états terminés prévus
par le PDF ne doivent être ajoutés qu'avec une transition métier et un test réels.
Les CHECK PostgreSQL des enums doivent être inspectés et migrés explicitement
avant toute écriture d'une nouvelle valeur.

## Protection et qualification des anciennes données

1. Inventorier les tables, contraintes et comptes sans écriture. Sauvegarder
   avec pg_dump avant toute migration validée ; vérifier la restauration dans
   une base distincte. Ne jamais supprimer le volume Docker existant.
2. Préparer le schéma sur une base isolée avec copie contrôlée. Conserver
   ddl-auto existant jusqu'à validation d'une autre stratégie ; ne pas ajouter
   Flyway/Liquibase simplement pour ce chantier.
3. Ajouter les données nouvelles de façon compatible avec les anciennes lignes.
   Une ancienne ressource sportive non qualifiée n'est pas un véhicule réservable.
   Les nouveaux DTO imposent ces champs, les anciennes lignes restent consultables
   en historique et exclues de l'exploitation jusqu'à qualification explicite.
4. Ne pas attribuer automatiquement le droit de conduire aux anciens voyageurs.
   L'ancien organisateur n'est pas une preuve de qualification conducteur.
   Un ancien trajet sans villes ou véhicule qualifié n'est pas proposé à la vente.
5. Conserver les montants des trajets, réservations, paiements et remboursements.
   Ne pas convertir les anciens montants 60/15 en 100/25 par UPDATE global.
6. Les calculs nouveaux utilisent BigDecimal. Préserver les anciennes colonnes
   et paiements tant qu'une migration NUMERIC n'est pas validée ; convertir les
   valeurs à la frontière, sans UPDATE global ni arrondi d'anciens paiements.
7. Ne pas reprendre nbPassagersActuels comme vérité de capacité : le socle inclut
   des demandes impayées et l'organisateur. Les places confirmées sont calculées
   selon une règle unique sous verrou, sans compteur supplémentaire. Si le
   compteur actuel est conservé, seules confirmation et annulation confirmée
   le modifient dans la même transaction ; les demandes impayées ne le modifient pas.
   Les anciennes lignes restent hors exploitation jusqu'à qualification.
8. Comparer identifiants, sommes, relations et historiques avant/après ; démarrer
   avec ddl-auto=validate sur la copie ; obtenir validation avant base métier.

## API et responsabilités

Adapter CreateTrajetRequest, TrajetRequest, TrajetDto, VehiculeRequest/Response
et leurs mappers existants. Un seul flux de création dans TrajetService gère
conducteur connecté ou conducteur sélectionné par administrateur habilité.
L'identifiant conducteur soumis par un voyageur ne doit jamais remplacer celui
de son authentification. Même règle pour réserver/payer au nom d'un tiers.

Réutiliser ReservationService pour les demandes et invitations, PaiementService
pour la simulation et confirmation. Les invitations peuvent utiliser les
réservations privées créées par le conducteur ; aucun système d'invitation ou
utilisateur parallèle n'est nécessaire. L'historique d'un voyageur reste lisible
sans donner accès aux données privées des autres passagers.

Le scheduler existant devient le point unique d'orchestration des expirations
et publications consenties ; pas de nouveaux traitements concurrents dupliqués.
Les règles et requêtes restent dans leurs couches existantes.
