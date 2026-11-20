# Rapport de correction GestionTransport

Branche vérifiée avant intervention `fix/backend-config-and-improvements`. Les changements locaux étaient déjà nombreux dans Java, Angular, la documentation et les scripts. Les corrections ont été appliquées au-dessus de ces changements. Aucun commit, push, merge, rebase ou changement de branche.

Backend Java 21.0.9, Spring Boot 3.5.0, Hibernate 6.6.15.Final, Maven Wrapper 3.9.16. Frontend Angular 21.2.26, TypeScript 5.9.3 et Vitest 4.1.12, avec Node 24.19 et npm 11.17 utilisés pour la validation locale.

## Résultats finaux des suites

| Validation | Résultat | Preuve locale |
|---|---|---|
| Maven `clean verify` | 199 tests réussis, 20 conditionnels traités séparément, aucun échec. JAR Spring Boot produit. | Journal Maven (`.local-validation/mission-h2-verify.log`) |
| PostgreSQL isolé | 18 tests métier et 2 tests de copie de schéma réussis. Aucun échec ni test ignoré lors de cette exécution. | Journal PostgreSQL (`.local-validation/mission-postgres-verify.log`) |
| Angular `npm test -- --watch=false --maxWorkers=2` | 94 tests réussis dans 16 fichiers. | Journal Vitest (`.local-validation/mission-angular-verify.log`) |
| Angular `npm run build` | Bundle de production généré. | Journal Angular (`.local-validation/mission-angular-build-final.log`) |
| Edge → API → PostgreSQL → profil | Règlement interne HTTP 204, solde à 0 €, suspension et blocage toujours actifs, aucune erreur JavaScript. La lecture PostgreSQL a confirmé le solde, le marqueur de règlement et la restriction active (`0\|t\|t`). | Résultat navigateur (`.local-validation/mission-evidence/browser-result.json`), avant (`.local-validation/mission-evidence/profil-avant.png`), après (`.local-validation/mission-evidence/profil-apres.png`) |

Le bilan backend est de 219 tests réussis sur les deux exécutions. La relance groupée avait rencontré des expirations lors de la négociation TLS JDBC. La validation ciblée sur PostgreSQL a utilisé les mêmes sources et migrations, avec un délai JDBC augmenté sur loopback. Les migrations ont ciblé exclusivement les bases de validation et de copie du conteneur réservé aux tests. `git diff --check` a réussi.

Le contrôle Edge utilise la pénalité historique du compte de démonstration, afin de vérifier aussi la compatibilité avec les données existantes. Le test HTTP à horloge injectée vérifie séparément la création automatique de la nouvelle sanction et son expiration exacte à 168 heures.

Le premier `npm ci` dans le dossier actif s’est interrompu sur `esbuild.exe`, détenu par des processus Node de ce projet. Certaines dépendances locales manquent depuis cette interruption. L’installation propre a réussi dans une copie isolée du frontend, utilisée pour les tests et le build. La revue automatique a rejeté la restauration par copie, car elle aurait modifié l’installation active de l’IDE. La remise en état de ce dossier nécessite de libérer ces verrous puis d’y relancer `npm ci`.

## Analyse des six échecs

| Test initial | Cause observée | Correction |
|---|---|---|
| `T05_inscriptionRefusee` | `ReservationServiceImpl.create → verifierEligibilite` renvoyait une BusinessException générique (« restriction active »). | Le service verrouille le voyageur puis applique le code et le message issus de l’éligibilité centralisée. |
| `T06_invitationParAutreOrganisateurRefuseeSansDetailsFinanciers` | `createForInvites` exigeait l’identité du chauffeur avant de vérifier l’invité. L’organisateur distinct était rejeté. | Les droits documentés de l’organisateur et du chauffeur affecté sont reconnus. Le refus lié au voyageur invité ne donne ni montant ni solde. |
| `T07_paiementEtConfirmationRefuses` | `PaiementServiceImpl.pay → ReservationServiceImpl.preparePaiement`, ainsi que `confirm`, utilisaient le contrôle historique. | Les deux entrées vérifient la participation dans leur transaction avec le même verrou que la sanction et le règlement. |
| `T15_consultationEtReglementAccessibles` | `VoyageurController.me` retournait le DTO sans `participation`. | Le profil et la connexion enrichissent le DTO personnel. Le test couvre le JSON, l’historique, le règlement répété et l’expiration. |
| `T16_endpointsCreationEtInscriptionNonContournables` | Les créations renvoyaient `ParticipationException`. L’inscription renvoyait une BusinessException dont le code JSON était nul. | Les deux parcours envoient `VOYAGEUR_SUSPENDU`. Le statut 400 du contrat existant est conservé. |
| `T18_paiementHTTPReserveAuProprietaireEtHistoriquePrive` | Sans route de règlement, `ResourceHttpRequestHandler` levait `NoResourceFoundException` et le gestionnaire générique répondait 500. | Route restreinte au rôle voyageur, contrôle de l’organisateur dans le service, 403 pour un tiers et 404 explicite pour une route absente. |

