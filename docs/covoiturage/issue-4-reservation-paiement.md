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

Issue #5 : exposer les villes et la recherche, compléter les DTO et sécuriser les
accès privés et modifications par l'identité authentifiée.
Issue #6 : adapter les formulaires Angular et les cartes de trajets à ces contrats.
Ces services seuls ne constituent pas encore la livraison complète de l'interface.
