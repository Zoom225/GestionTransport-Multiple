# Audit initial de la finalisation du TFE

Audit du 24 novembre 2026 pour [l’issue #34](https://github.com/Zoom225/GestionTransport-Multiple/issues/34). Le rapport est proposé sur une branche issue de `develop`. Le code testé est celui du dernier candidat fonctionnel de `origin`, dans un dossier de travail séparé.

## Références Git

| Référence au relevé | Commit | Écart |
| --- | --- | --- |
| `origin/master` | `688190e4841ee0064fbfbb4d957dd3f0b50526df` | Branche GitHub par défaut |
| `origin/develop` | `b8183927149617e4cd6d5e00b8ea713e931fc654` | 32 commits d’avance sur `master`, 0 en retard |
| `origin/frontend/angular-espace-voyageur-premium` | `d74caeb117a4b4dd3287a8efd1d5edb71839c6ce` | 36 commits d’avance sur `develop`, 0 en retard |

`git fetch origin --prune` a réussi. Le dépôt officiel correspond à `origin`. Le dossier de travail principal était déjà sur la branche de la PR #33 et contenait deux fichiers non suivis préservés. L’audit historique [#1](https://github.com/Zoom225/GestionTransport-Multiple/issues/1) couvrait le socle préconversion. [#7](https://github.com/Zoom225/GestionTransport-Multiple/issues/7) porte sur l’intégration et la démonstration.

## Pull requests et validations existantes

| PR | Branche cible au relevé | SHA source | État | CI vérifiée |
| --- | --- | --- | --- | --- |
| [#19](https://github.com/Zoom225/GestionTransport-Multiple/pull/19) | `master` | `b818392` | Ouverte | [43280475952](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/43280475952) |
| [#28](https://github.com/Zoom225/GestionTransport-Multiple/pull/28) | `develop` | `657cd8a` | Ouverte | [43602257921](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/43602257921) |
| [#30](https://github.com/Zoom225/GestionTransport-Multiple/pull/30) | `feature/issue-6-bannieres-agences-transport` | `0372393` | Brouillon ouvert | [43658352027](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/43658352027) |
| [#32](https://github.com/Zoom225/GestionTransport-Multiple/pull/32) | `feature/frontend-premium-car-carousel` | `83af866` | Ouverte | [44835487740](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/44835487740) |
| [#33](https://github.com/Zoom225/GestionTransport-Multiple/pull/33) | `fix/covoiturage-sanctions-et-validation` | `d74caeb` | Ouverte | [44835490692](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/44835490692) |

Les cinq exécutions référencées ont des tâches backend et frontend réussies. Pour #28, #30, #32 et #33, la CI confirme également l’étape dédiée aux migrations et aux mappings PostgreSQL. L’exécution en échec [44834979414](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/44834979414) correspond à un SHA antérieur de la PR #32. La PR #19 valide le candidat précédent, actuellement au sommet de `develop`.

Les simulations `git merge-tree --write-tree --name-only` ont réussi pour les quatre couples source/cible empilés et pour `master` avec le candidat final. Les branches des PR #32 et #33 divergent après `e81284b`. Les commits `83af866` et `d74caeb` appliquent un correctif CI de contenu identique. Une intégration doit conserver l’ascendance utile des deux branches et déclencher une CI sur son propre résultat. Les conditions encore ouvertes du brouillon #30 et de l’issue #29 doivent être vérifiées avant de déclarer le chantier livré.

Au moment de l’audit, les issues [#1](https://github.com/Zoom225/GestionTransport-Multiple/issues/1), [#3](https://github.com/Zoom225/GestionTransport-Multiple/issues/3), [#4](https://github.com/Zoom225/GestionTransport-Multiple/issues/4), [#6](https://github.com/Zoom225/GestionTransport-Multiple/issues/6), [#7](https://github.com/Zoom225/GestionTransport-Multiple/issues/7), [#29](https://github.com/Zoom225/GestionTransport-Multiple/issues/29), [#31](https://github.com/Zoom225/GestionTransport-Multiple/issues/31) et [#34](https://github.com/Zoom225/GestionTransport-Multiple/issues/34) sont ouvertes. Les issues [#2](https://github.com/Zoom225/GestionTransport-Multiple/issues/2) et [#5](https://github.com/Zoom225/GestionTransport-Multiple/issues/5) sont closes.

## Constats sur le candidat

- **Nomenclature.** Une recherche insensible à la casse sur les variantes du nom historique trouve 126 fichiers versionnés sur `master`, 10 sur `develop` et 0 sur `d74caeb`. Le contrôle complémentaire devra vérifier les termes métier en contexte, les données d’affichage et les exceptions SQL. L’artefact Maven reste nommé `industri`. Les corrections lexicales déjà présentes sur le candidat peuvent être reprises par intégration.
- **JWT.** Le candidat lit `JWT_SECRET` depuis l’environnement. `JwtConfig` construit la clé lors de l’usage et s’appuie sur JJWT pour vérifier les jetons signés. Une valeur JWT directe existe dans l’ancienne configuration versionnée de `master`. Son utilisation effective en production reste inconnue. La validation de la qualité de la clé au démarrage, les scénarios 401/403 et la rotation doivent être vérifiés. Aucune valeur de clé n’a été relevée dans ce rapport.
- **Montants.** `Voyageur.solde`, `Paiement.montant`, `Vehicule.prix`, `Trajet.prixTotal` et `Trajet.prixParPassager` sont déclarés en `Double`. `Restriction.montantPenalite` utilise déjà `BigDecimal`. La reprise doit couvrir ensemble la persistance, les calculs et les contrats REST.
- **PostgreSQL.** Le candidat conserve `ddl-auto=update` dans la configuration principale. Le workflow `.github/workflows/validation.yml` utilise PostgreSQL 15, un schéma isolé, une comparaison d’empreintes, le rejeu de la migration de nomenclature, son refus en présence de tables concurrentes et un contrôle Hibernate des mappings.
- **Réservation et sanctions.** Le candidat comporte des transactions et des verrous pessimistes sur des opérations de réservation et de trajet. Les tests de l’API et d’Angular existent. Un relevé par règle et un contrôle des paiements et réservations simultanés restent nécessaires pour la livraison.
- **Interface.** L’issue [#29](https://github.com/Zoom225/GestionTransport-Multiple/issues/29) fixe une rotation de six secondes. `car-hero-carousel.component.ts` et son test fixent actuellement 3 500 ms sur le candidat, qui gère aussi `prefers-reduced-motion`. L’issue #29 couvre déjà ce point. Les contrôles navigateur aux largeurs et au zoom demandés restent à consolider.
- **Documentation et déploiement.** Le dépôt contient des documents d’analyse et de validation dans `docs/covoiturage/`. Le cahier officiel numéroté RM01–RM30 et les intitulés officiels des acquis EPHEC restent à retrouver ou à confirmer. L’état d’un déploiement actif n’a pas été établi.

## Contrôles locaux

Windows 11, Java 21.0.9, Maven 3.9.15, Node 24.19.0 et npm 11.17.0. Code du candidat `d74caeb`.

| Contrôle | Résultat |
| --- | --- |
| `mvn -B -ntp clean verify` | Compilation et empaquetage réussis en 3 min 42 s. 200 tests réussis, 0 échec, 20 ignorés. Les bases PostgreSQL conditionnelles étaient désactivées pour ce premier passage. |
| `npm ci` | Installation réussie. |
| `npm test` | 96 tests réussis dans 16 fichiers, 0 échec, 0 ignoré, en 117,28 s. |
| `npm run build` | Build Angular de production réussi en 107,191 s. |
| `scripts/test-all.ps1 -NoIdeConfig` | Préparation sur PostgreSQL 15 et contrôle de 18 tests en 44,633 s, puis suite complète de 220 tests réussis, 0 échec et 0 ignoré en 3 min 22 s. La suite inclut 18 tests de référence PostgreSQL et 2 tests de validation du schéma restauré. |

`npm audit --package-lock-only --json` relève 16 entrées de sévérité modérée sur le verrou, issues de deux avis sous-jacents concernant [glob](https://github.com/advisories/GHSA-3g3m-2fjr-6pw4) et [minimatch](https://github.com/advisories/GHSA-g3hv-75wg-m249). Ce total inclut les dépendances signalées par propagation.

Le relevé `mvn dependency:list -DincludeScope=runtime` a permis de comparer 88 coordonnées à la base OSV. Des avis correspondent à 17 artefacts, dont Spring Boot, Spring Framework, Spring Security, Spring Data, Tomcat embarqué, Logback, Jackson et le pilote PostgreSQL. Ces correspondances de versions doivent être rapprochées des conditions de chaque avis avant remédiation. L’API publique des alertes Dependabot requiert une authentification.

## Suite de la livraison

Le prochain chantier est la validation de la chaîne #28 → #30 → #32 → #33 vers `develop`. Une [branche de consolidation](https://github.com/Zoom225/GestionTransport-Multiple/pull/37) prévoit une validation de l’arbre intégré. Les validations de sécurité, de précision financière, de PostgreSQL et de conformité métier doivent être liées à leurs chantiers respectifs. La livraison vers `master`, le tag et les preuves de déploiement restent soumis aux validations et autorisations prévues pour le TFE.
