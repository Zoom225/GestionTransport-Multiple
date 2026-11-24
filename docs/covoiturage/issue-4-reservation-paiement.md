# Issue #4 — Chauffeur et premier paiement confirmé

Branche : `feature/issue-4-transport-services`. Dépend de la PR #13 (issue #3).

## Comportement

- Le chauffeur est le Voyageur authentifié, autorisé via `peutProposerTrajet`.
  L'administrateur doit sélectionner un Voyageur existant dans son périmètre.
- Deux villes belges distinctes et un départ futur sont obligatoires.
  Le chauffeur et le véhicule sont verrouillés avant le contrôle des chevauchements.
  Les durées et horaires configurés de l'agence sont réutilisés.
- Le chauffeur ne reçoit aucune réservation ni aucun paiement et ne peut pas
  réserver sur son propre trajet. Il reste quatre places réservées aux clients.
- Une demande EN_ATTENTE et son paiement de 25 € ne bloquent aucune place.
  Plusieurs demandes peuvent donc concourir pour la même dernière place.
- Le paiement simulé verrouille le trajet et recharge son état PostgreSQL.
  Seul un paiement accepté confirme la demande et incrémente le compteur existant.
  À quatre clients confirmés, le trajet devient COMPLET. Une cinquième tentative
  échoue sans encaissement. Une annulation confirmée rembourse et libère une place.
- Le tarif centralisé utilise BigDecimal : 25,00 € par client, 100,00 € de référence.
  Les colonnes historiques Double restent inchangées ; aucun ancien paiement
  n'est recalculé et les places inoccupées ne sont facturées à personne.
- Les fenêtres s'appliquent aux réservations : VIP J-21, LOCAL J-14 dans son agence,
  LIBRE J-5. Restrictions et dette active empêchent la réservation et le paiement.
- Une demande expire après `RESERVATION_PAYMENT_TTL` (défaut `PT30M`).
  Le statut EXPIREE conserve l'historique et annule le paiement encore en attente.
  Le traitement est périodique, sans créer une dette au chauffeur.
- Un trajet privé ne devient public qu'avec consentement enregistré et dans les
  24 heures précédant le départ. Aucune pénalité pour des places inoccupées.

## Données historiques et migration

Les trajets sans villes renseignées restent consultables dans leur historique,
mais sont exclus de la recherche et des nouvelles réservations de covoiturage.
Le traitement d'expiration les ignore aussi. Une demande sans date de création
historique nécessite une régularisation explicite ; aucune date n'est inventée.

Hibernate `update` est conservé. Il ne modifie pas automatiquement le CHECK PostgreSQL
des statuts. `scripts/migrations/issue-4-reservation-expiration.sql` ajoute EXPIREE
à ce CHECK sans réécrire de ligne. Application réelle uniquement sur la base
isolée `issue1_validation_pg_20261111`, jamais sur `transportService`.
Sur une base métier existante : sauvegarde, revue de la contrainte, application
explicite du script avant activation du nouveau traitement.

## Vérifications exécutées

Maven : 129 tests, zéro échec, cinq tests PostgreSQL réels inclus.
Ils vérifient notamment quatre paiements (100 €), refus du cinquième,
annulation et réutilisation d'une place, deux paiements concurrents sur la dernière
place, expiration PostgreSQL et contraintes relationnelles.
Tests unitaires des fenêtres VIP/LOCAL/LIBRE et de l'exclusion du chauffeur.
Compilation IntelliJ réussie (avertissements MockBean préexistants).
Angular : les 58 tests existants passent (`npm test`).

## Suite

## Complément soutenance du 12 novembre 2026 — issue #4

Le service Voyageur existant centralise les restrictions ; aucun second moteur n'est
ajouté. `addRestrictionPourEvenement` verrouille le voyageur et enregistre un fait unique.
La contrainte unique de `source_evenement` protège également les écritures concurrentes.
Les nouvelles restrictions ont une date de début et une fin exclusive à J+7 par défaut.
Les anciennes dates inconnues ne sont pas inventées. L'expiration se calcule à la lecture,
sans suppression d'historique ni tâche planifiée supplémentaire.

