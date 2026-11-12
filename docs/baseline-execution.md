# Exécuter le socle sans modifier les données existantes

Archive du protocole de baseline. La configuration actuelle, les commandes et la
démonstration isolée sont décrites dans le [README](../README.md). L'ancien
initialiseur mentionné plus bas a été remplacé par une démonstration dev explicite
qui refuse toute base métier et toute base non vide ; il ne réécrit aucun compte.

Après renommage, consulter aussi [Exécution Transport](transport-execution.md)
pour les packages et routes actuels. Ce document conserve le protocole de baseline.

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

## Validation PostgreSQL réelle (opt-in)

Le test PostgresBaselineTest est ignoré sans POSTGRES_BASELINE_URL. Il refuse les
URL dont le nom de base ne suit pas issue1_validation_<suffixe>. Utiliser une
base neuve réservée à ces tests : ils créent le schéma avec ddl-auto=update et
ajoutent des fixtures, sans effacer ni réinitialiser la base à la fin.
Le suffixe n'est pas une preuve d'isolation : vérifier la destination avant lancement.

Exemple PowerShell, après création explicite d'une base isolée :

```powershell
$env:POSTGRES_BASELINE_URL='jdbc:postgresql://localhost:5440/issue1_validation_pg_20261111'
$env:DB_USER='transport'
$env:DB_PASSWORD='transport' # uniquement le serveur de développement Compose
mvn -B test
```

Les autres tests continuent d'utiliser H2. Le test PostgreSQL vérifie l'unicité
matricule, une clé étrangère de réservation, et deux transactions concurrentes
sur la dernière place d'un match Padel. Ces règles restent celles du socle,
aucune capacité Transport n'est introduite.

Pour le contrôle HTTP, employer une AUTRE base neuve, par exemple
issue1_validation_http_20261111. Le profil par défaut est adapté si les variables
DB_URL, DB_USER et DB_PASSWORD ciblent explicitement cette base.

```powershell
$baselineKey=New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($baselineKey)
$env:JWT_SECRET=[Convert]::ToBase64String($baselineKey)
$env:DB_URL='jdbc:postgresql://localhost:5440/issue1_validation_http_20261111'
$env:DB_USER='transport'
$env:DB_PASSWORD='transport'
$env:DEMO_DATA_ENABLED='false'
$env:SERVER_PORT='18080'
mvn -B spring-boot:run
```

Ne pas afficher JWT_SECRET ni conserver sa valeur dans Git. Le port 18080 évite
un conflit avec les applications habituelles. L'initialiseur ne doit pas créer
les données de démonstration ; vérifier sites et membres vides sur une base neuve.
Les journaux locaux sont dans target/baseline-postgres-*.log.

## Diagnostic Docker du 11 novembre 2026

Docker renvoyait "Docker Desktop is unable to start". Les journaux montrent
un échec historique de wsl.exe --version puis "checking preconditions: WSL update
required". Or wsl --version répondait 3.0.5.0 au diagnostic ; ne pas conclure
sur ce seul message qu'une mise à jour est nécessaire.

Un simple docker desktop restart a rétabli le moteur. Aucune réinitialisation,
mise à jour WSL, élévation ou modification BIOS/BCD n'a été nécessaire. Si le
blocage revient, reprendre les six commandes du diagnostic avant toute action
système. Toute procédure élevée ou tout redémarrage Windows devra être validé.
