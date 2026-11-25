# TransportMultiple

Application de gestion du covoiturage public et privé pour un réseau d’agences en Belgique. Elle gère les trajets, véhicules, voyageurs, invitations, réservations, paiements simulés et historiques. Les espaces voyageur et administration utilisent une API REST sécurisée par JWT.

## Prérequis

- Git et Windows PowerShell 5.1 ou PowerShell 7.
- JDK 21. Dans IntelliJ IDEA, configurer également le SDK du projet et le JDK du lanceur Maven sur Java 21.
- Docker Desktop démarré avec la commande `docker compose` et le support de `--wait`.
- Node.js 24 et npm. Le script `mvnw.cmd` fournit Maven, et `package-lock.json` fixe les dépendances Angular.

```powershell
git clone https://github.com/Zoom225/GestionTransport-Multiple.git
Set-Location GestionTransport-Multiple
java --version
docker compose version
node --version
npm.cmd --version
```

Utiliser la racine du dépôt pour Docker et Maven. Les commandes npm s’exécutent dans `frontend`.

## Menu de commandes

Pour afficher les commandes depuis la racine :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\commandes.ps1
```

Le menu donne accès à Docker, Maven et Angular. Pour lancer toute l’application, démarrer le backend et le frontend dans **deux terminaux**. Installer les dépendances Angular une première fois avant de lancer le frontend. Une de ces commandes peut aussi être appelée directement :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\commandes.ps1 -Action DemarrerBackend
powershell -NoProfile -ExecutionPolicy Bypass -File .\commandes.ps1 -Action DemarrerFrontend
powershell -NoProfile -ExecutionPolicy Bypass -File .\commandes.ps1 -Action TesterBackend
```

Lorsque le port 4200 est occupé, lancer le menu avec `-PortFrontend 4202` ou appeler directement `-Action DemarrerFrontend -PortFrontend 4202`.

Les autres actions sont `CompilerBackend`, `InstallerFrontend`, `CompilerFrontend`, `TesterFrontend`, `EtatDocker` et `ConstruireImage`. Les serveurs s’exécutent au premier plan dans le terminal qui les lance.


## 1. Préparer PostgreSQL

Le [fichier Compose](docker-compose.yml) démarre **PostgreSQL 15**. Il publie le port hôte **5440** vers le port **5432** du conteneur. La base locale porte le nom `transportService`. Le service Compose `postgres` utilise le conteneur `gestiontransport-db`.

### Poste déjà configuré

Conserver le volume existant. Pour retrouver son nom, lire le montage de `/var/lib/postgresql/data` dans la sortie suivante.

```powershell
docker inspect gestiontransport-db --format '{{range .Mounts}}{{println .Destination .Name}}{{end}}'
```

Renseigner `TRANSPORT_DB_VOLUME` avec ce nom exact dans le fichier local `.env`. Utiliser les identifiants du serveur existant. Les variables d’initialisation PostgreSQL s’appliquent à la création des données sur un volume neuf.

### Première installation

Créer un volume dédié, puis renseigner son nom dans `.env`. Utiliser `DB_USER` et `DB_PASSWORD` dans ce même fichier pour définir les identifiants locaux souhaités avant le premier démarrage.

```powershell
docker volume create gestiontransport-db-data
```

```dotenv
TRANSPORT_DB_VOLUME=gestiontransport-db-data
```

Le fichier `.env` est ignoré par Git. Conserver le volume lors des arrêts et éviter `docker compose down -v` sur un environnement contenant des données à conserver. Si le garde de schéma signale des tables historiques ou parallèles, utiliser une copie validée le temps de préparer la migration. [Procédure PostgreSQL](docs/finalisation/validation-postgresql.md) · [Schéma et compatibilité](docs/covoiturage/issue-3-modele-simple.md).

### Commandes Docker utiles

```powershell
docker compose config --quiet
docker compose up -d --no-recreate --wait --wait-timeout 60 postgres
docker compose ps
Test-NetConnection -ComputerName localhost -Port 5440
docker compose logs --tail=100 postgres
```

Pour arrêter uniquement PostgreSQL en conservant le stockage : `docker compose stop postgres`.

## 2. Compiler et démarrer le backend

