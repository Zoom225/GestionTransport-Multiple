# Déploiement de GestionTransport-Multiple

État au 24 novembre 2026. Configuration préparée pour Render, Vercel et PostgreSQL. Le projet Vercel et le workspace Render restent à confirmer. La mise en ligne et la rotation de production exigent les accords prévus pour la livraison.

## Backend Render et PostgreSQL

Le `Dockerfile` construit un JAR Spring Boot avec Java 21 et le démarre dans une image d’exécution Java 21. Le Blueprint `render.yaml` référence la branche `master`, le profil `production` et le contrôle de santé `GET /actuator/health`. Le déploiement automatique y est désactivé. Choisir la région du service en fonction de la base dédiée avant de provisionner. La configuration n’inclut aucun provisionnement PostgreSQL. [Blueprint Render](https://render.com/docs/blueprint-spec) · [Services web Render](https://render.com/docs/web-services).

Variables à renseigner dans Render.

| Variable | Valeur ou rôle |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `production` |
| `DB_URL` | URL JDBC PostgreSQL de la base dédiée. Utiliser le réseau privé si le service et la base le permettent, ou la connexion TLS requise par le fournisseur. |
| `DB_USER`, `DB_PASSWORD` | Utilisateur applicatif et mot de passe dans le gestionnaire de secrets. |
| `JWT_SECRET` | Valeur générée par un générateur cryptographique, propre à cet environnement. |
| `CORS_ALLOWED_ORIGINS` | Origines HTTPS des fronts approuvés, séparées par des virgules. Indiquer les origines exactes, sans chemin ni barre finale. |
| `PORT` | Port de service fourni par la plateforme. Valeur de repli locale 8080. |

Le profil de production désactive les données de démonstration et limite les informations publiques du contrôle de santé au statut. Les journaux utilisent la sortie du processus. Vérifier la disponibilité de PostgreSQL dans les journaux de démarrage et le retour `{"status":"UP"}` sur le point de santé. [Variables Render](https://render.com/docs/configure-environment-variables).

Avant déploiement sur une base existante, restaurer une sauvegarde, valider les migrations sur la copie, dont la migration des montants après intégration de la PR #47, puis appliquer les migrations autorisées dans la fenêtre convenue. Conserver `spring.jpa.hibernate.ddl-auto=update` et utiliser `validate` lors des contrôles isolés. La procédure de rotation JWT est décrite dans le [README](../README.md).

## Frontend Vercel

Configurer `frontend` comme répertoire racine du projet Angular et `master` comme branche de production. Sélectionner Node.js 24 et utiliser `npm ci`. `frontend/vercel.json` décrit la commande de build, le dossier produit et le traitement des routes internes. Définir `TRANSPORT_API_BASE_URL` comme l’URL HTTPS Render suivie exactement de `/api`. Le build local conserve `/api` pour le proxy Angular. Les environnements de prévisualisation doivent utiliser leurs propres valeurs et une origine explicitement autorisée. [Configuration Vercel](https://vercel.com/docs/project-configuration).

## Vérifications de publication

- Vérifier les deux URLs HTTPS et le code du commit déployé.
- Vérifier le contrôle de santé et la connexion PostgreSQL.
- Ouvrir une route Angular directe, puis vérifier les réponses préliminaires CORS depuis l’origine Vercel.
- Vérifier les connexions administrateur et voyageur et un parcours de réservation sur l’environnement de validation.
- Contrôler le refus d’un jeton invalide et d’un accès hors rôle.
- Consigner les URLs, dates, SHA, résultats et procédures de restauration autorisées.
