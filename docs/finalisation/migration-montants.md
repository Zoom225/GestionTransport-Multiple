# Migration des montants monétaires

Les soldes, tarifs et paiements utilisent `BigDecimal` et `numeric(19,2)`. Une pénalité existante reste en `numeric(12,2)`, avec le même nombre de décimales. Les tarifs opérationnels restent 25,00 € par passager et 100,00 € par trajet. Les API continuent à sérialiser les montants comme nombres JSON.

## Préparer une copie et contrôler les valeurs

Préparer et restaurer une sauvegarde PostgreSQL dans une base isolée. Si la base utilise encore des noms historiques, exécuter dans l’ordre les migrations du schéma Transport et des sanctions indiquées dans le guide d’exécution.

Avec l’application arrêtée sur la copie, exécuter `psql -X -v ON_ERROR_STOP=1 -d <base_isolee> -f scripts/migrations/issue-45-montants-bigdecimal.sql`. Les cinq colonnes sont converties dans une seule transaction. Les nombres non finis, les fractions de centime, les valeurs hors plage ou les types inattendus provoquent un refus global. En cas de refus, examiner les valeurs incompatibles sur la copie et obtenir une décision métier avant de corriger les données.

Comparer les valeurs monétaires, identifiants, relations et comptages avant et après. Le workflow rejoue la migration sur une fixture flottante et contrôle qu’une valeur incompatible annule la transaction. Pour la copie complète, utiliser `TransportSchemaValidationTest` avec la configuration de validation isolée. Conserver la sauvegarde vérifiée pour une restauration éventuelle.

Le garde-fou du backend arrête le démarrage avant Hibernate si des colonnes monétaires existantes exigent cette migration. L’exécution sur une base métier exige l’accord et la fenêtre d’intervention prévus pour la livraison.
