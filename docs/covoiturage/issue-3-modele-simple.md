# Issue #3 — modèle simple de covoiturage

Branche : feature/issue-3-domain-persistence, depuis develop 058ddc2 après
intégration revue et testée des PR #8, #9, #10, #12 et #11.

La relation organisateur devient explicitement chauffeur dans Trajet, avec
Voyageur et la même colonne obligatoire organisateur_id. Les associations,
requêtes JPA et références Java sont adaptées sans table Conducteur.
VilleBelge centralise les dix villes et leurs libellés, sans données de trajets
obligatoires. Les colonnes de villes restent nullables pour les anciens trajets,
qui devront être qualifiés avant d'être proposés à la réservation.

Trajet porte le maximum central de quatre clients et une validation du compteur
entre zéro et quatre. La publication automatique exige un consentement explicite,
désactivé par défaut. Voyageur possède un droit de proposition désactivé par
défaut pour les comptes existants. L'audit JPA des dates existantes est activé
pour permettre le calcul fiable de l'échéance des demandes.

Les catégories sont VIP, LOCAL et LIBRE dans Java et Angular. Le convertisseur
conserve GLOBAL et SITE dans la base ; le type AGENCE de l'administrateur reste
distinct et inchangé. L'adaptation minimale des types et formulaires Angular
accompagne ce contrat, sans nouveau composant ni système de rôles.

## Vérifications exécutées

- Maven Java 21 : compilation puis 115 tests, aucun échec, erreur ou test ignoré.
- PostgreSQL : tests opt-in sur issue1_validation_pg_20261111 uniquement.
- Trois tests de persistance ajoutés : chauffeur/villes/audit/mapping VIP,
  chauffeur absent refusé, compteur supérieur à quatre refusé.
- Angular : 58 tests réussis et build de production réussi.
- Aucun lancement Hibernate ni écriture sur transportService. Aucun ancien
  paiement, compte ou trajet métier modifié ; diff local pom.xml conservé.

Cette étape prépare le modèle. L'issue #4 doit encore supprimer la réservation
automatique du chauffeur, déplacer la prise de place au paiement et traiter
expiration/annulation sous le même verrou. Les nouveaux droits et villes seront
contrôlés par les services et exposés par les DTO existants dans l'étape suivante.
