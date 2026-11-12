# GestionTransport-Multiple

Application Spring Boot et Angular en cours de transformation vers le transport public et privé.

## Tarif actuel — décision métier (issue #4)

Le montant d'un nouveau trajet est de **100 €**, soit **25 € par personne pour
quatre places**. Le backend applique ce tarif à la création du trajet et du
paiement associé. Les nouvelles données de démonstration utilisent le même tarif.
Les trajets et paiements déjà enregistrés conservent leurs montants historiques.

Cette décision est définitive : un chauffeur déjà associé, quatre clients maximum,
25 € par client, 100 € si complet. La capacité variable est abandonnée.

L'[étape modèle de l'issue #3](docs/covoiturage/issue-3-modele-simple.md) prépare
le chauffeur, les villes et les catégories VIP/local/libre. Les règles de
confirmation au paiement et l'interface finale restent en cours de réalisation.

Le guide actuel est [Exécution Transport](docs/transport-execution.md), avec les
nouveaux packages, routes et contrats. Le périmètre et les exceptions de
compatibilité sont décrits dans [Nomenclature Transport](docs/nomenclature-transport.md).

Les sections ci-dessous sont conservées comme documentation historique du socle.
Le volume existant doit être préservé ; le nom de base et le compte sont migrés
explicitement selon la procédure ci-dessous. Les
anciennes routes et les anciens noms de classes ne décrivent plus le code courant.

- Backend : Spring Boot 3.5, Java 21, Maven
- Frontend : Angular 21, Angular Material, Tailwind, Vitest
- Base de donnees : PostgreSQL 15 avec Docker Compose

## Guide principal — préparation du covoiturage belge (issue #2)

GestionTransport-Multiple évolue vers une application de covoiturage entre villes
belges. La cible comprend recherche départ/destination/date, conducteur Voyageur,
un chauffeur et quatre places clients maximum, réservations confirmées au paiement, restrictions
et administration. **Ces fonctions ne sont pas encore toutes implémentées.**

L'énoncé PDF de cinq pages et le code existant ont été analysés. Consulter :

- [Analyse métier, existant/manquant et règles](docs/covoiturage/analyse-metier.md).
- [Classes, modèle relationnel et migration protégée](docs/covoiturage/modele-donnees.md).
- [Issues, branches, PR, tests et critères de livraison](docs/covoiturage/plan-livraison.md).

Le tarif actuel demandé est **100 € par trajet, soit 25 € par personne pour quatre
places**, traité dans la PR #10 / issue #4. Les anciens montants restent conservés.
Cette règle est définitive : le chauffeur ne compte pas parmi les quatre clients.
Les recettes sont de 25, 50, 75 ou 100 € pour un à quatre clients payants.

### Versions et architecture

Java 21, Spring Boot 3.5, Maven, PostgreSQL 15, Angular 21, Angular Material et
Tailwind sont conservés. Le flux reste Controller → Service → Repository → base,
avec DTO, Mapper, validation, JWT et gestion centralisée des erreurs. Aucun
microservice, Google Maps, Redis, Kafka ou paiement bancaire réel n'est prévu.

Les PR #8 et #9 portent la baseline et la nomenclature Transport ; elles ne sont
pas encore fusionnées au moment de cette analyse. Les commandes suivantes
décrivent ce socle après leur intégration. develop ne constitue pas encore la
plateforme de covoiturage finale. Vérifier sa branche avant toute démonstration.

### 1. Vérifier les outils et préserver le travail local

```powershell
git status
git diff -- pom.xml
git fetch origin
java -version
mvn --version
node --version
npm --version
docker version
docker compose version
```

Le pom.xml local utilise actuellement l'artifactId `industri` ; son diff doit
rester préservé. Ne pas supposer le nom du JAR ou effacer les modifications pour
changer de branche. Les branches suivent les issues et les PR ciblent develop.

### 2. Docker et PostgreSQL

```powershell
docker compose config
docker compose pull
docker compose up -d
docker compose ps
docker compose logs --tail=100 postgres
```

Le Compose actuel utilise l'image officielle PostgreSQL 15, le port hôte 5440 et
un volume persistant. Il ne contient pas encore de Dockerfile backend/frontend :
`docker compose build` ne compile donc pas ces deux applications. Leurs builds
sont Maven et Angular, ci-dessous ; leur conteneurisation appartient à l'issue #7.

