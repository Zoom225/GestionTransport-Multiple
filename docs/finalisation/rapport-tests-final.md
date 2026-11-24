# Rapport de non-régression du candidat TFE

Contrôles du 24 novembre 2026 sur le candidat intégré `67128d5accbf04ad71ca121dadba209502bcb833`, Windows 11, Java 21.0.9, Maven 3.9.15, Node.js 24.19.0, npm 11.17.0 et PostgreSQL 15.20 isolé. La [CI du candidat](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/45467082023) a réussi sur ce SHA. Les deux tests de connexion réels sont ajoutés dans le chantier #58.

| Passage | Commande et périmètre | Réussis | Échecs | Erreurs | Ignorés | Durée |
| --- | --- | ---: | ---: | ---: | ---: | --- |
| Backend de l’audit | `scripts/test-all.ps1 -NoIdeConfig` sur `d74caeb` | 220 | 0 | 0 | 0 | 3 min 22 s |
| Préparation PostgreSQL du candidat combiné | `scripts/test-all.ps1 -NoIdeConfig`, passage `PostgresBaselineTest` | 18 | 0 | 0 | 0 | 10 min 52 s Maven |
| Suite backend avec les tests de connexion | Même script, passage `clean verify` | 209 | 0 | 1 | 0 | 9 min |
| Connexions administrateur et voyageur | `ConnexionReelleTest` dans le passage ci-dessus | 2 | 0 | 0 | 0 | 100,9 s |
| Frontend intégré | `npm ci`, puis `npm test` | 96 | 0 | 0 | 0 | 215 s |
| Build Angular intégré | `npm run build` | — | — | — | — | 87,905 s, réussi |
| Contrôle de restauration PostgreSQL | `TransportSchemaValidationTest` sur la nouvelle restauration | 2 | 0 | 0 | 0 | 5 min 30 s Maven |

Le passage local combiné a rencontré une expiration de lecture sur PostgreSQL au démarrage de `PostgresBaselineTest`. Les dix-huit tests étaient exécutés avec succès lors de la préparation précédente. Le compteur du passage interrompu contient une erreur d’initialisation et ne constitue pas un passage complet. La relance isolée et la CI de cette PR doivent confirmer la suite complète.

Les vérifications demandées portent sur la distinction 401 et 403, le JWT invalidé par rotation, les périmètres d’agence, les invitations, la dernière place, les paiements, l’expiration, les annulations, la pénalité et la suspension jusqu’à l’échéance. [Preuves métier](https://github.com/Zoom225/GestionTransport-Multiple/pull/62) · [Tests JWT](https://github.com/Zoom225/GestionTransport-Multiple/pull/46) · [Migration décimale](https://github.com/Zoom225/GestionTransport-Multiple/pull/47) · [Restauration](https://github.com/Zoom225/GestionTransport-Multiple/pull/64).

Les parcours avec Chromium ont vérifié l’affichage et la navigation à cinq largeurs et à 200 %. Le parcours réel du profil a vérifié le règlement HTTP 204 et la conservation de la suspension après rechargement. [Preuves navigateur](https://github.com/Zoom225/GestionTransport-Multiple/pull/39) · [Intégration](https://github.com/Zoom225/GestionTransport-Multiple/pull/37).