Les six tests ci-dessus ont repassé lors de l’exécution ciblée de 86 tests. Les assertions portant sur les anciennes phrases ont été adaptées uniquement aux messages et statuts demandés. Aucun test n’a été désactivé.

## Fonctionnement et sécurité

- Le traitement H‑24 conserve la condition du trajet privé incomplet et sans consentement de publication. Pour un organisateur identifié, la dette de 100 € et la sanction sont enregistrées dans la même transaction. Les dates exactes sont des `Instant`, obtenues avec le `Clock` injecté. L’expiration est exclusive et le blocage tient compte de la fin la plus tardive. Le traitement répété ne débite pas une seconde fois.
- L’API protège les créations privées et publiques, les inscriptions, les invitations et le paiement ou la confirmation d’une place. Pour un ajout, elle vérifie l’invité sous verrou. L’autorisation de l’auteur reste indépendante.
- Le règlement est la confirmation du mécanisme interne déjà présent dans `TrajetServiceImpl.payerPenalite`. Le serveur fixe le montant. Le voyageur ne transmet ni montant ni identifiant de payeur. Le trajet et le voyageur sont verrouillés et l’horodatage conservé rend le double appel idempotent. Le service refuse un solde insuffisant et conserve les autres dettes ainsi que la suspension. Aucun prestataire bancaire n’est intégré.
- Les anciennes pénalités identifiées sur un trajet restent visibles et réglables. Les restrictions historiques à dates civiles conservent leurs dates d’origine. Les dettes historiques dépourvues d’un trajet imputé restent visibles au solde et demandent une régularisation.
- Les créations existantes sans organisateur explicite, notamment le flux administrateur, ne changent pas d’attribution. Le système n’impute aucune dette au chauffeur par déduction.

## Contrats HTTP et écrans

- `GET /api/voyageurs/me` et `POST /api/voyageurs/login` incluent `participation` dans le profil personnel. Cet objet indique les dates de suspension, le motif, la dette bloquante et les droits de créer ou de rejoindre un trajet.
- `GET /api/voyageurs/me/sanctions` renvoie l’historique du voyageur connecté, comprenant le montant, les dates disponibles et l’état de règlement.
- `POST /api/trajets/{id}/penalite/pay` répond 204 après un règlement interne ou une répétition déjà réglée. Le rôle voyageur et la propriété sont vérifiés. `POST /api/reservations/private-invites` exige explicitement le rôle voyageur.
- Le profil Angular affiche la dette, l’historique, le règlement et la suspension. L’état personnel est rechargé après règlement, puis automatiquement par la session, avec un rafraîchissement programmé à proximité de l’expiration. Les pages de création, de trajets publics et de réservations désactivent les actions correspondant à l’état reçu.

## PostgreSQL et scénario reproductible

Le projet utilise JPA `ddl-auto=update` et des scripts SQL versionnés, sans Flyway. Les migrations additives existantes des tickets 8, 9 et 10 couvrent l’organisateur, les marqueurs d’imputation et de règlement, le lien unique vers le trajet, les clés étrangères, la contrainte de 168 heures et la trace d’annulation. Aucune migration métier n’a été modifiée.

`SuspensionParticipationTest#parcoursDemonstrationDeLEcheanceALaReactivation` crée un trajet privé via le service, avance le Clock de test à H‑24, lance deux fois le traitement, vérifie le débit et l’intervalle, puis exerce les routes HTTP de création, d’invitation, de paiement de la pénalité et de réservation. Après règlement, la même inscription est refusée avant la borne et acceptée à 168 heures. La seule horloge avancée est celle du test.

```powershell
.\mvnw.cmd '-Dtest=SuspensionParticipationTest#parcoursDemonstrationDeLEcheanceALaReactivation' test
.\scripts\test-all.ps1 -NoIdeConfig
```

