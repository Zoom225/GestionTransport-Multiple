# Exécuter le socle sans modifier les données existantes

Java 21, Maven et les versions de dépendances du dépôt sont conservés.

## Backend et données

`mvn -B test` utilise exclusivement H2 en mémoire via `src/test/resources/application.properties`.
Ces tests ne valident pas les particularités PostgreSQL ni la concurrence réelle.

Pour démarrer avec PostgreSQL, fournir `JWT_SECRET` (valeur aléatoire d'au moins
32 octets), puis `DB_URL`, `DB_USER`, `DB_PASSWORD` selon l'environnement.
La clé JWT précédemment versionnée doit être considérée compromise et remplacée
dans tout environnement qui l'utilisait. Ne jamais la réutiliser.

`mvn spring-boot:run` conserve `ddl-auto=update` : sauvegarder la base avant tout
démarrage sur une base existante. Ne pas supprimer le volume `padel-db-data`.
Le Compose existant expose PostgreSQL 15 sur le port 5440 avec des identifiants
de développement publics ; il ne constitue pas une configuration de production.

L'initialiseur de démonstration est désormais désactivé par défaut. Activer
`DEMO_DATA_ENABLED=true` uniquement sur une base jetable : il crée des comptes
connus et peut réécrire des identifiants de membres existants.

Swagger est configuré sur `/swagger-ui.html` et `/v3/api-docs`.
Son fonctionnement HTTP doit encore être vérifié sur un démarrage PostgreSQL.

## Frontend

Depuis `frontend/`, installer avec `npm ci`, puis exécuter `npm run build` et
`npm test`. Le verrou npm doit rester inchangé. Le script de test utilise Vitest.
Lancer le frontend avec `npm start` utilise le proxy REST du dépôt.

## Validation de phase

Conserver la PR #8 en brouillon tant que les contrôles requis ne sont pas validés.
Ne pas démarrer les phases dépendantes avant validation de cette baseline.
