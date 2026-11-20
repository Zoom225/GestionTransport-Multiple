# Organisateur et penalite de trajet prive incomplet

## Regle confirmee

La dette de 100 EUR (4 places x 25 EUR) concerne le voyageur organisateur. Le chauffeur affecte ne doit jamais etre debite du seul fait qu'il conduit. Le voyageur ne choisit pas le chauffeur.

## Code disponible

- Trajet.organisateur : relation distincte, colonne voyageur_organisateur_id. Ce nom evite toute collision avec organisateur_id dans l'ancien schema et sa migration historique vers chauffeur_id.
- Imputation dans la fenetre H-24, uniquement pour un trajet prive PLANIFIE incomplet sans consentement de publication et avec organisateur identifie.
- Verrou de trajet puis verrou du voyageur ; ajout de 100 EUR au solde existant et enregistrement de la date d'imputation. Une relance ne refacture pas le trajet.
- Paiement simule dans TrajetService.payerPenalite : seul l'organisateur peut regler, retrait de 100 EUR sans effacer une autre dette, date de paiement conservee, double appel sans nouveau debit.
- Invitations privees autorisees pour l'organisateur identifie et le chauffeur affecte. Les controles de dette et de suspension du voyageur invite restent obligatoires. Pour les anciens trajets sans organisateur, les droits historiques restent inchanges mais aucune penalite d'incompletude ne peut debiter leur chauffeur.
- Migration additive scripts/migrations/issue-8-organisateur-penalite.sql preparee, non appliquee sur la base metier. Aucun transfert automatique des dettes historiques.

## Raccordement encore en attente

Le flux actuel de creation confond l'identite du voyageur et celle du chauffeur. L'administration peut choisir un chauffeur ; aucun mecanisme d'affectation automatique distinct n'existe. Le nouveau champ organisateur n'est pas encore alimente par le flux de creation. Le paiement simule n'est pas encore expose dans l'API et l'interface. La fonctionnalite n'est donc pas livree de bout en bout.

Une proposition d'affectation d'un chauffeur disponible a ete rejetee par la revue automatique pour son impact sur la creation et les autorisations. Aucune affectation automatique n'a ete introduite. Choisir entre une affectation prealable par l'administrateur et une affectation automatique explicitement autorisee avant le raccordement final. Les tests exploratoires de cette proposition sont conserves hors du code executable dans .local-validation/affectation-proposition.diff ; aucun test preexistant n'a ete supprime.

## Verification

PenaliteOrganisateurTest contient cinq tests : organisateur redevable et chauffeur inchange, idempotence, autres dettes conservees, exclusion des anciens trajets sans organisateur et des trajets non eligibles, paiement simule et autorisation. ReservationServiceTest contient deux nouveaux tests distinguant organisateur et chauffeur pour les invitations. PostgresBaselineTest ajoute un test reel d'imputation et de paiement concurrents, tous deux une seule fois.

Les nouvelles migrations et les nouveaux tests n'autorisent aucune modification automatique de la base metier.

## Resultat du 18 novembre 2026

`mvnw verify` via `scripts/test-all.ps1` : **180 tests reussis, 0 echec, 0 erreur, 0 ignore**, compilation et JAR Spring Boot reussis. Les 16 tests PostgreSQL sont executes sur une nouvelle base isolee. La restauration du schema historique et sa migration reussissent avec le nouveau champ voyageur_organisateur_id. La migration issue-8 a ete executee deux fois avec succes sur la copie de test ; aucune execution sur la base metier. `git diff --check` reussi. Aucun fichier frontend modifie dans ce chantier ; la derniere validation Angular precedente reste 87 tests reussis et build de production reussi.

La fonctionnalite reste partielle tant que l'affectation du chauffeur et le raccordement de l'organisateur dans le flux applicatif ne sont pas clarifies et implementes. Aucun test preexistant ni aucune assertion preexistante n'a ete supprime ou affaibli.

## Mise en coherence de la demonstration

DataInitializer cree maintenant V1001 comme chauffeur affecte sans dette, V1002 comme organisateur des huit trajets a venir et F10002 comme organisateur du trajet prive historique incomplet. Ce dernier est date d'hier et porte sa date d'imputation : la dette de 100 EUR est une fixture deja comptabilisee, sans nouveau debit par le scheduler. La restriction de sept jours est une autre fixture explicite. Les comptes et mots de passe de demonstration restent identiques.

Le DTO expose les identites de l'organisateur et du chauffeur independamment ainsi que les dates d'imputation/paiement. La liste des trajets organises et l'acces prive reconnaissent l'organisateur explicite. Le comportement historique est conserve pour les trajets sans organisateur renseigne. Aucun algorithme d'affectation automatique ni choix de chauffeur dans l'espace voyageur n'est ajoute.

Le generateur reste sans effet sur une base deja remplie. Il faut une base neuve isolee pour profiter du scenario corrige ; aucune reattribution de compte ou de dette n'est effectuee sur les anciennes bases.

## Validation finale de la demonstration

Backend : 184 tests reussis, 0 echec, 0 erreur, 0 ignore ; compilation et JAR reussis. Frontend : build de production reussi et 87 tests reussis. Les nouveaux tests verifient les roles distincts, la dette initiale, le mapping, la liste des trajets et l'acces prive.

Base neuve creee dans gestiontransport-tests : transport_demo_coherent_20261118173313, port 5542. Verification HTTP reussie : connexion F10002 / Voyage1234!, dette 100 EUR, restriction active ; connexion V1001 / Voyage1234!, solde 0 EUR ; un trajet prive consulte par son organisateur F10002, chauffeur distinct V1001, imputation enregistree non payee. Aucun token conserve dans le rapport.

Le backend temporaire sur le port 18082 est arrete apres controle. Pour relancer cette demonstration sur le port 8080 : .\.local-validation\lancer-demo-coherente.ps1

Le script local genere une cle JWT, utilise la base deja initialisee en mode validate et laisse les autres bases intactes. L'ancienne configuration IntelliJ n'a pas ete redirigee. Le raccordement complet de la creation applicative et de l'interface de paiement de penalite reste un chantier distinct.
