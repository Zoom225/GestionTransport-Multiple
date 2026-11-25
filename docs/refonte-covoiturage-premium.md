# Refonte premium du covoiturage

La page d’accueil existante conserve la recherche et les trajets provenant de l’API. La nouvelle bannière présente le covoiturage privé et public. Elle donne accès au formulaire de recherche et à la route de proposition existante, avec le même contrôle de connexion.

## Identité visuelle et composants

Le bleu nuit `#10243A`, le turquoise `#12B8A6` et le blanc glacier `#F5F7FA` alimentent les variables CSS centrales. Les autres tons de l’interface sont des nuances de cette base et les couleurs des états métier restent distinctes. Les styles utilisent la hiérarchie de titres existante, des bordures fines, des ombres discrètes, des zones tactiles et un focus visible. Les actions principales combinent texte bleu nuit et fond turquoise. Les boutons désactivés gardent un style distinct.

- `frontend/src/styles.scss` et `frontend/src/app/app.scss` portent la charte, les surcharges Angular Material et la mise en page responsive.
- `frontend/src/app/app.ts` garde les routes de navigation et ajoute les liens utiles vers la recherche et le fonctionnement dans le pied de page.
- `features/home/pages/landing.page.ts` adapte le panneau de recherche, les sections de présentation et la grille de résultats.
- `shared/components/car-hero-carousel.component.ts` anime la bannière et gère les commandes visuelles, clavier et tactiles.
- `shared/components/trajet-card.component.ts` et `trajet-search.component.ts` retravaillent les cartes, la distinction départ/arrivée, le prix, les places et la grille du formulaire.
- `features/voyageur/pages/voyageur-public-trajets-page.component.ts` harmonise les cartes et l’appel à la création.
- `features/voyageur/pages/voyageur-home-page.component.scss` et `voyageur-profile-page.component.scss` appliquent la palette aux pages de connexion et de profil. Les pages de création, réservation, paiement et les dialogues héritent de la charte partagée.
- `features/admin/pages/admin-home.page.scss` et `admin-login.page.scss` alignent l’administration et sa connexion sur la même palette.
- `core/i18n/translations.ts` actualise les libellés français et anglais. `shared/components/car-hero-carousel.component.spec.ts` couvre les comportements du carrousel.

Les composants sont réutilisés et les modèles de données, validations, services HTTP, guards, sessions et règles de réservation, paiement, pénalité et suspension restent ceux de l’application. Les routes actuelles ont été conservées. Le projet ne possède pas de route autonome d’inscription ni de page légale à relier au pied de page.

## Carrousel et images

Quatre visuels, rotation circulaire toutes les **6 000 ms**, fondu de **650 ms** avec `cubic-bezier(.22, .61, .36, 1)`. Le survol, l’onglet caché ou moins de 25 % du carrousel dans la fenêtre suspendent temporairement la lecture. Le focus entrant, la navigation manuelle et le balayage la mettent en pause jusqu’à la reprise explicite. Les flèches, Début et Fin sont disponibles au clavier. Le balayage est horizontal, avec un seuil de 50 px, pour préserver le défilement vertical. La préférence de mouvement réduit désactive le défilement automatique et les transitions. Les observateurs, écouteurs et timers sont nettoyés.

