# Matrice de traçabilité de la finalisation

État au 24 novembre 2026. Les SHA du candidat et de la branche qui porte les preuves sont distingués. Les issues restent ouvertes jusqu’aux conditions de clôture de la livraison.

| Référence | Problème | Issue | Branche et commit | PR | Tests | CI | Statut |
| --- | --- | --- | --- | --- | --- | --- | --- |
| AUD-01 | Fixer le candidat de référence | [#34](https://github.com/Zoom225/GestionTransport-Multiple/issues/34) | `docs/issue-34-audit-initial-tfe`, `b04cb4d` | [#35](https://github.com/Zoom225/GestionTransport-Multiple/pull/35) | Candidat `d74caeb`, 220 réussis backend PostgreSQL inclus et 96 réussis Angular. | [Réussie](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/45457413002) | Intégré dans develop au commit `406e233`. |
| INT-01 | Intégrer la chaîne empilée | [#36](https://github.com/Zoom225/GestionTransport-Multiple/issues/36), [#7](https://github.com/Zoom225/GestionTransport-Multiple/issues/7) | `release/issue-36-integration-transport-tfe`, `f5c9dc6` | [#37](https://github.com/Zoom225/GestionTransport-Multiple/pull/37) | Arbre identique à `d74caeb`. Suite isolée et build rejoués sur cet arbre. | [Réussie](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/45455796380) | Pull request ouverte en brouillon. |
| NOM-01 | Nom du JAR et historique SQL | [#38](https://github.com/Zoom225/GestionTransport-Multiple/issues/38) | `refactor/issue-38-nettoyage-contexte-transport`, `dfec7a2` | [#41](https://github.com/Zoom225/GestionTransport-Multiple/pull/41) | JAR vérifié, Maven 200 réussis, 20 ignorés, 0 échec. | À relever sur #41. | Pull request ouverte. |
| SEC-01 | Démarrage JWT et réponses 401/403 | [#40](https://github.com/Zoom225/GestionTransport-Multiple/issues/40) | `security/issue-40-securisation-jwt` | À publier. | En cours. | À exécuter. | En cours. |
| UI-01 | Rétablir la rotation à six secondes | [#29](https://github.com/Zoom225/GestionTransport-Multiple/issues/29) | `fix/issue-29-rotation-carrousel`, `3abe08b` | [#39](https://github.com/Zoom225/GestionTransport-Multiple/pull/39) | 11 tests ciblés réussis, build et parcours Chromium à cinq largeurs. | [Réussie pour le premier commit](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/45457260214) | Pull request ouverte. |
| UI-02 | Sanctions et règlement dans le profil | [#31](https://github.com/Zoom225/GestionTransport-Multiple/issues/31) | `frontend/angular-espace-voyageur-premium`, `d74caeb` | [#33](https://github.com/Zoom225/GestionTransport-Multiple/pull/33) | Tests Angular de profil et suite PostgreSQL. Parcours réel de livraison à vérifier. | [Réussie](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/44835490692) | Pull request ouverte. |
| DB-01 | Préserver le schéma et les données de validation | [#3](https://github.com/Zoom225/GestionTransport-Multiple/issues/3) | Candidat `d74caeb` | [#32](https://github.com/Zoom225/GestionTransport-Multiple/pull/32), [#37](https://github.com/Zoom225/GestionTransport-Multiple/pull/37) | 18 tests PostgreSQL de référence, 2 de mapping. Migration, répétabilité et refus du schéma ambigu dans la CI. | [Réussie](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/45455796380) | En validation pour la livraison. |
| TRA-01 | Centraliser les preuves | [#42](https://github.com/Zoom225/GestionTransport-Multiple/issues/42) | `docs/issue-42-tracabilite-tfe` | Suivi dans l’issue. | Liens et états relevés. | Suivi dans l’issue. | En validation. |
| LIV-01 | Livraison vers la branche par défaut | À ouvrir à la livraison. | `develop` vers `master` | [#19](https://github.com/Zoom225/GestionTransport-Multiple/pull/19) | La PR existante précède les quatre PR empilées. | [Réussie sur le candidat précédent](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/43280475952) | À réaliser. |

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
