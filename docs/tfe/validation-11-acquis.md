# Dossier de preuves des onze acquis EPHEC

Les domaines ci-dessous reprennent les intitulés fournis pour la mission. La grille du professeur et les identifiants RM01 à RM30 restent à confirmer. Java/Spring Boot et Angular démontrent des compétences techniques comparables à celles mobilisées avec C# et Blazor, sous réserve de l’accord du professeur sur cette équivalence.

Les preuves de référence portent sur `d74caeb`, intégré via la PR #37. Le rejeu de cet arbre a donné 220 tests backend réussis, dont 20 conditionnels avec PostgreSQL, et 96 réussis en Angular, sans échec ni test ignoré. Les preuves postérieures indiquent leur propre commit. [Audit](https://github.com/Zoom225/GestionTransport-Multiple/blob/develop/docs/finalisation/audit-initial.md) · [CI d’intégration](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/45464071817).

## AA01 · Analyse des besoins

Formaliser les acteurs et leurs fenêtres d’accès à la réservation.

- Élément vérifié. Section de comparaison des exigences. La règle est testée par `ReservationServiceTest.respecteLaFenetreDeChaqueCategorie`. [`docs/covoiturage/analyse-metier.md`](https://github.com/Zoom225/GestionTransport-Multiple/blob/d74caeb117a4b4dd3287a8efd1d5edb71839c6ce/docs/covoiturage/analyse-metier.md).
- Exigence métier. Fenêtres VIP 21 jours, LOCAL 14 jours, LIBRE 5 jours. Correspondance RM à confirmer.
- Test et résultat. `src/test/java/com/transport/gestiontransport/service/ReservationServiceTest.java`. Le contrôle correspondant fait partie des suites réussies sur le candidat de référence.
- Traçabilité. Issue [#2](https://github.com/Zoom225/GestionTransport-Multiple/issues/2), PR [#37](https://github.com/Zoom225/GestionTransport-Multiple/pull/37), commit de preuve `d74caeb`.
- Démonstration. Comparer une demande VIP à J+21 et à J+22, puis effectuer la même vérification pour LOCAL et LIBRE.
- Limite. Les libellés de la source académique restent à confirmer.

```text
VIP/LOCAL/LIBRE ; appliquer le délai à chaque réservation
```

## AA02 · Cahier des charges

Transformer une exigence en six cas limites vérifiables.

- Élément vérifié. `ReservationServiceTest.respecteLaFenetreDeChaqueCategorie`. [`src/test/java/com/transport/gestiontransport/service/ReservationServiceTest.java`](https://github.com/Zoom225/GestionTransport-Multiple/blob/d74caeb117a4b4dd3287a8efd1d5edb71839c6ce/src/test/java/com/transport/gestiontransport/service/ReservationServiceTest.java).
- Exigence métier. Fenêtres et refus hors délai. Correspondance RM à confirmer.
- Test et résultat. `src/test/java/com/transport/gestiontransport/service/ReservationServiceTest.java`. Le contrôle correspondant fait partie des suites réussies sur le candidat de référence.
- Traçabilité. Issue [#4](https://github.com/Zoom225/GestionTransport-Multiple/issues/4), PR [#37](https://github.com/Zoom225/GestionTransport-Multiple/pull/37), commit de preuve `d74caeb`.
- Démonstration. Lancer le test paramétré et rapprocher chaque couple accepté/refusé du cahier validé.
- Limite. Ce cas illustre la démarche de validation. Le cahier officiel complet est attendu.

```text
@org.junit.jupiter.params.provider.CsvSource({"VIP,21,true", "VIP,22,false", "LOCAL,14,true", "LOCAL,15,false", "LIBRE,5,true", "LIBRE,6,false"})
```

## AA03 · Modélisation entité-association et relationnelle

Représenter la réservation comme association entre un trajet et un voyageur.

- Élément vérifié. Classe `Reservation`, champs `trajet` et `voyageur`. [`src/main/java/com/transport/gestiontransport/entity/Reservation.java`](https://github.com/Zoom225/GestionTransport-Multiple/blob/d74caeb117a4b4dd3287a8efd1d5edb71839c6ce/src/main/java/com/transport/gestiontransport/entity/Reservation.java).
- Exigence métier. Réservation rattachée à un voyageur et à un trajet. Correspondance RM à confirmer.
- Test et résultat. `src/test/java/com/transport/gestiontransport/PostgresBaselineTest.java`. Le contrôle correspondant fait partie des suites réussies sur le candidat de référence.
- Traçabilité. Issue [#3](https://github.com/Zoom225/GestionTransport-Multiple/issues/3), PR [#37](https://github.com/Zoom225/GestionTransport-Multiple/pull/37), commit de preuve `d74caeb`.
- Démonstration. Suivre une réservation de la fiche du trajet à ses clés étrangères.
- Limite. Les cardinalités logiques et le mapping physique sont détaillés dans le dossier de diagrammes.

```text
@JoinColumn(name = "trajet_id", nullable = false)
```

## AA04 · Base de données et contraintes d’intégrité

Sérialiser les demandes concurrentes côté PostgreSQL et contrôler les associations.

- Élément vérifié. Interface `TrajetRepository`, méthode `findByIdForUpdate`. [`src/main/java/com/transport/gestiontransport/repository/TrajetRepository.java`](https://github.com/Zoom225/GestionTransport-Multiple/blob/d74caeb117a4b4dd3287a8efd1d5edb71839c6ce/src/main/java/com/transport/gestiontransport/repository/TrajetRepository.java).
- Exigence métier. Capacité du véhicule et unicité des liens. Correspondance RM à confirmer.
- Test et résultat. `src/test/java/com/transport/gestiontransport/PostgresBaselineTest.java`. Le contrôle correspondant fait partie des suites réussies sur le candidat de référence.
- Traçabilité. Issue [#3](https://github.com/Zoom225/GestionTransport-Multiple/issues/3), PR [#37](https://github.com/Zoom225/GestionTransport-Multiple/pull/37), commit de preuve `d74caeb`.
- Démonstration. Relancer `postgresEnforcesUniqueVoyageurAndReservationForeignKeys` en base isolée et consulter le résultat.
- Limite. Les deux tests de schema validate sont complémentaires des tests de concurrence.

```text
@Lock(LockModeType.PESSIMISTIC_WRITE)
```

## AA05 · Architecture trois couches

Propager une demande depuis le contrôleur jusqu’au service et au repository.

- Élément vérifié. Classe `PaiementController`, méthode `payCurrentVoyageur`. [`src/main/java/com/transport/gestiontransport/controller/PaiementController.java`](https://github.com/Zoom225/GestionTransport-Multiple/blob/d74caeb117a4b4dd3287a8efd1d5edb71839c6ce/src/main/java/com/transport/gestiontransport/controller/PaiementController.java).
- Exigence métier. Paiement individuel et détermination serveur de l’identité. Correspondance RM à confirmer.
- Test et résultat. `src/test/java/com/transport/gestiontransport/controller/PaiementControllerTest.java`. Le contrôle correspondant fait partie des suites réussies sur le candidat de référence.
- Traçabilité. Issue [#5](https://github.com/Zoom225/GestionTransport-Multiple/issues/5), PR [#37](https://github.com/Zoom225/GestionTransport-Multiple/pull/37), commit de preuve `d74caeb`.
- Démonstration. Déclencher un paiement simulé et suivre le contrôleur, le service et la persistance.
- Limite. Le service simule un paiement. Il ne génère aucune transaction bancaire.

```text
paiementMapper.toResponse(paiementService.pay(reservationId, voyageurId))
```

## AA06 · Authentification et autorisation

Valider un jeton et contrôler le rôle sur un endpoint protégé.

- Élément vérifié. Classe `JwtConfig`. Sur le candidat d’origine, `extractClaims` vérifie la signature. [`src/main/java/com/transport/gestiontransport/config/JwtConfig.java`](https://github.com/Zoom225/GestionTransport-Multiple/blob/d74caeb117a4b4dd3287a8efd1d5edb71839c6ce/src/main/java/com/transport/gestiontransport/config/JwtConfig.java).
- Exigence métier. Authentification voyageur et administrateur, périmètre de rôle. Correspondance RM à confirmer.
- Test et résultat. `src/test/java/com/transport/gestiontransport/config/JwtConfigTest.java`. La [CI JWT](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/45461746379) réussit sur `131fd7f`. La suite locale avec PostgreSQL a donné 224 réussis, 0 échec et 0 ignoré.
- Traçabilité. Issue [#40](https://github.com/Zoom225/GestionTransport-Multiple/issues/40), PR [#46](https://github.com/Zoom225/GestionTransport-Multiple/pull/46), commit de preuve `131fd7f`.
- Démonstration. Comparer une requête autorisée, une requête avec rôle insuffisant et une requête avec jeton invalide.
- Limite. Le test de rotation est isolé. La clé de production reste à traiter dans l’environnement autorisé.

```text
.verifyWith(getSigningKey())
```

## AA07 · API REST et services sécurisés

Servir l’historique du voyageur authentifié.

- Élément vérifié. Classe `VoyageurController`, méthode `mesSanctions`. [`src/main/java/com/transport/gestiontransport/controller/VoyageurController.java`](https://github.com/Zoom225/GestionTransport-Multiple/blob/d74caeb117a4b4dd3287a8efd1d5edb71839c6ce/src/main/java/com/transport/gestiontransport/controller/VoyageurController.java).
- Exigence métier. Protection de l’historique personnel et du droit au règlement. Correspondance RM à confirmer.
- Test et résultat. `src/test/java/com/transport/gestiontransport/SuspensionParticipationTest.java`. Le contrôle correspondant fait partie des suites réussies sur le candidat de référence.
- Traçabilité. Issue [#31](https://github.com/Zoom225/GestionTransport-Multiple/issues/31), PR [#33](https://github.com/Zoom225/GestionTransport-Multiple/pull/33), commit de preuve `d74caeb`.
- Démonstration. Afficher l’historique du compte de démonstration et vérifier le refus pour un autre propriétaire.
- Limite. Le numéro de règle métier officiel n’est pas confirmé.

```text
@GetMapping("/me/sanctions")
```

## AA08 · Interface utilisateur

Rendre l’état de participation et le règlement visibles sur le profil.

- Élément vérifié. Classe `VoyageurProfilePage`, méthode `payerPenalite`. [`frontend/src/app/features/voyageur/pages/voyageur-profile-page.component.ts`](https://github.com/Zoom225/GestionTransport-Multiple/blob/d74caeb117a4b4dd3287a8efd1d5edb71839c6ce/frontend/src/app/features/voyageur/pages/voyageur-profile-page.component.ts).
- Exigence métier. Visibilité du règlement, suspension maintenue et accessibilité. Correspondance RM à confirmer.
- Test et résultat. `frontend/src/app/features/voyageur/pages/voyageur-profile-page.component.spec.ts`. Vitest et parcours Chromium réel réussis. Le règlement a donné HTTP 204, le solde zéro et la suspension persistante.
- Traçabilité. Issue [#31](https://github.com/Zoom225/GestionTransport-Multiple/issues/31), PR [#33](https://github.com/Zoom225/GestionTransport-Multiple/pull/33), commit de preuve `d74caeb`.
- Démonstration. À 375 px, régler la pénalité du compte de démonstration, recharger le profil et observer le droit de participation.
- Limite. La revue axe-core et le zoom de #39 couvrent un périmètre automatisé ciblé.

```text
payerPenalite(sanction: SanctionResponse): void {
```

## AA09 · Transactions métier et gestion des erreurs

Empêcher une seconde vente de la dernière place et valider le paiement dans la même transaction.

- Élément vérifié. Classe `ReservationServiceImpl`, méthode `preparePaiement`. [`src/main/java/com/transport/gestiontransport/service/impl/ReservationServiceImpl.java`](https://github.com/Zoom225/GestionTransport-Multiple/blob/d74caeb117a4b4dd3287a8efd1d5edb71839c6ce/src/main/java/com/transport/gestiontransport/service/impl/ReservationServiceImpl.java).
- Exigence métier. Paiement unique, capacité, délai et titulaire. Correspondance RM à confirmer.
- Test et résultat. `src/test/java/com/transport/gestiontransport/PostgresBaselineTest.java`. Le contrôle correspondant fait partie des suites réussies sur le candidat de référence.
- Traçabilité. Issue [#4](https://github.com/Zoom225/GestionTransport-Multiple/issues/4), PR [#37](https://github.com/Zoom225/GestionTransport-Multiple/pull/37), commit de preuve `d74caeb`.
- Démonstration. Lancer `concurrentReservationsMustNotSellTheLastPlaceTwice` sur deux demandes de la dernière place.
- Limite. Les tests de concurrence observés utilisent une instance PostgreSQL isolée.

```text
Trajet trajet = verrouillerTrajet(reservation.getTrajet().getId());
```

## AA10 · Tests, GitHub, intégration continue et déploiement

Exécuter les validations sur des commits traçables.

- Élément vérifié. Tâches `backend` et `frontend` du workflow. [`.github/workflows/validation.yml`](https://github.com/Zoom225/GestionTransport-Multiple/blob/d74caeb117a4b4dd3287a8efd1d5edb71839c6ce/.github/workflows/validation.yml).
- Exigence métier. Réservations et intégrité du schéma. Correspondance RM à confirmer.
- Test et résultat. `src/test/java/com/transport/gestiontransport/repository/TransportSchemaValidationTest.java`. Le contrôle correspondant fait partie des suites réussies sur le candidat de référence.
- Traçabilité. Issue [#36](https://github.com/Zoom225/GestionTransport-Multiple/issues/36), PR [#37](https://github.com/Zoom225/GestionTransport-Multiple/pull/37), commit de preuve `d74caeb`.
- Démonstration. Ouvrir un run et montrer les suites Java, PostgreSQL, Angular et le build.
- Limite. L’hébergement de production et les URLs publiques restent à confirmer.

```text
mvn -B -ntp -Dtest=TransportSchemaValidationTest test
```

## AA11 · Documentation et démonstration

Démarrer une démonstration reproductible sur un jeu de données isolé.

- Élément vérifié. Classe `DataInitializer`, méthode `run`. [`src/main/java/com/transport/gestiontransport/DataInitializer.java`](https://github.com/Zoom225/GestionTransport-Multiple/blob/d74caeb117a4b4dd3287a8efd1d5edb71839c6ce/src/main/java/com/transport/gestiontransport/DataInitializer.java).
- Exigence métier. Jeu isolé, comptes de démonstration et préservation des données métier. Correspondance RM à confirmer.
- Test et résultat. `src/test/java/com/transport/gestiontransport/DataInitializerTest.java`. Les 15 cas de DataInitializer réussissent dans la validation de référence.
- Traçabilité. Issue [#34](https://github.com/Zoom225/GestionTransport-Multiple/issues/34), PR [#35](https://github.com/Zoom225/GestionTransport-Multiple/pull/35), commit de preuve `d74caeb`.
- Démonstration. Comparer l’initialisation sur une base de démonstration vide et le refus sur une base métier.
- Limite. Les identifiants et mots de passe de production sont hors du dossier de soutenance.

```text
@Profile("dev")
```

## Modélisation

Les [diagrammes de transport](diagrammes-transport.md) décrivent les cas d’utilisation, la réservation et le paiement, les classes et associations et le schéma de référence.
