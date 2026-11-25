# Validation de la restauration PostgreSQL

Contrôles effectués sur PostgreSQL 15.20 dans le conteneur de tests isolé. Le backend de vérification correspond à `67128d5accbf04ad71ca121dadba209502bcb833`. Le schéma source avait déjà été restauré, converti vers les noms Transport et migré vers les montants décimaux sur des données synthétiques.

## Répéter la restauration sur une copie

1. Choisir une base de validation isolée. Utiliser des identifiants conservés dans l’environnement de test et un emplacement d’archive protégé.
2. Produire une archive `pg_dump -Fc --no-owner`.
3. Créer une nouvelle base isolée, puis exécuter `pg_restore --exit-on-error --no-owner` sur celle-ci.
4. Exécuter `scripts/migrations/issue-3-empreintes.sql` sur la source isolée et sur la restauration. Comparer le relevé avant toute action supplémentaire.
5. Vérifier l’état des clés étrangères dans `pg_constraint` puis exécuter `mvn -B -ntp -Dtest=TransportSchemaValidationTest test` avec `TRANSPORT_SCHEMA_URL`, `DB_USER` et `DB_PASSWORD` configurés pour la nouvelle base.

L’archive de test est restée dans le conteneur de validation avec des droits `600`. Aucun jeu de données ni archive n’est publié.

## Résultats de la restauration

| Table | Lignes | Contraintes | Index |
| --- | ---: | ---: | ---: |
| administrateurs | 3 | 4 | 3 |
| agences | 14 | 1 | 1 |
| indisponibilites_service | 0 | 2 | 1 |
| paiements | 15 | 4 | 2 |
| reservations | 15 | 5 | 2 |
| restrictions | 6 | 7 | 6 |
| trajets | 15 | 11 | 5 |
| vehicules | 15 | 2 | 1 |
| voyageurs | 39 | 3 | 2 |

La source et la restauration correspondent sur les neuf empreintes, les comptages et le nombre de contraintes et d’index par table. Aucun lien vers une contrainte de clé étrangère non validée n’a été relevé. Hibernate en mode `validate` a réussi ses deux tests, avec zéro échec et zéro test ignoré, en 5 min 30 s au total pour Maven.

Le candidat a aussi passé les contrôles de répétabilité, de refus de schéma ambigu et de migration décimale vérifiés lors de la CI de `develop`. [CI intégrée](https://github.com/Zoom225/GestionTransport-Multiple/actions/runs/45467082023) · [Migration des montants](migration-montants.md).

La vérification correspond aux données synthétiques. Les archives et la fenêtre de restauration d’un environnement métier doivent être validées avec son responsable avant intervention.
