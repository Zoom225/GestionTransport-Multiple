# Rapport final de préparation du TFE

État au 24 novembre 2026. Ce rapport rend compte de la finalisation vérifiée dans `develop` et des pull requests associées. Les résultats de livraison et de déploiement seront renseignés après autorisation et contrôle des environnements réels.

## 1. Résumé exécutif

Le candidat Transport est consolidé dans `develop`. L’audit, l’intégration de la chaîne existante, le carrousel, la nomenclature, le durcissement JWT, les montants décimaux et les premières preuves de soutenance sont publiés sur le dépôt officiel. La fusion finale, la migration de production et le déploiement restent des opérations séparées.

## 2. État initial

L’audit a relevé `master` à `688190e`, `develop` à `b818392` et le dernier candidat à `d74caeb`. Les PR #28, #30, #32 et #33 étaient empilées. Les deux fichiers non suivis observés dans le dossier de travail de l’utilisateur sont préservés. [Audit initial](audit-initial.md).

## 3. Corrections réalisées

Intégration sans réécriture d’histoire, période du carrousel restaurée, nom du JAR et du contrat de participation harmonisés, validation JWT au démarrage, séparation des réponses 401 et 403 et migration monétaire explicite. Les détails, tests et risques figurent dans les PR ci-dessous.

## 4. Issues créées

