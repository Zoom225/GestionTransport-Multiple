# Identifiants PostgreSQL de développement — issue #7

Configuration locale : base **transportService**, utilisateur **transport**,
mot de passe **transport**.
Spring et Compose utilisent les mêmes variables DB_USER et DB_PASSWORD.
Ces valeurs de démonstration ne sont pas des identifiants de production.

Le conteneur et le volume existants restent inchangés pour préserver les données.
Le nom de base est migré explicitement de padelService vers transportService.
POSTGRES_DB, POSTGRES_USER et
POSTGRES_PASSWORD initialisent uniquement un volume neuf ; les modifier ne
renomme pas un compte déjà enregistré.

## Volume déjà initialisé

Depuis la racine du dépôt sous PowerShell, avec le conteneur PostgreSQL lancé :

```powershell
# Uniquement si le compte historique n'a pas encore été migré :
.\scripts\migrate-dev-db-user.ps1
# Renommer ensuite la base (applications connectées arrêtées) :
.\scripts\rename-dev-database.ps1
docker compose up -d
docker compose ps
```

Le script cible exclusivement le compte de développement historique. Il refuse
un conflit de comptes, sauvegarde le cluster dans target/ (ignoré par Git),
renomme le rôle en conservant son identifiant interne et ses propriétés, change
son mot de passe de développement, puis supprime uniquement le rôle temporaire
créé pour cette opération. Le second script refuse une base encore utilisée,
sauvegarde son contenu, renomme la base et vérifie son identifiant interne ainsi
que les effectifs de ses tables. Les tables, propriétaires et historiques
restent liés au même identifiant de rôle ; aucune donnée métier n'est réécrite.

La sauvegarde contient des données et des informations de rôles : la conserver
localement et ne pas la publier. Ne pas supprimer le volume pour appliquer les
nouveaux identifiants. Après migration, les anciennes sessions ou commandes
utilisant l'ancien nom de compte doivent être reconfigurées.

Pour vérifier l'authentification TCP depuis le conteneur sans imprimer le mot de passe :

```powershell
docker compose exec -T postgres sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" psql -h 127.0.0.1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT current_user, current_database();"'
```

## Exécution vérifiée le 12 novembre 2026

- Compte renommé de padel en transport, identifiant interne conservé ; mot de
  passe de développement transport. Aucun rôle temporaire de migration restant.
- Base renommée en transportService, identifiant interne conservé ; effectifs
  des neuf tables publiques identiques avant/après. Sauvegardes locales conservées.
- Connexion JDBC en lecture seule, depuis l'hôte, avec les valeurs par défaut
  de application.properties : transport@transportService, succès.
- Docker Compose valide et PostgreSQL sain sur 5440, même volume persistant.
- Trois tests PostgresBaselineTest réussis avec DB_USER/DB_PASSWORD=transport
  sur la base isolée, avant renommage de la base métier. Aucune écriture de test
  dirigée vers transportService ; seul le contrôle JDBC en lecture seule l'utilise.
- La première commande de renommage a été refusée par PostgreSQL car PowerShell
  supprimait les guillemets du nom mixte ; aucune modification de nom à ce stade.
  Le script corrigé transmet le SQL sur l'entrée standard et a ensuite réussi.
