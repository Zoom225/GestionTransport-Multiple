# Audit initial — GestionTransport-Multiple

Issue : #1

## État de départ

- Dépôt GitHub : `Zoom225/GestionTransport-Multiple`
- Branche stable : `master`
- Branche d'intégration : `develop`
- Socle hérité : projet Padel, à transformer progressivement en plateforme de transport public/privé.
- Modification locale signalée : `pom.xml` modifié et non commité (ne pas écraser sans examen).
- Les builds et les tests n'ont pas encore été exécutés sur l'environnement local dans cette phase.

## Architecture à préserver

- Backend Spring Boot / Maven / Java 21, API REST, séparation Controller-Service-Repository, DTO et sécurité.
- Frontend Angular dans `frontend/`, services API, routage, écrans utilisateur et gestionnaire.
- Base relationnelle PostgreSQL ; configuration Docker dans `docker-compose.yml`.
- Tests backend et frontend présents dans le socle.

## Principes de transformation

1. Conserver une baseline exécutable et enregistrer les résultats des tests.
2. Introduire le vocabulaire métier de transport sans simple substitution textuelle.
3. Construire les concepts Agence, Véhicule, Conducteur, Trajet, Voyageur, Réservation, Paiement selon les règles documentées dans l'issue #2.
4. Refactoriser progressivement modèle/repository, service, API/DTO/mappers, sécurité, puis Angular.
5. Préserver les données en définissant une stratégie explicite de migration ; ne jamais supprimer des tables/colonnes sans accord et sauvegarde.
6. Contrôler la capacité des véhicules dans une transaction pour éviter les doubles réservations.
7. Un commit et une PR traçables par phase ; pas de fusion si compilation/tests échouent.

## Vérifications de démarrage à faire localement (PowerShell)

```powershell
git status
git diff -- pom.xml
java -version
.\mvnw.cmd test
cd frontend
npm ci
npm run build
npm test -- --run
```

Adapter la dernière commande au script de test effectivement présent dans `frontend/package.json`. Les tests nécessitant PostgreSQL ou Docker doivent être exécutés avec les services configurés.

## Contrôle de sécurité avant toute publication

- Vérifier `.env`, identifiants DB, clés JWT, tokens et fichiers contenant des secrets.
- Examiner les fichiers de logs `backend*.log` et les retirer du suivi si nécessaire, sans effacer les traces locales utiles.
- Ne jamais publier un secret ; s'il a déjà été publié, effectuer une rotation.

## Critères de clôture #1

- [ ] Builds backend et frontend exécutés et résultats consignés
- [ ] Tests initiaux exécutés et résultats consignés
- [ ] Configuration de démarrage vérifiée
- [ ] Secrets/logs inspectés
- [ ] Modifications locales de `pom.xml` intégrées ou écartées volontairement
- [x] Branches `develop` et `chore/issue-1-audit-baseline` créées
