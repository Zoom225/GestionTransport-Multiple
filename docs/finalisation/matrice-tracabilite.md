# Matrice de traçabilité de la finalisation

État au 24 novembre 2026. Les SHA du candidat et de la branche qui porte les preuves sont distingués. Les issues restent ouvertes jusqu’aux conditions de clôture de la livraison.

| Référence | Problème | Issue | Branche et commit | PR | Tests | CI | Statut |
| --- | --- | --- | --- | --- | --- | --- | --- |
| AUD-01 | Fixer le candidat de référence | [#34](https://github.com/Zoom225/GestionTransport-Multiple/issues/34) | `docs/issue-34-audit-initial-tfe`, `b04cb4d` | [#35](https://github.com/Zoom225/GestionTransport-Multiple/pull/35) | Candidat `d74caeb`, 220 réussis backend PostgreSQL inclus et 96 réussis Angular. | [Réussie](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/45457413002) | Intégré dans develop au commit `406e233`. |
| INT-01 | Intégration de la chaîne empilée | [#36](https://github.com/Zoom225/GestionTransport-Multiple/issues/36) | `release/issue-36-integration-transport-tfe`, `f5c9dc6` | [#37](https://github.com/Zoom225/GestionTransport-Multiple/pull/37) | Arbre identique à `d74caeb`, parcours réel profil et règlement HTTP 204. | [Réussie](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/45464071817) | Intégré dans develop au commit `a39d5d4`. |
| NOM-01 | Nom du JAR et droits de participation | [#38](https://github.com/Zoom225/GestionTransport-Multiple/issues/38) | `refactor/issue-38-nettoyage-contexte-transport`, `dfec7a2`, `83fa93e` | [#41](https://github.com/Zoom225/GestionTransport-Multiple/pull/41) | JAR vérifié, 19 tests MockMvc ciblés et 96 Angular réussis. | [Réussie](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/45464716089) | Intégré dans develop au commit `c14cc0a`. |
| SEC-01 | Démarrage JWT et réponses 401/403 | [#40](https://github.com/Zoom225/GestionTransport-Multiple/issues/40) | `security/issue-40-securisation-jwt`, `131fd7f` | [#46](https://github.com/Zoom225/GestionTransport-Multiple/pull/46) | 224 réussis, 0 échec et 0 ignoré avec PostgreSQL isolé. | [Réussie](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/45465220509) | Intégré dans develop au commit `e43e878`. Rotation réelle à confirmer. |
| UI-01 | Rotation à six secondes | [#29](https://github.com/Zoom225/GestionTransport-Multiple/issues/29) | `fix/issue-29-rotation-carrousel`, `3abe08b`, `80e8006` | [#39](https://github.com/Zoom225/GestionTransport-Multiple/pull/39) | 11 tests ciblés, 95 affichages Chromium, zoom 200 % et captures. | [Réussie](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/45464476557) | Intégré dans develop au commit `0160cbf`. |
| UI-02 | Sanctions et règlement dans le profil | [#31](https://github.com/Zoom225/GestionTransport-Multiple/issues/31) | `frontend/angular-espace-voyageur-premium`, `d74caeb` | [#33](https://github.com/Zoom225/GestionTransport-Multiple/pull/33) | Suite Angular et parcours réel sur données isolées. | [Réussie](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/44835490692) | Intégré dans develop via #37. |
| DB-01 | Préserver le schéma et les données de validation | [#3](https://github.com/Zoom225/GestionTransport-Multiple/issues/3) | Candidat `d74caeb` | [#32](https://github.com/Zoom225/GestionTransport-Multiple/pull/32), [#37](https://github.com/Zoom225/GestionTransport-Multiple/pull/37) | 18 tests PostgreSQL de référence, 2 de mapping. Migration, répétabilité et refus du schéma ambigu dans la CI. | [Réussie](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/45455796380) | En validation pour la livraison. |
| TRA-01 | Centraliser les preuves | [#42](https://github.com/Zoom225/GestionTransport-Multiple/issues/42) | `docs/issue-42-tracabilite-tfe` | Suivi dans l’issue. | Liens et états relevés. | Suivi dans l’issue. | En validation. |
| LIV-01 | Livraison vers la branche par défaut | [#44](https://github.com/Zoom225/GestionTransport-Multiple/issues/44) | `develop` vers `master` | [#19](https://github.com/Zoom225/GestionTransport-Multiple/pull/19) | Conflit de README dû au commit `e8f139d` constaté. | Contrôle final à effectuer. | En validation. |
| FIN-01 | Préserver les montants en BigDecimal | [#45](https://github.com/Zoom225/GestionTransport-Multiple/issues/45) | `refactor/issue-45-montants-bigdecimal`, `4f92a87`, `08eafe9` | [#47](https://github.com/Zoom225/GestionTransport-Multiple/pull/47) | 221 tests isolés avant intégration et migration SQL vérifiée sur fixture. | [Réussie](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/45467082023) | Intégré dans develop au commit `67128d5`. |
| GIT-01 | Modèles GitHub en français | [#48](https://github.com/Zoom225/GestionTransport-Multiple/issues/48) | `chore/issue-48-modeles-github`, `393d8c3` | [#49](https://github.com/Zoom225/GestionTransport-Multiple/pull/49) | Quatre formulaires YAML et dix items de validation vérifiés. | [Réussie](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/45463922839) | Intégré dans develop au commit `a41428b`. |
| AA-01 | Dossier des onze acquis et diagrammes | [#50](https://github.com/Zoom225/GestionTransport-Multiple/issues/50) | `docs/issue-50-validation-onze-acquis-tfe`, `e91906e` | [#51](https://github.com/Zoom225/GestionTransport-Multiple/pull/51) | Onze extraits et chemins vérifiés. | [Réussie](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/45465236562) | Pull request ouverte, grille officielle attendue. |
| MET-01 | Relevé des preuves métier | [#56](https://github.com/Zoom225/GestionTransport-Multiple/issues/56) | `test/issue-56-validation-metier`, `c285fe2` | [#62](https://github.com/Zoom225/GestionTransport-Multiple/pull/62) | Contrôles sur le candidat, décisions optionnelles [#59](https://github.com/Zoom225/GestionTransport-Multiple/issues/59), [#60](https://github.com/Zoom225/GestionTransport-Multiple/issues/60) et [#61](https://github.com/Zoom225/GestionTransport-Multiple/issues/61). | [CI du candidat](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/45467082023) | Pull request ouverte. |
| INF-01 | Préparation Render et Vercel | [#52](https://github.com/Zoom225/GestionTransport-Multiple/issues/52) | `chore/issue-52-deploiement-production` | À publier. | Tests de santé et CORS, build Angular. | À relever. | En cours, accès fournisseurs à confirmer. |
| DEP-01 | Avis de dépendances | [#53](https://github.com/Zoom225/GestionTransport-Multiple/issues/53), [#54](https://github.com/Zoom225/GestionTransport-Multiple/issues/54), [#55](https://github.com/Zoom225/GestionTransport-Multiple/issues/55) | Chantiers indépendants. | À publier. | Relevés OSV et npm dans l’audit. | À relever. | En validation. |
| QA-01 | Rejeu combiné de non-régression | [#58](https://github.com/Zoom225/GestionTransport-Multiple/issues/58) | `test/issue-58-validation-non-regression` | À publier. | Candidat intégré `67128d5` et contrôles de connexion. | [CI du candidat](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/45467082023) | En cours. |

## Règles métier

Les références RM01 à RM30 sont réservées aux identifiants du cahier des charges officiel. La preuve technique est stockée par fonctionnalité en attendant la correspondance officielle. Le rapport d’audit décrit les limites.

| Fonctionnalité | Code et preuve du candidat | Correspondance RM |
| --- | --- | --- |
| Accès et périmètre | `SecurityConfig`, `AdminAuthorizationService`, `AdminAuthorizationServiceTest`. | À confirmer. |
| Réservations et paiements | `ReservationServiceImpl`, `PaiementServiceImpl`, `PostgresBaselineTest`. | À confirmer. |
| Règlement et durée de suspension | `VoyageurEligibilityService`, `SuspensionParticipationTest`, `issue-9-suspension-168h.sql`. | À confirmer. |
| Affichage des sanctions | `voyageur-profile-page.component.ts` et `voyageur-profile-page.component.spec.ts`. | À confirmer. |
| Migrations du schéma | `scripts/migrations/issue-3-schema-transport.sql`, `TransportSchemaValidationTest`. | À confirmer. |

## Acquis d’apprentissage

| Référence | Domaine de la mission | Preuve à relier au dossier EPHEC |
| --- | --- | --- |
| AA01 | Analyse des besoins | `docs/covoiturage/analyse-metier.md`. |
| AA02 | Cahier des charges | Référentiel officiel à obtenir. |
| AA03 | Modélisation | `docs/covoiturage/modele-donnees.md`. |
| AA04 | Intégrité des données | DB-01. |
| AA05 | Architecture trois couches | Répertoires `controller`, `service` et `repository`. |
| AA06 | Authentification et autorisation | SEC-01. |
| AA07 | API REST sécurisée | Contrôleurs de réservation, paiement et voyageurs. |
| AA08 | Interface | UI-01 et UI-02. |
| AA09 | Métier, transactions et erreurs | `ReservationServiceImpl` et `SuspensionParticipationTest`. |
| AA10 | Tests, GitHub, CI et déploiement | AUD-01, INT-01 et DB-01. Déploiement à confirmer. |
| AA11 | Documentation et démonstration | AUD-01, TRA-01 et rapports de preuve des PR. |

Pour actualiser la matrice, consigner le résultat d’abord dans l’issue et dans la PR du chantier. Publier ensuite une mise à jour groupée de la matrice et du journal depuis `develop`, après vérification des SHA et des statuts de CI.