Conserver le volume existant ; migrer explicitement le compte et la base vers
transport et transportService sans supprimer les données. Ne jamais
utiliser `docker compose down -v` pour dépanner. Les identifiants `transport/transport`
présents dans le Compose sont exclusivement ceux du développement local.

Pour une démonstration potentiellement modificatrice, créer une base distincte
(commande à lancer une seule fois ; si elle existe, vérifier son contenu) :

```powershell
docker compose exec postgres createdb -U transport covoiturage_demo
```

### 3. Configurer et démarrer le backend

```powershell
$env:DB_URL='jdbc:postgresql://localhost:5440/covoiturage_demo'
$env:DB_USER='transport'
$env:DB_PASSWORD='transport'
$jwtBytes=New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($jwtBytes)
$env:JWT_SECRET=[Convert]::ToBase64String($jwtBytes)
$env:DEMO_DATA_ENABLED='false'
mvn -B spring-boot:run
```

Ne pas afficher la clé JWT, publier des secrets ou réutiliser l'ancienne clé
versionnée. Ces variables sont locales au terminal. En dehors du développement,
utiliser des identifiants propres à l'environnement et un compte DB limité.

`ddl-auto=update` peut modifier le schéma : ne pas lancer cette commande sur la
base métier sans sauvegarde et validation. L'initialiseur de démonstration reste
désactivé par défaut, car sa version héritée peut modifier les comptes existants.
Il sera adapté aux exemples belges uniquement dans un environnement de développement.

Backend : http://localhost:8080 ; Swagger : http://localhost:8080/swagger-ui.html ;
OpenAPI : http://localhost:8080/v3/api-docs. Après nomenclature #9, les ressources
principales sont `/api/agences`, `/api/vehicules`, `/api/trajets`, `/api/voyageurs`,
`/api/reservations`, `/api/paiements`, `/api/indisponibilites-service`.

### 4. Compiler et tester le backend

```powershell
mvn -B test
mvn -B -DskipTests package
```

Après #8, la suite standard utilise H2 en mémoire. Pour les tests PostgreSQL
opt-in, utiliser exclusivement une base isolée nommée `issue1_validation_<suffixe>` :

```powershell
docker compose exec postgres createdb -U transport issue1_validation_academique
$env:POSTGRES_BASELINE_URL='jdbc:postgresql://localhost:5440/issue1_validation_academique'
$env:DB_USER='transport'
$env:DB_PASSWORD='transport'
mvn -B test
Remove-Item Env:POSTGRES_BASELINE_URL
```

Éviter de compiler simultanément avec l'IDE et Maven dans les mêmes répertoires
de sortie. Ne pas considérer les tests H2 comme preuve du comportement PostgreSQL.

### 5. Installer, compiler, tester et démarrer Angular

Dans un autre terminal, à partir de la racine du dépôt :

```powershell
cd frontend
npm ci
npm run build
npm test
npm start
```

Ouvrir http://localhost:4200. Le proxy existant cible le backend sur 8080 ; les
contrats frontend/backend doivent être livrés ensemble. Réutiliser l'intercepteur
JWT, les sessions et les guards ; se reconnecter après une modification de contrat.

### 6. Présentation et validation

La démonstration finale devra montrer recherche réelle, conducteur automatique,
paiement simulé identifié, confirmation, annulation, restriction et statistiques.
Les villes populaires servent de raccourcis de recherche, jamais de fausses
places disponibles. Vérifier mobile 375 px, tablette 768 px, ordinateur 1440 px,
clavier/focus et absence de débordement horizontal.

Résultats vérifiés de la branche tarif `3758998` : 112 tests Java avec PostgreSQL
isolé et 58 tests Angular réussis, build Angular réussi. Ces résultats ne sont
pas ceux du modèle cible, encore documentaire, ni ceux de develop avant fusion.
L'analyse #2 ne modifie aucune classe, dépendance, entité ni donnée PostgreSQL.

### Documentation historique conservée

Les sections ci-dessous décrivent le socle antérieur. Le guide ci-dessus et les
documents de l'issue #2 indiquent les prérequis et la cible de la refonte.

## Prerequis

Installer sur la machine :

- Java 21
- Node.js compatible avec Angular 21
- npm
- Docker Desktop

Verifier les versions :

```bash
java -version
node -v
npm -v
docker --version
```

