# Issue #6 — Interface de covoiturage

Branche `feature/issue-6-transport-frontend`, après la PR #15.

Les pages et services Angular existants sont réutilisés. Deux composants partagés
évitent de dupliquer la recherche et les cartes entre l'accueil et la liste publique.
Le service HTTP existant charge et mémorise les villes fournies par le backend.
L'interceptor existant est l'unique source des en-têtes d'authentification.

Accueil : « Voyagez ensemble, simplement », recherche villes/date, vrais trajets,
état vide, suggestions de recherches populaires clairement identifiées,
présentation du fonctionnement, espace voyageur et pied de page.

Cartes : villes, chauffeur, véhicule, places restantes sur quatre, prix par personne,
PUBLIC/PRIVE et action désactivée si complet.

Création : formulaire existant adapté aux deux villes, consentement H-24 et quatre
invitations clients. Le chauffeur n'est pas un des clients. Les limites de réservation
par catégorie ne limitent plus la date de création d'un trajet par son chauffeur.
Le même formulaire est réutilisé dans `/admin/trajets/new` avec sélection obligatoire
d'un chauffeur autorisé et les guards administrateurs existants.

Administration : case de permission chauffeur dans le formulaire Voyageur existant,
tableau de bord conservé, agences filtrées par identifiant. Profil voyageur : voyages
à venir, historique, paiements et liste des trajets proposés ; restrictions existantes
conservées. Les appellations et pictogrammes sportifs visibles ont été corrigés.

Code supprimé : `app.html` était un doublon inutilisé du template inline de `App`.
Le template réellement utilisé reste inline, compatible avec les tests Vitest.
L'ancien dessin de terrain de sport sur l'accueil a été retiré.

## Vérifications réelles

- 61 tests Angular passent : les 58 tests conservés/adaptés et trois tests des
  composants partagés (filtres, quatre clients hors chauffeur, trajet complet).
- Build Angular de production exécuté avec succès.
- Edge/Playwright : 375, 768 et 1440 px ; largeur document égale à la largeur
  viewport dans les trois cas ; menu mobile ouvert/fermé et navigation vérifiés.
- Recherche Mons → Bruxelles du navigateur au backend : HTTP 200 et filtres contrôlés.
- Swagger, OpenAPI, villes et recherche publique : HTTP 200.
- Aucune erreur JavaScript. Captures relues dans `target/browser-validation/`.

Essais sur PostgreSQL HTTP isolé, DEMO_DATA_ENABLED=false ; aucune insertion
automatique de trajets ou de comptes dans la base métier.
