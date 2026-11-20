# Audit full-stack du 18 novembre 2026

## Origine des 17 tests ignores

Aucun `@Disabled`, `@ActiveProfiles`, `@Tag`, assumption ou profil de test restrictif trouve dans src/test. Le profil `dev` concerne les donnees de demonstration, desactivees pendant les tests. Les messages « No active profile set » et l'avertissement JVM CDS ne sont pas des causes d'exclusion.

- **P** — `PostgresBaselineTest` exige une URL `POSTGRES_BASELINE_URL` dont le nom de base suit le motif `issue1_validation_[a-z0-9_]+`. Quinze tests sont ignorés si cette condition échoue. Les tests de contraintes et de verrous utilisent PostgreSQL.
- **S** — `TransportSchemaValidationTest` exige une URL `TRANSPORT_SCHEMA_URL` dont le nom de base suit le motif `transport_schema_copy_[a-z0-9_]+`. Deux tests sont ignorés si cette condition échoue. Ces tests utilisent une copie restaurée et migrée avec Hibernate en mode `validate`.

Les variables definies dans un terminal ne sont pas automatiquement transmises a IntelliJ deja ouvert. Utiliser la configuration **GestionTransport - Tous les tests PostgreSQL**, generee par `scripts/test-all.ps1`, au lieu de l'ancienne configuration JUnit sans ces variables.

## Corrections de cet audit

- scripts/test-all.ps1 : inspection Docker par JSON, corrigeant la relance avec un conteneur existant sous Windows PowerShell ; controle des rapports pour refuser une validation PostgreSQL ignoree.
- src/test/resources/application.properties : commentaire corrige pour distinguer H2 par defaut et PostgreSQL opt-in ; aucune propriete fonctionnelle changee.
- .idea/runConfigurations/GestionTransport_Tests_PostgreSQL.xml : configuration locale regeneree, hors Git.
- Ce rapport. Les autres modifications Git sont anterieures et conservees.

Aucune assertion modifiee, aucun test supprime, aucun service duplique, aucune migration modifiee. Aucune ecriture dans gestiontransport-db ni dans son volume. De nouvelles bases sont creees uniquement dans gestiontransport-tests (PostgreSQL 15, port 127.0.0.1:5542).

## Verification des regles metier

ReservationServiceTest verifie les fenetres VIP/LOCAL/LIBRE, restrictions, dettes, chauffeur exclu des places, invitations privees et annulations. PostgresBaselineTest verifie notamment quatre paiements de 25 EUR, refus de la cinquieme place, concurrence sur la derniere place, annulation/remboursement, expiration sans dette et acces HTTP prive. PaiementServiceImpl englobe paiement et confirmation dans une transaction ; ReservationServiceImpl verrouille le trajet et revalide l'eligibilite au paiement.

**Ecart non resolu : penalite automatique de 80 EUR.** Aucune constante ni assertion de 80 EUR trouvee. TrajetServiceImpl.checkAndConvertExpiredPrivateTrajets traite a H-24 les trajets prives PLANIFIE sans consentement de publication et incomplets. imputerPenaliteTrajetPriveIncomplet impute PRIX_TRAJET_EUROS, soit 4 x 25 = **100 EUR**, au chauffeur lorsque son solde vaut zero. Ce comportement est egalement decrit dans TrajetService et dans les donnees de demonstration. Le montant n'est pas couvert par les tests existants. Le solde nul ne constitue pas un historique de facturation par trajet : plusieurs trajets incomplets et une dette deja presente ne sont pas correctement distingues par cette garde. Clarifier montant, declencheur, cumul et idempotence avant de modifier la regle ; aucune regle metier changee pendant cet audit.

## Configuration et limites

Docker Compose est valide ; gestiontransport-db utilise le volume PostgreSQL externe conserve. Ce conteneur metier est arrete pendant l'audit. Les tests utilisent exclusivement H2 et le conteneur isole gestiontransport-tests. JWT_SECRET est obligatoire au runtime ; une cle reservee aux tests est configuree dans src/test/resources/application.properties. Spring Boot utilise ddl-auto=update par defaut, tandis que le test de copie utilise validate. Aucune migration appliquee a la base metier.

Le build et les tests automatises ne prouvent pas tous les parcours dans un navigateur ni le demarrage HTTP sur la base metier. Le projet ne peut donc pas etre declare entierement fonctionnel, notamment pour la penalite de 80 EUR.

## Resultats de cette execution

- Backend : mvnw verify via scripts/test-all.ps1, 172 reussis, 0 echec, 0 erreur, 0 ignore. Compilation et JAR Spring Boot reussis.
- Frontend : npm test -- --maxWorkers=2, 87 reussis, 0 echec, 0 ignore, 15 fichiers de tests.
- Frontend : npm run build, compilation de production reussie.
- docker compose config --quiet : reussi. git diff --check : reussi (avertissements LF/CRLF uniquement).
- Journaux locaux : .local-validation/review-all-backend.log, review-angular-tests.log et review-angular-build.log.

## Detail des 17 tests initialement ignores

Les codes P et S correspondent aux causes expliquees ci-dessus. Chaque statut provient du rapport Surefire de la derniere execution.

| Test | Cause initiale | Statut final |
| --- | --- | --- |
| `uneDemandePostgresExpireSansModifierLesPaiementsHistoriques` | P | Reussi |
| `apiCreeLeTrajetAvecLeChauffeurAuthentifieEtProtegeLePrive` | P | Reussi |
| `postgresEnforcesUniqueVoyageurAndReservationForeignKeys` | P | Reussi |
| `concurrentReservationsMustNotSellTheLastPlaceTwice` | P | Reussi |
| `apiRechercheMonsBruxellesEtExposeLaListeDesVilles` | P | Reussi |
| `apiReserveSansIdentifiantDemandeurFourniParLeNavigateur` | P | Reussi |
| `quatrePaiementsPuisAnnulationLiberentUnePlaceSansFacturerLeChauffeur` | P | Reussi |
| `restrictionsEvenementiellesIdempotentesEtHistoriqueExpireConserve` | P | Reussi |
| `agencyTypesRemainCompatibleWithLegacySqlValues` | P | Reussi |
| `restrictionsExpireesSontLeveesEtUneRestrictionActiveNestPasDupliquee` | P | Reussi |
| `apiAbsenceExigeUnAdministrateurDeLaBonneAgenceEtConserveLaDeclaration` | P | Reussi |
| `uneRessourceHistoriqueNestPasMiseEnServiceAutomatiquement` | P | Reussi |
| `postgresRefuseUneCinquiemePlaceEtUneSecondeDemandeActiveMemeHorsService` | P | Reussi |
| `deuxRestrictionsConcurrentesPourLeMemeFaitNeCreentQuUneLigne` | P | Reussi |
| `detteExigibleBloqueLePaiementSansConfondreLesDemandesEnAttente` | P | Reussi |
| `lesRelationsChauffeurVehiculeReservationEtPaiementRestentLisibles` | S | Reussi |
| `lesMappingsTransportLisEntLesDonneesRestaureesSansTablesParalleles` | S | Reussi |