## Les 20 exclusions de l’audit

Les 18 premiers tests exigeaient une URL nommée `issue1_validation_*` (variable `POSTGRES_BASELINE_URL`). Les deux derniers exigeaient une URL `transport_schema_copy_*` (variable `TRANSPORT_SCHEMA_URL`). Les rapports Surefire indiquaient que ces variables étaient absentes. Aucun rapport Failsafe ni test explicitement désactivé n’a été trouvé dans l’audit.

Statut final pour chacun des vingt tests ci-dessous : **réussi** sur PostgreSQL.

| Suite | Test |
|---|---|
| PostgreSQL | `uneDemandePostgresExpireSansModifierLesPaiementsHistoriques` |
| PostgreSQL | `apiCreeLeTrajetAvecLeChauffeurAuthentifieEtProtegeLePrive` |
| PostgreSQL | `postgresEnforcesUniqueVoyageurAndReservationForeignKeys` |
| PostgreSQL | `concurrentReservationsMustNotSellTheLastPlaceTwice` |
| PostgreSQL | `T17_sanctionPaiementEtInscriptionConcurrentsRespectentLaSuspension` |
| PostgreSQL | `apiRechercheMonsBruxellesEtExposeLaListeDesVilles` |
| PostgreSQL | `apiReserveSansIdentifiantDemandeurFourniParLeNavigateur` |
| PostgreSQL | `quatrePaiementsPuisAnnulationLiberentUnePlaceSansFacturerLeChauffeur` |
| PostgreSQL | `restrictionsEvenementiellesIdempotentesEtHistoriqueExpireConserve` |
| PostgreSQL | `agencyTypesRemainCompatibleWithLegacySqlValues` |
| PostgreSQL | `restrictionsExpireesSontLeveesEtUneRestrictionActiveNestPasDupliquee` |
| PostgreSQL | `penaliteConcurrenteEtPaiementNeDebitentQueOrganisateurUneSeuleFois` |
| PostgreSQL | `apiAbsenceExigeUnAdministrateurDeLaBonneAgenceEtConserveLaDeclaration` |
| PostgreSQL | `uneRessourceHistoriqueNestPasMiseEnServiceAutomatiquement` |
| PostgreSQL | `postgresRefuseUneCinquiemePlaceEtUneSecondeDemandeActiveMemeHorsService` |
| PostgreSQL | `deuxRestrictionsConcurrentesPourLeMemeFaitNeCreentQuUneLigne` |
| PostgreSQL | `detteExigibleBloqueLePaiementSansConfondreLesDemandesEnAttente` |
| PostgreSQL | `organisateurDistinctConsulteSonTrajetPriveAvecLeBonChauffeur` |
| Copie de schéma | `lesRelationsChauffeurVehiculeReservationEtPaiementRestentLisibles` |
| Copie de schéma | `lesMappingsTransportLisEntLesDonneesRestaureesSansTablesParalleles` |

## Fichiers modifiés par cette intervention

Sous `src/main/java/com/transport/gestiontransport` :

- `config/SecurityConfig.java`
- `controller/TrajetController.java`, `controller/VoyageurController.java`
- `dto/response/VoyageurResponse.java`, `exception/GlobalExceptionHandler.java`
- `repository/RestrictionRepository.java`, `repository/TrajetRepository.java`
- `service/impl/ReservationServiceImpl.java`, `service/impl/TrajetServiceImpl.java`, `service/impl/VoyageurEligibilityService.java`

Sous `src/test/java/com/transport/gestiontransport` :

- `SuspensionParticipationTest.java`, `controller/VoyageurControllerTest.java`, `service/ReservationServiceTest.java`, `service/VoyageurEligibilityServiceTest.java`

Sous `frontend/src/app` :

- `core/api/trajets-api.service.ts`, `core/api/voyageurs-api.service.ts`, `core/auth/voyageur-session.service.ts`, `shared/models/voyageur.model.ts`
- `features/voyageur/pages/voyageur-profile-page.component.{ts,html,spec.ts}` et `voyageur-create-trajet-page.component.ts`
- `features/voyageur/pages/voyageur-public-trajets-page.component.{ts,spec.ts}` et `voyageur-reservations-page.component.{ts,spec.ts}`

Le script `scripts/test-all.ps1` prépare les bases de test, applique les migrations sur celles-ci et lance `clean verify`. `-NoIdeConfig` conserve la configuration IntelliJ existante. La base métier et son volume n’ont pas été ciblés.