Depuis la racine, le [lanceur Windows](start-local.ps1) attend PostgreSQL, fournit une clé JWT locale temporaire si elle manque, compile le backend et démarre Spring Boot sur le port **8080**. Garder le terminal ouvert. **Ctrl+C** arrête le backend.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\start-local.ps1
```

Pour compiler ou produire le JAR indépendamment du démarrage :

```powershell
.\mvnw.cmd -B -ntp clean compile
.\mvnw.cmd -B -ntp clean verify
```

Le JAR est dans `target/gestiontransport-multiple-0.0.1-SNAPSHOT.jar`. Les rapports Maven sont dans `target/surefire-reports/`. Arrêter le backend avant un rebuild Maven et éviter une compilation IntelliJ simultanée. Pour lancer directement via `mvnw.cmd spring-boot:run` ou IntelliJ, fournir `JWT_SECRET` et les paramètres PostgreSQL dans l’environnement du processus. Le [script IntelliJ](scripts/configure-intellij-jwt.ps1) peut préparer la clé dans la configuration locale du poste.

| Vérification | Adresse |
| --- | --- |
| Santé backend | [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) |
| Swagger UI | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |
| Document OpenAPI | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) |
| Configuration Swagger | [http://localhost:8080/v3/api-docs/swagger-config](http://localhost:8080/v3/api-docs/swagger-config) |

Dans Swagger, utiliser **Authorize** avec un JWT valide pour essayer les routes protégées. Le backend applique les droits ADMIN et VOYAGEUR.

## 3. Compiler et démarrer Angular

Dans un second terminal, depuis la racine du dépôt :

```powershell
Set-Location frontend
npm.cmd ci
npm.cmd test
npm.cmd run build
npm.cmd start
```

Ouvrir [http://localhost:4200](http://localhost:4200). Le [proxy Angular](frontend/proxy.conf.json) transmet `/api` au backend sur le port 8080. Si le port 4200 est occupé, utiliser `npm.cmd start -- --host 127.0.0.1 --port 4202` et ouvrir [http://127.0.0.1:4202](http://127.0.0.1:4202). Arrêter le serveur Angular avec **Ctrl+C** avant de relancer `npm.cmd ci` sous Windows.

Le build de production est dans `frontend/dist/frontend/browser`. Pour héberger le frontend séparément, renseigner `TRANSPORT_API_BASE_URL` avec l’URL HTTPS du backend suivie de `/api` et configurer l’origine frontend côté backend. [Guide de déploiement](docs/deploiement-production.md).

## 4. Tester sur PostgreSQL isolé et construire l’image

Depuis la racine :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-all.ps1 -NoIdeConfig
docker build --tag transportmultiple:local .
docker image inspect transportmultiple:local --format '{{.Config.User}}'
```

`mvnw.cmd clean verify` exécute les tests standards. Le [script de validation](scripts/test-all.ps1) prépare des bases PostgreSQL de test distinctes et rejoue aussi la validation du schéma. Docker Compose lance la base locale. Le [Dockerfile](Dockerfile) construit séparément l’image du backend. Les jobs GitHub Actions vérifient le backend, PostgreSQL, Angular et l’image. [Workflow](.github/workflows/validation.yml) · [Rapport de tests](docs/finalisation/rapport-tests-final.md).

## Configuration et dépannage

| Paramètre | Usage |
| --- | --- |
| `TRANSPORT_DB_VOLUME` | Nom du volume externe PostgreSQL utilisé par Compose. |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Connexion PostgreSQL du backend. Le lanceur reprend les valeurs de Compose lorsque les variables correspondantes sont absentes. |
| `JWT_SECRET` | Clé fournie par l’environnement. La clé locale temporaire du lanceur change d’un démarrage à l’autre. |
| `DEMO_DATA_ENABLED` | Désactivé par défaut. Réserver les données de démonstration à une base isolée. |
| `CORS_ALLOWED_ORIGINS`, `TRANSPORT_API_BASE_URL` | Origines frontend autorisées par le backend et adresse de l’API dans le frontend de production. |

- **`JWT_SECRET` introuvable** : utiliser le lanceur ou configurer la variable dans le terminal ou la configuration IntelliJ qui démarre réellement Spring Boot.
- **Port occupé** : repérer le processus avec `Get-NetTCPConnection -LocalPort 8080 -State Listen`. Arrêter volontairement son instance backend avant d’en lancer une autre.
- **Erreur PostgreSQL** : vérifier `docker compose ps`, le port 5440, le volume dans `.env` et les identifiants du rôle déjà présent sur le volume.
- **L’interface ne joint pas l’API** : vérifier d’abord `/actuator/health` sur le port 8080, puis `frontend/proxy.conf.json` ou les paramètres d’hébergement.

## Documentation

- [Règles de covoiturage et preuves RM01 à RM30](docs/finalisation/validation-metier.md)
- [Architecture et modèle de données](docs/covoiturage/modele-donnees.md) · [Diagrammes](docs/validation/diagrammes-transport.md)
- [Scénarios de validation et données de démonstration](docs/covoiturage/issue-7-validation.md)
- [Exécution sur une installation existante](docs/transport-execution.md) · [Validation PostgreSQL](docs/finalisation/validation-postgresql.md)
- [Déploiement de production](docs/deploiement-production.md) · [Traçabilité des interventions](docs/finalisation/matrice-tracabilite.md)