## Structure du projet

```text
padell/
  src/                 Backend Spring Boot
  frontend/            Frontend Angular
  docker-compose.yml   PostgreSQL Docker
  pom.xml              Configuration Maven backend
  README.md            Documentation compilation et lancement
```

## Base de donnees PostgreSQL

Le projet utilise PostgreSQL via Docker.

Configuration :

```text
Base       : transportService
Utilisateur: transport
Mot de passe: transport
Port local : 5440
Port Docker: 5432
```

Ces identifiants sont ceux du développement local. DB_USER et DB_PASSWORD
permettent de fournir d'autres valeurs. Pour un volume déjà initialisé, modifier
le Compose ne change ni le compte ni le nom de base PostgreSQL : utiliser la
[migration des identifiants de développement](docs/identifiants-postgresql.md).

Demarrer PostgreSQL :

```bash
docker compose up -d
```

Verifier que le conteneur tourne :

```bash
docker ps
```

Arreter PostgreSQL :

```bash
docker compose down
```

Conserver le volume PostgreSQL : ne pas utiliser l'option `-v` de `down` pour
changer les identifiants ou dépanner un démarrage.

## Backend Spring Boot

Le backend se trouve a la racine du projet.

Port backend :

```text
http://localhost:8080
```

Configuration principale :

```text
src/main/resources/application.properties
```

Connexion a la base :

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5440/transportService}
spring.datasource.username=${DB_USER:transport}
spring.datasource.password=${DB_PASSWORD:transport}
server.port=8080
```

### Installer/compiler le backend

Depuis la racine du projet :

```bash
.\mvnw.cmd clean compile
```

### Lancer les tests backend

```bash
.\mvnw.cmd test
```

### Builder le backend

```bash
.\mvnw.cmd clean package
```

Le fichier genere se trouve dans :

```text
target/
```

### Lancer le backend

Avec Maven :

```bash
.\mvnw.cmd spring-boot:run
```

Ou apres un package :

```bash
java -jar target/padelmultiple-0.0.1-SNAPSHOT.jar
```

## Swagger API

Quand le backend tourne :

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON :

```text
http://localhost:8080/v3/api-docs
```

## Frontend Angular

Le frontend se trouve dans :

```text
frontend/
```

Le proxy Angular redirige les appels `/api` vers le backend :

```text
frontend/proxy.conf.json
```

Configuration proxy :

```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true
  }
}
```

### Installer les dependances frontend

Depuis le dossier `frontend` :

```bash
cd frontend
npm install
```

### Lancer le frontend en developpement

```bash
npm start
```

Ou explicitement sur le port 4200 :

```bash
npm run start:4200
```

URL frontend :

```text
http://localhost:4200
```

### Compiler le frontend

```bash
npm run build
```

Le build Angular est genere dans :

```text
frontend/dist/
```

### Lancer les tests frontend

```bash
npm test
```

Mode watch :

```bash
npm run test:watch
```

## Compilation complete du projet

Depuis la racine :

```bash
docker compose up -d
.\mvnw.cmd clean test
cd frontend
npm install
npm test
npm run build
```

## Lancement complet en local

Terminal 1 : base de donnees

```bash
docker compose up -d
```

Terminal 2 : backend

```bash
.\mvnw.cmd spring-boot:run
```

Terminal 3 : frontend

```bash
cd frontend
npm start
```

URLs :

```text
Frontend : http://localhost:4200
Backend  : http://localhost:8080
Swagger  : http://localhost:8080/swagger-ui.html
Postgres : localhost:5440
```

## Commandes utiles

Voir les logs PostgreSQL :

```bash
docker logs padel-db
```

Relancer proprement la base :

```bash
docker compose down
docker compose up -d
```

Nettoyer le build backend :

```bash
.\mvnw.cmd clean
```

Nettoyer le build frontend :

```bash
cd frontend
Remove-Item -Recurse -Force dist
```

## Ordre conseille pour une demo

1. Demarrer PostgreSQL :

```bash
docker compose up -d
```

2. Lancer le backend :

```bash
.\mvnw.cmd spring-boot:run
```

3. Lancer le frontend :

```bash
cd frontend
npm start
```

4. Ouvrir :

```text
http://localhost:4200
```

5. Verifier l'API avec Swagger :

```text
http://localhost:8080/swagger-ui.html
```
