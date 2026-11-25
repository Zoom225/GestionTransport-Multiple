# Conventions de TransportMultiple

- Employer un vocabulaire professionnel et technique dans les contenus publics.
- Traiter un chantier à la fois. Réutiliser les issues et PR pertinentes. Utiliser une branche dédiée et des commits conventionnels en français. Vérifier le diff, le dépôt distant et la CI avant intégration.
- Préserver les travaux locaux et l’historique publié. Utiliser des chemins relatifs et des variables d’environnement. Tenir le journal local de reprise à jour après chaque chantier.
- Conserver l’architecture Controller → Service → Repository → PostgreSQL, les DTO, les mappers, les transactions et la gestion centralisée des exceptions. Préserver la structure Angular et appliquer DRY, SOLID et KISS.
- Préserver les rôles, contrôles métier, restrictions d’agence et verrouillages. Faire valider toute règle ambiguë. Tester les migrations explicitement sur une copie PostgreSQL isolée.
- Garder les secrets hors du dépôt et des journaux. Vérifier les accès JWT et l’isolation des données. Respecter les autorisations de production et de livraison.
- Lancer les tests ciblés avant les suites complètes. Exécuter `./mvnw.cmd clean verify` sous Windows et les scripts de `frontend/package.json`. Publier les résultats réels et vérifier la CI avant fusion.