ReservationService applique les politiques optionnelles d'annulation tardive et abusive
du chauffeur. Seuls les faits d'annulation réellement enregistrés depuis cette évolution
entrent dans le comptage chauffeur : aucune date historique n'est reconstruite. Les seuils
restent à zéro et les politiques désactivées jusqu'à validation métier explicite.
Une annulation imposée par le chauffeur ne sanctionne pas automatiquement le client.

L'absence est un acte administratif explicite sur une réservation confirmée et payée,
après départ d'un trajet non annulé. Le déclarant et le motif sont tracés. Aucune absence
n'est inférée par un scheduler. La déclaration est idempotente ; la suspension ne se
déclenche que si la politique d'absence a été expressément activée.

Un solde exigible positif reste bloquant, contrairement à une demande de paiement en
attente. Aucun montant de sanction ou dette automatique n'est créé. Les règles de paiement,
verrous de dernière place et remboursements existants sont réutilisés.

Configuration, endpoints, migration additive et usage Angular sont détaillés dans le README.

Issue #5 : exposer les villes et la recherche, compléter les DTO et sécuriser les
accès privés et modifications par l'identité authentifiée.
Issue #6 : adapter les formulaires Angular et les cartes de trajets à ces contrats.
Ces services seuls ne constituent pas encore la livraison complète de l'interface.
# Renforcement de C01 — villes de départ et d'arrivée distinctes

Branche `fix/issue-4-villes-distinctes`, depuis #26. La règle C01 existait déjà
dans le service de création, mais les sélecteurs autorisaient un choix identique
et le message n'apparaissait qu'à la soumission. Aucun changement du modèle SQL,
des DTO, de la tarification ou des données de démonstration n'est nécessaire.

Un validateur réactif partagé contrôle maintenant les formulaires de création
et de recherche. La ville choisie est désactivée dans l'autre liste. Si une valeur
identique est imposée au formulaire, un message traduit FR/EN apparaît immédiatement,
le formulaire est invalide et aucun envoi n'est effectué. Corriger la destination
efface l'erreur. Les filtres vides ou partiels restent autorisés en recherche.

Le service vérifie la différence des villes avant de charger et verrouiller le
véhicule. Une création ou recherche directe avec deux villes identiques retourne
HTTP400 : « Choisissez deux villes différentes : la ville de départ et la
destination ne peuvent pas être identiques. » Les villes manquantes restent
contrôlées par les validations existantes de création. Les autorisations sont
conservées et les anciens trajets ne sont pas modifiés.

Résultats exécutés le 14 novembre 2026 :

- `mvn -B verify` final : 169 tests réussis, sans échec, erreur ou ignoré,
  PostgreSQL isolé et validation JPA sur copie inclus.
- Build Angular final et 78 tests réussis. Tests ajoutés : réaction immédiate du
  formulaire de création, refus d'émission de recherche invalide, correction et
  filtres facultatifs ; service sans verrouillage/écriture sur villes identiques.
- Le premier passage backend a signalé un mock de véhicule devenu inutile dans
  l'ancien test de rejet des villes identiques ; retrait de ce seul mock, aucune
  suppression de test ni assouplissement de Mockito.
- Compilation IntelliJ réussie, relance sur copie migrée en17,748s ; le serveur
  Angular du projet a été relancé après les compilations pour les contrôles finaux.
- REST réel sur copie locale : création/recherche MONS→MONS HTTP400 avec message
  clair, aucun trajet ajouté ; MONS→BRUXELLES et recherche MONS seul HTTP200.
- Navigateur Edge : recherche et création à375/768/1440, même ville désactivée
  dans les listes ; valeur identique forcée sur le formulaire de création en
  mode développement : message immédiat, bouton désactivé, erreur effacée après
  correction. Aucun débordement horizontal ni erreur JavaScript.
