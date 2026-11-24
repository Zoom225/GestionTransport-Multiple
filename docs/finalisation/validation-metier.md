# Validation des règles de transport et de covoiturage

Référence intégrée `67128d5accbf04ad71ca121dadba209502bcb833`. Les tests cités sont présents sur ce commit et relèvent de sa [CI réussie](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/45467082023). Le tableau suit les exigences formulées pour la mission. La numérotation RM01 à RM30 est réservée au cahier des charges officiel, dont la version définitive reste à confirmer.

| Exigence examinée | Source principale | Preuve |
| --- | --- | --- |
| Voyageur authentifié et accès personnel | `VoyageurController.login`, `JwtAuthenticationFilter` | `VoyageurControllerTest.loginVoyageur_ShouldAuthenticateAndReturnToken`, `SuspensionParticipationTest.jwtAuthentifieLeVoyageurEtDistingueLesRefus`. Login et session en navigateur réel sur #36. |
| Administrateur et périmètre d’agence | `AuthServiceImpl.login`, `AdminAuthorizationService` | `AdminAuthorizationServiceTest.globalAdminCanAccessEveryAgence`, `agenceAdminCanAccessOwnAgenceOnly`. Couverture supplémentaire des identifiants invalides suivie dans #58. |
| Fenêtres VIP 21, LOCAL 14, LIBRE 5 jours | `ReservationServiceImpl` | `respecteLaFenetreDeChaqueCategorie` teste la borne incluse et le jour suivant. |
| Voyageur et accès entre agences | `ReservationServiceImpl`, `AdminAuthorizationService` | `shouldThrowWhenAgenceVoyageurBooksOnWrongAgence` et `deuxAgencesDeMemeNomRestentIsoleesParLeurIdentifiant`. |
| Chauffeur et véhicule obligatoires | `TrajetServiceImpl`, relation `Trajet.chauffeur` et `Trajet.vehicule` | `unAdministrateurDoitSelectionnerUnChauffeur`, `lesRelationsChauffeurVehiculeReservationEtPaiementRestentLisibles`. |
| Chauffeur autorisé, véhicule disponible et créneaux | `TrajetServiceImpl`, `TrajetRepository` | `refuseUnChauffeurNonAutorise`, `refuseUnVehiculeIndisponible`, `createTrajet_ShouldFail_WhenSlotIsAlreadyBooked`. |
| Public, privé et invitations | `TrajetController`, `ReservationServiceImpl` | `shouldCreateReservationForPublicTrajet`, `shouldThrowWhenNonOrganizerJoinsPriveTrajet`, `shouldCreateReservationsForInvites` et `T19_chauffeurAffectePeutInviterViaEndpointPrive`. |
| Création et restrictions du proposeur | `TrajetServiceImpl`, `VoyageurEligibilityService` | `createTrajet_ShouldSucceed_WhenAllRulesAreMet`, `T03_creationPriveeRefusee` et `T04_creationPubliqueRefusee`. |
| Réservation unique et quatre passagers hors chauffeur | `ReservationServiceImpl`, invariants PostgreSQL | `leChauffeurNeReservePasUnePlaceClient`, `shouldThrowWhenVoyageurAlreadyRegistered`, `postgresRefuseUneCinquiemePlaceEtUneSecondeDemandeActiveMemeHorsService`. |
| Dernière place, transaction et ordre des paiements | `ReservationServiceImpl.preparePaiement`, `PaiementServiceImpl.pay` | `concurrentReservationsMustNotSellTheLastPlaceTwice`, `quatrePaiementsPuisAnnulationLiberentUnePlaceSansFacturerLeChauffeur`. |
| Règlement de 25 €, simulation et tarif de 100 € | `TrajetService`, `PaiementServiceImpl` | `shouldCreatePaiementWithCorrectAmount`, `shouldThrowWhenAlreadyPaid` et validation décimale de la [PR #47](https://github.com/Zoom225/GestionTransport-Multiple/pull/47). |
| Vérification du propriétaire d’une réservation ou d’un paiement | `PaiementController.payCurrentVoyageur`, `PaiementServiceImpl.checkPaiementOwner` | `apiReserveSansIdentifiantDemandeurFourniParLeNavigateur`. Contrôle du règlement de pénalité par `T18_paiementHTTPReserveAuProprietaireEtHistoriquePrive`. |
| Délai d’impayé et absence de débit après expiration | `ReservationServiceImpl`, `RESERVATION_PAYMENT_TTL` à `PT30M` par défaut | `uneDemandePostgresExpireSansModifierLesPaiementsHistoriques` et `uneDemandeExpireSansDetteEtSansModifierLesPlacesConfirmees`. |
| Annulation de réservation ou de trajet | `ReservationServiceImpl`, `TrajetServiceImpl` | `shouldRefundPaymentWhenCancelledAfterPayment`, `annulationTotaleRembourseEtAnnuleSansEffacerLaDette`. |
| Publication automatique avec consentement | `TrajetServiceImpl`, `TrajetScheduler` | `nePublieJamaisUnTrajetPriveSansConsentement`, `publicationConsentanteDansLes24HeuresNePenalisePasLeChauffeur`. |
| Trajet privé incomplet, pénalité de 100 €, suspension de 168 heures | `TrajetServiceImpl.imputerPenaliteTrajetPriveIncomplet` | `T01_detteCreee`, `T02_suspensionExactement168HeuresEtOrigineConservee`. |
| Paiement de pénalité et suspension persistante | `TrajetServiceImpl.payerPenalite`, `VoyageurEligibilityService` | `T08_paiementAnticipeNeChangePasFin`, `T10_expirationAvecDetteRegleeAutoriseSansRedemarrage`. [Parcours réel #37](https://github.com/Zoom225/GestionTransport-Multiple/pull/37). |
| Historique et absence de doublons de sanction | `RestrictionRepository`, `VoyageurEligibilityService.historique` | `T12_reexecutionIdempotente`, `T13_deuxSanctionsConserventHistoriqueEtFinLaPlusTardive`, `penaliteConcurrenteEtPaiementNeDebitentQueOrganisateurUneSeuleFois`. |

## Décisions métier en attente

- Annulation tardive. L’origine, le délai et l’effet restent soumis à décision. Le paramètre `LATE_CANCELLATION_ENABLED` est désactivé par défaut. [#59](https://github.com/Zoom225/GestionTransport-Multiple/issues/59).
- Absence. Une déclaration habilitée existe. La sanction automatique reste inactive via `NO_SHOW_PENALTY_ENABLED`. [#60](https://github.com/Zoom225/GestionTransport-Multiple/issues/60).
- Annulations répétées du chauffeur. Seuil, fenêtre et effet doivent être confirmés. `DRIVER_CANCELLATION_PENALTY_ENABLED` reste désactivé par défaut. [#61](https://github.com/Zoom225/GestionTransport-Multiple/issues/61).

La correspondance définitive RM01 à RM30 et la validation des règles sur un environnement déployé restent à établir. Les décisions et preuves sont reprises dans la [matrice de traçabilité](https://github.com/Zoom225/GestionTransport-Multiple/pull/43).