| Domaine | Issues créées |
| --- | --- |
| Audit, intégration, nomenclature | [#34](https://github.com/Zoom225/GestionTransport-Multiple/issues/34), [#36](https://github.com/Zoom225/GestionTransport-Multiple/issues/36), [#38](https://github.com/Zoom225/GestionTransport-Multiple/issues/38) |
| Sécurité, argent, dépendances | [#40](https://github.com/Zoom225/GestionTransport-Multiple/issues/40), [#45](https://github.com/Zoom225/GestionTransport-Multiple/issues/45), [#53](https://github.com/Zoom225/GestionTransport-Multiple/issues/53), [#54](https://github.com/Zoom225/GestionTransport-Multiple/issues/54), [#55](https://github.com/Zoom225/GestionTransport-Multiple/issues/55) |
| Traçabilité, dépôt et acquis | [#42](https://github.com/Zoom225/GestionTransport-Multiple/issues/42), [#48](https://github.com/Zoom225/GestionTransport-Multiple/issues/48), [#50](https://github.com/Zoom225/GestionTransport-Multiple/issues/50), [#66](https://github.com/Zoom225/GestionTransport-Multiple/issues/66) |
| Métier, PostgreSQL et non-régression | [#56](https://github.com/Zoom225/GestionTransport-Multiple/issues/56), [#57](https://github.com/Zoom225/GestionTransport-Multiple/issues/57), [#58](https://github.com/Zoom225/GestionTransport-Multiple/issues/58), [#68](https://github.com/Zoom225/GestionTransport-Multiple/issues/68) |
| Décisions métier | [#59](https://github.com/Zoom225/GestionTransport-Multiple/issues/59), [#60](https://github.com/Zoom225/GestionTransport-Multiple/issues/60), [#61](https://github.com/Zoom225/GestionTransport-Multiple/issues/61) |
| Déploiement et livraison | [#44](https://github.com/Zoom225/GestionTransport-Multiple/issues/44), [#52](https://github.com/Zoom225/GestionTransport-Multiple/issues/52), [#67](https://github.com/Zoom225/GestionTransport-Multiple/issues/67) |

## 5. Issues réutilisées

[#29](https://github.com/Zoom225/GestionTransport-Multiple/issues/29) pour la durée et les preuves du carrousel. Les issues [#7](https://github.com/Zoom225/GestionTransport-Multiple/issues/7) et [#31](https://github.com/Zoom225/GestionTransport-Multiple/issues/31) restent les références transversales d’intégration et de profil voyageur.

## 6. Issues clôturées

Les chantiers suivis restent ouverts pour la livraison effective sur la branche par défaut. Les PR empilées #30, #32 et #33 ont été clôturées après vérification de l’intégration de leurs têtes dans `develop`. La PR #28 est reconnue comme fusionnée.

## 7. Issues encore ouvertes

Le dépôt et la matrice donnent l’état courant. Les décisions #59, #60 et #61, le déploiement #52 et la livraison #67 nécessitent des validations distinctes.

## 8–11. Branches, commits, pushs et pull requests

Les branches ci-dessous correspondent aux publications identifiées. Pour chaque ligne, le lien de PR donne l’historique des commits et des pushs associés. Les branches de décision sans changement sont suivies par leur issue.

| Issue | Branche publiée | Commit de preuve | PR |
| --- | --- | --- | --- |
| #34 | `docs/issue-34-audit-initial-tfe` | `066cb53`, `b04cb4d` | [#35](https://github.com/Zoom225/GestionTransport-Multiple/pull/35) |
| #36 | `release/issue-36-integration-transport-tfe` | `f5c9dc6` | [#37](https://github.com/Zoom225/GestionTransport-Multiple/pull/37) |
| #29 | `fix/issue-29-rotation-carrousel` | `3abe08b`, `80e8006` | [#39](https://github.com/Zoom225/GestionTransport-Multiple/pull/39) |
| #38 | `refactor/issue-38-nettoyage-contexte-transport` | `dfec7a2`, `83fa93e` | [#41](https://github.com/Zoom225/GestionTransport-Multiple/pull/41) |
| #40 | `security/issue-40-securisation-jwt` | `131fd7f` | [#46](https://github.com/Zoom225/GestionTransport-Multiple/pull/46) |
| #42 | `docs/issue-42-tracabilite-tfe` | `2517d2f`, `2c906a2` | [#43](https://github.com/Zoom225/GestionTransport-Multiple/pull/43) |
| #44 | `release/issue-44-resoudre-readme-master` | `64e62fe` | [#63](https://github.com/Zoom225/GestionTransport-Multiple/pull/63) |
| #45 | `refactor/issue-45-montants-bigdecimal` | `4f92a87`, `08eafe9` | [#47](https://github.com/Zoom225/GestionTransport-Multiple/pull/47) |
| #48 | `chore/issue-48-modeles-github` | `393d8c3` | [#49](https://github.com/Zoom225/GestionTransport-Multiple/pull/49) |
| #50 | `docs/issue-50-validation-onze-acquis-tfe` | `e91906e` | [#51](https://github.com/Zoom225/GestionTransport-Multiple/pull/51) |
| #53 | `security/issue-53-actualiser-spring-boot` | `c8fbf78` | [#69](https://github.com/Zoom225/GestionTransport-Multiple/pull/69) |
| #54 | `security/issue-54-actualiser-glob` | `954dc1e` | [#65](https://github.com/Zoom225/GestionTransport-Multiple/pull/65) |
| #56 | `test/issue-56-validation-metier` | `c285fe2` | [#62](https://github.com/Zoom225/GestionTransport-Multiple/pull/62) |
| #57 | `fix/issue-57-validation-postgresql` | `1f78f92` | [#64](https://github.com/Zoom225/GestionTransport-Multiple/pull/64) |
| #67 | `develop` vers `master` | SHA à figer avant autorisation | [#19](https://github.com/Zoom225/GestionTransport-Multiple/pull/19) |

## 12. Résultats GitHub Actions

Les validations intégrées ont réussi pour la [consolidation](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/45464071817), le [carrousel](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/45464476557), la [nomenclature](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/45464716089), le [JWT](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/45465220509), la [migration monétaire](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/45467082023) et la [réconciliation du README](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/45470372830).

## 13. Tests backend

Le candidat de référence `d74caeb` compte 220 tests réussis, sans échec ni ignoré. La branche JWT a obtenu 224 tests réussis avec PostgreSQL isolé. Les chiffres d’un passage combiné sont relevés dans le chantier [#58](https://github.com/Zoom225/GestionTransport-Multiple/issues/58).

## 14. Tests frontend

Sur `d74caeb`, 96 tests Vitest dans 16 fichiers ont réussi en 117,28 s. Le build de production a réussi en 107,191 s. Le carrousel a aussi passé 11 tests ciblés de rotation et d’accessibilité.

## 15. Tests PostgreSQL

Dix-huit tests de référence couvrent notamment la dernière place, les paiements et les contraintes. Deux tests de mappings en mode `validate` ont réussi sur la restauration isolée. [Restauration PostgreSQL](validation-postgresql.md).

## 16. Validation des migrations

La CI contrôle le renommage du schéma, les empreintes, le rejeu et le refus d’un schéma ambigu. La migration monétaire a accepté deux exécutions successives sur fixture après avoir refusé une fraction de centime et annulé la transaction. [Migration BigDecimal](migration-montants.md).

## 17. Corrections de sécurité

Le secret JWT est chargé depuis l’environnement et validé au démarrage. La signature, l’expiration et le rôle effectif sont vérifiés. La rotation d’une clé isolée invalide les anciens jetons. La rotation des environnements réels reste à confirmer. [PR JWT](https://github.com/Zoom225/GestionTransport-Multiple/pull/46).

## 18. Corrections financières

Les soldes, prix et paiements utilisent `BigDecimal` et les cinq colonnes migrent vers `numeric(19,2)`. Le tarif par passager reste à 25,00 €, le tarif de référence et la pénalité à 100,00 €. [PR BigDecimal](https://github.com/Zoom225/GestionTransport-Multiple/pull/47).

## 19. Règles métier

La preuve par fonctionnalité couvre les fenêtres, agences, invitations, transactions, paiements, suspensions, annulations et contrôles concurrents. Les trois politiques optionnelles sont désactivées. L’association aux numéros RM01 à RM30 exige le cahier officiel. [Relevé métier](validation-metier.md).

## 20. Nettoyage du contexte historique

Sur le candidat, la recherche des variantes historiques ne renvoyait aucune correspondance de marque. Les deux propriétés du contrat de participation renvoient désormais à des trajets, et le JAR utilise le nom du projet. [PR de nomenclature](https://github.com/Zoom225/GestionTransport-Multiple/pull/41).

## 21. Exceptions conservées

Les anciens identifiants SQL nécessaires à la migration, aux empreintes, à la fixture et au garde-fou sont répertoriés. L’historique Git conserve la provenance. [Exceptions de compatibilité](exceptions-compatibilite.md).

## 22. Responsive et accessibilité

Chromium a couvert 95 affichages dont les cinq largeurs demandées, dix parcours au zoom natif 200 % et vingt analyses axe-core ciblées. Aucun débordement ni violation axe-core dans ce périmètre. Le parcours réel du profil sur base isolée a enregistré le règlement HTTP 204 et maintenu la suspension au rechargement. [Captures et validation](validation-carrousel.md).

## 23. Déploiement

Render et Vercel restent à confirmer avec les accès nécessaires. La migration de la base métier, la rotation des secrets et la publication des URLs réelles devront être vérifiées après autorisation. [Chantier de production](https://github.com/Zoom225/GestionTransport-Multiple/issues/52).

## 24. Onze acquis EPHEC

Le dossier relie chaque domaine à un extrait, un chemin, un test, une issue, une PR et un scénario. La validation officielle des intitulés et de l’équivalence Java/Spring Boot et Angular avec C# et Blazor reste à obtenir. [Dossier et diagrammes](https://github.com/Zoom225/GestionTransport-Multiple/pull/51).

## 25. Matrice de traçabilité

La [matrice](matrice-tracabilite.md) et le [journal](journal-interventions.md) distinguent les statuts réels et les preuves vérifiées.

## 26. Commit de master

Dernier SHA observé avant autorisation, `e8f139daadb7065212829b2e7072679aa9b5c485`. Sa divergence dans le README est réconciliée dans `develop`. [PR de réconciliation](https://github.com/Zoom225/GestionTransport-Multiple/pull/63).

## 27. Tag de livraison

`v1.0.0-tfe` reste à créer après fusion autorisée et contrôle de `master`.

## 28. Risques résiduels

Après mise à jour du parent vers Spring Boot 3.5.16 sur #69, le relevé OSV recense sept artefacts encore signalés. Leur applicabilité et les versions correctives compatibles sont à qualifier. Une restauration et une migration sur les données de production demandent un contrôle distinct.

## 29. Blocages

Cahier de règles et grille du professeur non reçus. L’accès Render exige de confirmer le workspace et l’accès Vercel nécessite l’autorisation de l’équipe. Les capacités disponibles ne permettent pas la création du jalon et des labels demandés. La fusion finale reste soumise à autorisation explicite.

## 30. Soutenance

Montrer successivement un parcours de réservation à 25 €, la dernière place concurrente sur PostgreSQL isolé, puis le règlement de 100 € sans levée anticipée de la suspension. Présenter le dossier des acquis avec le SHA testé et la CI. Afficher les scénarios de déploiement autorisés et vérifiés si leurs URLs sont disponibles.