| Visuel | Origine et droits |
|---|---|
| `car-suv-*.webp` | SUV fictif généré pour le projet avec l’outil intégré ImageGen. [Conditions OpenAI](https://openai.com/policies/terms-of-use/) |
| `car-electrique-*.webp` | Électrique fictive générée pour le projet avec le même outil. [Conditions OpenAI](https://openai.com/policies/terms-of-use/) |
| `car-route-*.webp` | Berline de Kelvin Linhares. [Photo source](https://www.pexels.com/photo/sleek-silver-car-on-scenic-country-road-28502253/) · [Licence Pexels](https://www.pexels.com/license/) |
| `car-famille-*.webp` | Photo de Mikhail Nilov, conservée dans le carrousel et les cartes d’illustration. [Photo source](https://www.pexels.com/photo/a-family-standing-near-parked-car-while-looking-at-the-camera-8323226/) · [Licence Pexels](https://www.pexels.com/license/) |
| `car-bruxelles-640.webp` et `route-partagee.jpg` | Photos d’ambiance Pexels existantes, utilisées dans la bannière des pages et la présentation. Sources dans le registre des visuels. |

Les huit nouveaux fichiers comportent des variantes 640, 1 024 et 1 536 px, plus un cadrage mobile 640×800. La variante principale mobile pèse environ 71 Ko. L’image initiale est prioritaire. Les autres vues sont chargées progressivement, avec dimensions et textes alternatifs. Les visuels restent identifiés comme illustrations distinctes de la flotte. Les prompts complets figurent dans [le registre des images](../frontend/public/images/LICENCES.md).

## Vérification

- **96 tests Angular réussis dans 16 fichiers** avec `npm test -- --watch=false --pool=threads --maxWorkers=2`. Les deux premières exécutions utilisaient des processus Windows et ont rencontré des délais d’attente de workers. Le pool de threads a exécuté la suite complète. Après la dernière finition visuelle, les **14 tests ciblés** de navigation et du carrousel ont réussi. Suite complète (`.local-validation/premium-refonte-tests-threads.log`) · Finition (`.local-validation/premium-refonte-tests-finition.log`)
- **Build de production réussi** depuis le dossier `frontend`. Le serveur de développement écoute sur `127.0.0.1:4212`. Journal du build (`.local-validation/premium-refonte-build-final.log`)
- **Edge à 320, 375, 390, 430, 768, 1 024, 1 280, 1 440, 1 920 et 2 560 px**. Aucun débordement horizontal de la page d’accueil sur la matrice testée. Le cadrage de la photo et le libellé de marque au format compact ont été inspectés visuellement. Résultats et pages (`.local-validation/premium-refonte-preuves/rapport-navigateur-pages.json`) · 320 px (`.local-validation/premium-refonte-preuves/accueil-320.png`) · 390 px (`.local-validation/premium-refonte-preuves/accueil-390.png`) · Tablette (`.local-validation/premium-refonte-preuves/accueil-768.png`) · Bureau (`.local-validation/premium-refonte-preuves/accueil-1440.png`)
- Edge a confirmé le fondu de 650 ms, la rotation, les flèches et le clavier, le survol, la navigation mobile avec Échap, le balayage tactile et l’arrêt automatique en mouvement réduit. L’observation hors écran et le nettoyage sont couverts par les tests Angular. Les contrastes calculés bleu nuit/turquoise et bleu nuit/blanc glacier sont respectivement de **6,31:1** et **14,65:1**. Ce sont des vérifications ciblées, sans audit WCAG ni mesure Lighthouse complète. Vérification mobile (`.local-validation/premium-refonte-preuves/rapport-navigateur-mobile.json`)
- Le formulaire réel « Mons → Bruxelles » a émis `GET /api/trajets/public?depart=MONS&arrivee=BRUXELLES` via le proxy Angular et reçu **HTTP 200**. `GET /api/trajets/villes` a répondu 200 avec dix villes et le catalogue public non filtré 200 avec zéro trajet au moment de la vérification. Certains essais sur le backend local ont dépassé le délai du navigateur, puis la réponse a été confirmée lors d’un essai dédié.
- Les deux pages de connexion sont accessibles. La carte de trajet et son dialogue, le profil avec un état suspendu et une pénalité à régler, la création, la recherche connectée, les réservations, les paiements et le tableau de bord administrateur sont vérifiés dans Edge avec des fixtures de navigateur. Aucune opération de paiement ou de réservation n’a été envoyée à la base. Dialogue (`.local-validation/premium-refonte-preuves/details-390-fixture.png`) · Profil et pénalité (`.local-validation/premium-refonte-preuves/profil-penalite-390-fixture.png`) · Administration (`.local-validation/premium-refonte-preuves/administration-1024-fixture.png`)

Aucune dépendance npm ajoutée. Les changements locaux antérieurs ont été conservés. Aucun commit, push ni merge.
