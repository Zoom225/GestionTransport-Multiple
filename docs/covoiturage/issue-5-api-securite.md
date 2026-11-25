# Issue #5 — Contrats REST et accès

Branche : `feature/issue-5-transport-api`, après la PR #14.

L'API existante est adaptée, sans deuxième contrôleur ni mapper parallèle.

| Méthode / chemin | Comportement |
| --- | --- |
| GET `/api/trajets/villes` | Source centrale des codes et libellés des villes belges |
| GET `/api/trajets/public?depart=MONS&arrivee=BRUXELLES&date=2026-12-01` | Filtres facultatifs, trajets futurs qualifiés et disponibles ; zone LOCAL contrôlée si connecté |
| POST `/api/trajets` | Villes distinctes, véhicule, départ, PUBLIC/PRIVE ; chauffeur connecté ou sélection obligatoire pour un administrateur |
| GET `/api/trajets/{id}` | Contrôle de zone, invitation privée ou périmètre administrateur |
| PUT `/api/trajets/{id}` | Identité authentifiée contrôlée avant l'identité du chauffeur du trajet |
| GET `/api/reservations/trajet/{id}` | Chauffeur : ses clients ; client : ses propres réservations ; administrateur : agence autorisée |

Les DTO exposent `villeDepart`, `villeArrivee`, `chauffeurId`, `chauffeurNom`,
`placesRestantes`, `placesMaximum` et `agenceId`. Les champs organisateur sont
conservés pour la compatibilité de l'interface existante, avec la même valeur.
Ils ne désignent pas un second utilisateur.

`VoyageurRequest` et `VoyageurResponse` existants exposent `peutProposerTrajet`.
Sa modification passe par les endpoints administrateurs déjà protégés.
Un champ omis lors d'une mise à jour ne retire pas un droit existant.

L'isolation administrateur des listes repose sur l'identifiant d'agence,
et non sur son nom, qui peut être identique entre plusieurs agences.

Les refus d'accès renvoient 403. Les villes/dates/JSON invalides renvoient 400 ;
les conflits de persistance ou de verrouillage renvoient 409 avec un message
compréhensible. Aucune exception SQL n'est exposée dans la réponse.

Les contrôles PostgreSQL/MockMvc couvrent la création authentifiée, quatre places
clients disponibles à la création, le détail privé refusé à un tiers, l'absence
d'authentification, la recherche Mons–Bruxelles et la liste des villes.
Un test supplémentaire vérifie l'isolation entre agences de même nom.

Validation exécutée : Maven 132 tests, aucun échec ni test ignoré, sept tests
PostgreSQL/REST inclus ; compilation IntelliJ réussie sans problème signalé.
La base métier `transportService` et le diff local de `pom.xml` sont préservés.
