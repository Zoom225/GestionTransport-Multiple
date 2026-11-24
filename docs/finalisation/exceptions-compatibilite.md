# Compatibilité du schéma historique

Le backend utilise le package `com.transport.gestiontransport`. Le nom des tables, champs et parcours courants suit le vocabulaire du transport. Les identifiants SQL hérités sont conservés aux emplacements suivants pour traiter les bases antérieures au renommage.

| Emplacement | Rôle des anciens identifiants |
| --- | --- |
| `scripts/migrations/issue-3-schema-transport.sql` | Détecte les schémas ambigus et renomme les tables et colonnes existantes, notamment `sites`, `terrains`, `membres` et `matches`. |
| `scripts/migrations/issue-3-empreintes.sql` | Calcule des empreintes comparables avant et après le renommage en comprenant les deux familles de noms. |
| `scripts/migrations/issue-7-invariants-covoiturage.sql` | Interroge les anciennes relations pour vérifier les invariants sur un schéma préconversion. |
| `src/test/resources/schema-historique-fixture.sql` | Produit le schéma de départ sur une base de test isolée. |
| `src/main/java/com/transport/gestiontransport/config/TransportSchemaGuard.java` | Détecte les tables héritées et bloque le démarrage avant qu’Hibernate puisse créer des tables concurrentes. |
| `src/test/java/com/transport/gestiontransport/config/TransportSchemaGuardTest.java` | Vérifie le comportement du garde-fou sur les schémas historiques et courants. |

Les documents `docs/audit-initial.md` et `docs/covoiturage/issue-6-interface.md` conservent des observations datées sur les anciens parcours. Le guide courant est [le README](../../README.md).

Le JAR exécutable est produit par Maven sous le nom `target/gestiontransport-multiple-0.0.1-SNAPSHOT.jar`. Les configurations IntelliJ personnelles qui nomment explicitement l’ancien module doivent être actualisées après réimport du projet Maven. Le script de test accepte le nom de module importé quand il peut le lire dans IntelliJ.
