# Diagrammes de GestionTransport-Multiple

Référence fonctionnelle `d74caeb` intégrée par la PR #37. Les associations représentées sont issues des entités sous `src/main/java/com/transport/gestiontransport/entity/`. Les associations facultatives de données historiques sont indiquées.

## Cas d’utilisation

```mermaid
flowchart LR
    Voyageur(["Voyageur"]) --> Rechercher["Rechercher un trajet public"]
    Voyageur --> Reserver["Demander une place"]
    Voyageur --> Payer["Payer sa réservation"]
    Voyageur --> Profil["Voir son profil et ses sanctions"]
    Organisateur(["Organisateur"]) --> Proposer["Proposer un trajet avec un chauffeur"]
    Organisateur --> Inviter["Inviter des voyageurs à un trajet privé"]
    Organisateur --> Regler["Régler une pénalité"]
    Admin(["Administrateur"]) --> Gerer["Gérer les ressources et contrôler les trajets"]
    Gerer --> Agence["Périmètre GLOBAL ou AGENCE"]
```

## Activité de réservation et paiement

```mermaid
flowchart TD
    A[Demande du voyageur] --> B{Trajet et voyageur éligibles}
    B -- Non --> C[Refus métier]
    B -- Oui --> D[Réservation en attente et paiement en attente]
    D --> E{Paiement demandé avant échéance}
    E -- Non --> F[Expiration possible par traitement planifié]
    E -- Oui --> G[Verrouiller le trajet et vérifier le titulaire]
    G --> H{Place et paiement encore disponibles}
    H -- Non --> C
    H -- Oui --> I[Valider le paiement et confirmer la réservation]
    I --> J{Quatre places confirmées}
    J -- Oui --> K[Trajet complet]
    J -- Non --> L[Trajet encore disponible]
```

## Séquence de paiement

```mermaid
sequenceDiagram
    actor U as Voyageur
    participant A as Angular
    participant C as PaiementController
    participant P as PaiementServiceImpl
    participant R as ReservationServiceImpl
    participant B as PostgreSQL
    U->>A: Confirmer le paiement
    A->>C: POST /api/paiements/reservation/{id}/pay
    C->>P: pay(id, voyageurId issu de la session)
    P->>R: preparePaiement(id, voyageurId)
    R->>B: Verrouiller le trajet
    R-->>P: Réservation et paiement en attente validés
    P->>B: Enregistrer le paiement
    P->>R: confirm(id)
    R->>B: Attribuer la place et mettre à jour le trajet
    P-->>C: Paiement confirmé
    C-->>A: HTTP 200 et réponse PaiementResponse
    A-->>U: Afficher la confirmation
```

## Classes principales

```mermaid
classDiagram
    Agence "1" <-- "0..*" Vehicule : rattachement
    Agence "0..1" <-- "0..*" Voyageur : rattachement
    Agence "0..1" <-- "0..*" Administrateur : périmètre
    Agence "0..1" <-- "0..*" IndisponibiliteService : périmètre
    Vehicule "1" <-- "0..*" Trajet : affectation
    Voyageur "1" <-- "0..*" Trajet : chauffeur
    Voyageur "0..1" <-- "0..*" Trajet : organisateur
    Trajet "1" <-- "0..*" Reservation : trajet
    Voyageur "1" <-- "0..*" Reservation : passager
    Reservation "1" <-- "0..1" Paiement : règlement
    Voyageur "1" <-- "0..*" Restriction : voyageur
    Trajet "0..1" <-- "0..1" Restriction : origine de la pénalité
    Administrateur "0..1" <-- "0..*" Reservation : déclarant de l’absence
```

## Modèle conceptuel

```mermaid
erDiagram
    AGENCE ||--o{ VEHICULE : rattache
    AGENCE o|--o{ VOYAGEUR : rattache
    VEHICULE ||--o{ TRAJET : assure
    VOYAGEUR ||--o{ TRAJET : conduit
    VOYAGEUR o|--o{ TRAJET : organise
    TRAJET ||--o{ RESERVATION : reçoit
    VOYAGEUR ||--o{ RESERVATION : demande
    RESERVATION ||--o| PAIEMENT : déclenche
    VOYAGEUR ||--o{ RESTRICTION : reçoit
    TRAJET o|--o| RESTRICTION : origine
```

Un voyageur peut exercer les rôles de chauffeur, organisateur ou passager selon l’association. Les règles de réservation empêchent le chauffeur d’occuper l’une des quatre places passagers du même trajet. Les contraintes de disponibilité comparent les horaires des trajets actifs pour le même véhicule ou chauffeur.

## Modèle logique relationnel

Les clés primaires sont les identifiants des entités. Les clés étrangères entre parenthèses peuvent être nulles.

| Relation | Identifiants référencés |
| --- | --- |
| AGENCE | Identifiant de l’agence |
| VEHICULE | agence |
| VOYAGEUR | (agence) |
| ADMINISTRATEUR | (agence) |
| INDISPONIBILITE_SERVICE | (agence) |
| TRAJET | véhicule, chauffeur, (organisateur) |
| RESERVATION | trajet, voyageur, (administrateur déclarant l’absence) |
| PAIEMENT | réservation, unique |
| RESTRICTION | voyageur, (trajet d’origine), unique lorsqu’il est renseigné |

## Modèle physique PostgreSQL

| Objet physique | Contraintes et preuve |
| --- | --- |
| `trajets` | `vehicule_id` et `chauffeur_id` obligatoires. `voyageur_organisateur_id` référence `voyageurs.id`. Les invariants Transport bornent le nombre de passagers et contrôlent les statuts. |
| `reservations` | `trajet_id` et `voyageur_id` obligatoires. L’index partiel `ux_reservations_actives` cible les demandes actives du même voyageur sur un trajet. |
| `paiements` | `reservation_id` obligatoire et unique via le mapping un-à-un. |
| `restrictions` | Identifiant d’événement unique. `trajet_origine_id` est facultatif et unique lorsqu’il existe. La migration de suspension ajoute le contrôle de 168 heures. |
| Prix, montants et soldes | Le candidat de référence utilise les flottants sur plusieurs colonnes. La [PR #47](https://github.com/Zoom225/GestionTransport-Multiple/pull/47) propose `numeric(19,2)` et une migration explicite. `restrictions.montant_penalite` est en `numeric(12,2)`. |

Sources de contraintes. [Invariants](../../scripts/migrations/issue-3-invariants-transport.sql), [organisateur](../../scripts/migrations/issue-8-organisateur-penalite.sql), [suspension](../../scripts/migrations/issue-9-suspension-168h.sql).
