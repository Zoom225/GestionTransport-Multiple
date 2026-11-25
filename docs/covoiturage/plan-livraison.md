# Traçabilité et critères de livraison

## État des références examinées

| Issue | Branche / PR | État constaté |
| --- | --- | --- |
| #1 | chore/issue-1-audit-baseline, PR #8 | Brouillon ouvert, non fusionné ; baseline et PostgreSQL testés dans les sessions précédentes |
| #3, #5, #6 | refactor/transport-nomenclature, PR #9 | Brouillon ouvert, non fusionné ; noms Transport, pas encore le métier covoiturage |
| #4 | fix/issue-4-tarif-trajet-100, PR #10 | Correction demandée 100 €/25 €, dépend de #9 ; 112 tests Java et 58 Angular réussis |
| #2 | feature/issue-2-analyse-transport | Présente analyse, depuis origin/develop 688190e ; aucun changement JPA |

Les commits de référence sont 9691ae3 (baseline), 173ae88 (nomenclature),
3758998 (tarif). Aucun de ces résultats ne vaut validation du modèle cible.
Le worktree de l'analyse est séparé pour laisser le pom.xml local industri
intact dans le répertoire de travail principal.

## Ordre de réalisation après revue et intégration

| Issue / branche prévue | Travail réutilisé et adapté | Critère de passage |
| --- | --- | --- |
| #2 / feature/issue-2-analyse-transport | Analyse du PDF, dictionnaire, règles, cas d'utilisation, UML, données et migrations | Validation du modèle et décisions D01–D04 identifiées |
| #3 / feature/issue-3-domain-persistence | Entités et repositories existants, catégories VIP/LOCAL/LIBRE, conducteur Voyageur, véhicule et villes | Contraintes et compatibilité PostgreSQL vérifiées sur base isolée |
| #4 / feature/issue-4-transport-services | Création conducteur authentifié, disponibilité, demande/paiement, annulation, expiration, restrictions | Tests métier et concurrence au paiement réussis |
| #5 / feature/issue-5-transport-api | DTO/mappers/controllers existants, sécurité et OpenAPI | Tests des contrats et anti-usurpation, aucune fuite technique |
| #6 / feature/issue-6-transport-frontend | Recherche, cartes, profil conducteur, réservation/paiement, admin et statistiques | Build et tests, parcours réels, responsive et clavier |
| #7 / feature/issue-7-integration-validation | Docker, PostgreSQL, démarrage, parcours complet, documentation et démonstration | Contrôles de bout en bout réellement exécutés et tracés |

Chaque branche de code doit partir du dernier develop validé, après intégration
de ses dépendances. Ne pas créer simultanément plusieurs modèles incompatibles.
Chaque PR vise develop, décrit les tests réellement exécutés et attend sa revue.
Les PR #8, #9 et #10 ne sont pas fusionnées automatiquement par cette analyse.

## Matrice minimale des tests à maintenir/ajouter

- Création : conducteur connecté imposé côté serveur, conducteur absent refusé,
  droit de proposer contrôlé, conducteur admin obligatoire, départ/destination
  distincts, véhicule disponible et créneaux conducteur/véhicule compatibles.
- Catégories : VIP à J+21 accepté et J+22 refusé ; local à J+14 et J+15 ; libre
  à J+5 et J+6 ; tous refusés après le départ ; local hors zone non réservable.
- Capacité : quatre clients au maximum ; chauffeur non compté ; confirmation
  des quatre clients puis refus de la cinquième ; recettes 25/50/75/100 €.
- Concurrence PostgreSQL : deux paiements sur dernière place, un seul accepté ;
  double paiement même réservation ; paiement/annulation ; paiement/expiration ;
  création de deux trajets incompatibles pour même conducteur ou véhicule.
- Privé : invité autorisé, tiers refusé, pas de publication sans consentement,
  conversion seulement dans la fenêtre H-24 et traitement répété idempotent.
- Paiements : 25 € dans le cas actuel, montant historique inchangé, dette,
  confirmation, remboursement simulé et historique ; jamais de carte bancaire.
- Restriction : suspension active, expiration sans effacement et absence de
  doublon pour un même fait ; déclencheurs non validés non activés.
- API : filtres villes/date, champs invalides, JWT invalide, propriété des
  réservations/paiements, périmètre administrateur et confidentialité privée.
- UI : vrais résultats ou état vide, aucune fausse disponibilité, chargement,
  erreurs utiles, formulaires réactifs, FR/EN, focus et navigation clavier.
- Responsive : contrôler 375, 768 et 1440 px ; recherche en une colonne sur
  mobile, cartes tactiles, navigation adaptée, tables sans débordement global.

## Données de démonstration et interface

Une liste centrale de villes suffit ; créer les exemples comme données uniquement
dans un profil de développement explicitement activé et une base de démonstration.
Ne pas réactiver l'initialiseur hérité sur une base métier : il modifie aussi des
comptes. Adapter cet initialiseur plutôt que créer un second seed concurrent.

L'accueil cible affiche « Voyagez ensemble, simplement », une recherche départ,
destination/date et des liaisons populaires. Les liaisons d'inspiration peuvent
être des raccourcis de recherche ; elles ne doivent pas afficher de places fictives.
Les cartes de résultats contiennent conducteur, véhicule, places et prix réels.

Réutiliser Material, Tailwind, composants, guards et intercepteur actuels. Aucun
nouveau framework UI. Les statistiques utilisent les réservations/paiements
réels et le périmètre backend, sans graphique décoratif ni compteur inventé.

## Vérifications de cette étape documentaire

PDF de cinq pages lu et comparé au code de la PR #9, PR #8–#10 et issues #1–#7
consultées. Aucun test logiciel nouveau n'est revendiqué pour ces documents.
Les résultats 112/58 concernent la branche de tarif 3758998, pas develop.
Valider les liens, l'absence de secrets et git diff --check avant le commit.
La plateforme complète reste à implémenter après validation et intégration.
