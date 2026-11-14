# Issue #6 — Interface de covoiturage

## Refonte visuelle professionnelle — PR #21

Branche : `feature/issue-6-ui-professionnelle`, depuis `develop` validée à `b818392`.
Issue existante #6 réutilisée. Les PR #19 (livraison) et #20 (lancement IntelliJ/JWT)
restent séparées. Aucune fusion automatique.

### Audit avant modification

Déjà présents : Angular 21, Material/Tailwind, FR/EN, navigation, recherche par villes
chargées depuis l'API, cartes de trajet, formulaires, espaces voyageur/chauffeur et
administration global/local, historique, paiement simulé et restrictions.
L'API possédait ses contrats et les guards/interceptor existaient réellement.
Une page ou fiche de détail dédiée manquait ; les couleurs, ombres et styles étaient
dispersés entre composants. Les anciennes palettes variaient selon les écrans.

`git status` initial : diff local `pom.xml` (`industri`) et `package-lock.json` non
suivi à la racine. Tous deux préservés hors commits. Références distantes récupérées ;
PR #19/#20 et issue #6 examinées. Aucune modification des données PostgreSQL.

Référence réellement exécutée avant modification : installation `npm ci`,
build de production et **63 tests réussis**. Le premier `npm ci` a échoué avec
`EPERM unlink esbuild.exe` : deux serveurs Angular de ce projet verrouillaient le
binaire. Leur identification par chemin a permis de les arrêter sans toucher aux
autres projets, puis l'installation a réussi. Les scripts npm sont inchangés.
**45 captures de référence**, avec API simulées, couvrent 15 écrans à 375/768/1440 px.

### Modifications réutilisant l'existant

- Variables CSS de palette, typographie, bordures, ombres et états sémantiques dans
  `frontend/src/styles.scss`. Styles header/footer dans `app.scss`. Tokens Material
  adaptés à la version installée, sans bibliothèque supplémentaire.
- Navigation mobile accessible, lien d'évitement, état actif, focus et fermeture
  par Échap ; liens chauffeur et administrateur selon les sessions existantes.
  Footer commun à tous les écrans. Animations réduites selon la préférence système.
- Accueil avec illustration SVG locale originale, recherche existante et suggestions
  explicitement présentées comme recherches, pas comme disponibilités fictives.
- Cartes réutilisées et fiche `TrajetDetailComponent` en dialogue Material, accessible
  au clavier : villes, horaires, chauffeur, véhicule, disponibilité, type, statut et prix.
  Aucun nouveau endpoint/DTO ; fermeture sans réservation, action de réservation
  désactivée dans les mêmes cas que sur la carte, confirmation après paiement inchangée.
- Formulaires, profils, réservations, paiement/historique, pages chauffeur et toutes
  les pages admin harmonisés. Boutons désactivés lisibles, états vide/erreur/chargement,
  statuts explicites et présentation responsive des motifs/contraintes.
- SVG d'icônes partagés : les icônes de mot de passe fonctionnent sans police distante.
  Libellés VIP/local/libre cohérents ; règles et enumérations métier non modifiées.
- Messages 401/session expirée, 403/accès refusé et erreur réseau traduits dans l'utilitaire
  de présentation existant. Les autres messages métier du backend restent prioritaires.
  Aucun changement de requête, contrat JSON, authentification, guard ou interceptor.

Fichiers créés : `trajet-detail.component.ts`, deux SVG locaux dans `frontend/public/images`,
`scripts/validation-ui.cjs` et captures JPEG avant/après. Composants existants adaptés ;
aucun nouveau service API, DTO, mapper ou backend. Les styles globaux concurrents et
effets décoratifs superposés sont remplacés par le thème commun ; les styles des cartes
et de l'accueil sont adaptés dans les composants existants. Les liens de polices
distantes et la dépendance visuelle aux ligatures Material Icons sont retirés.

### Vérifications finales

`npm run build` : réussi. `npm test` : **69 tests réussis** (63 conservés + deux tests
de fiche/blocage + quatre tests de messages d'erreur traduits). Compilation IntelliJ réussie.
Les tests existants n'ont pas été supprimés.

Le bundle initial passe de **535,31 à 547,77 kB**, transfert estimé de **133,63 à
137,41 kB** : légère hausse associée à la fiche Material et aux textes, sans nouvelle
dépendance de production. Le chargement différé des routes est conservé. Les SVG
sont locaux et légers ; aucune police distante nécessaire.

Les vérifications navigateur utilisent Edge, Playwright et axe-core avec **API
simulées dans le navigateur**, jamais des écritures PostgreSQL. Elles vérifient les
interactions et contrats utilisés côté frontend ; elles ne remplacent pas une validation
du métier serveur ou un test bancaire réel. Les validations serveur antérieures sont
documentées séparément dans `issue-7-validation.md`.

La passe complète comprend **57 contrôles responsive** à 375, 768 et 1440 px :
16 écrans (accueil, connexions, profil, recherche, création voyageur/admin, réservations,
paiements, dashboard global/local et pages de gestion), fiche de trajet, déclaration
d'absence et historique des restrictions. Aucun débordement horizontal, champ sans
libellé détecté ou erreur JavaScript. Les formulaires de connexion actifs, le masquage
du mot de passe, le menu mobile/Échap, le lien d'évitement, la navigation protégée,
les états vide/erreur, FR/EN et la préférence de mouvement réduit sont contrôlés.

Parcours **recherche Mons → Bruxelles → fiche → demande EN_ATTENTE → paiement simulé
de 25 € → confirmation → trois places restantes → historique** réussi avec ces API
simulées. Les erreurs de session/réseau sont provoquées avant le paiement réussi.
Une carte complète et sa fiche refusent l'action de réservation. L'administration
permet de saisir un motif et d'enregistrer une déclaration avec réponse API simulée,
puis de consulter les motifs et dates des restrictions historiques.

**20 analyses axe-core**, sur les règles WCAG A/AA sélectionnées, ne signalent aucune
violation. Ce résultat est complété par la revue visuelle des captures ; il ne constitue
pas un audit exhaustif de conformité. Les dégradés peuvent limiter le calcul automatique
des contrastes : les bandeaux admin ont été uniformisés sur fond marine et les actions
principales sur fond bleu, avec contrôle visuel des textes et des états désactivés.

Corrections pendant les vérifications : styles externes initialement non résolus par
Vitest JIT (conventions inline conservées), doubles de dialogue remplacés par un espion
sur le service effectivement injecté, typage et placement d'une branche d'affichage
corrigés, trois défauts de contraste détectés par axe-core corrigés. Aucun test supprimé
et aucune règle métier changée pour obtenir ces résultats.

Le contrôle interactif du formulaire d'absence a aussi révélé une soumission non
raccordée : le formulaire utilisait `ngSubmit` sans directive de groupe de formulaire.
Le template écoute désormais l'événement natif `submit`, empêche la navigation native
et appelle la méthode existante. Motif, validations, permissions, API et règles de
restriction restent identiques. Ce correctif concerne uniquement le branchement UI.

Captures comparables : [accueil avant](../images/ui/avant-accueil-1440.jpg),
[accueil après](../images/ui/apres-accueil-1440.jpg),
[mobile après](../images/ui/apres-accueil-375.jpg),
[administration après](../images/ui/apres-dashboard-1440.jpg),
[fiche mobile](../images/ui/fiche-trajet-375.jpg).
Ces captures contiennent uniquement des fixtures clairement nommées « Démonstration UI ».

### Reproduire les contrôles frontend

Depuis la racine, sous PowerShell, arrêter les serveurs Angular de ce projet avant
`npm ci` si Windows verrouille esbuild. Les commandes ne nécessitent aucun backend :

```powershell
cd frontend
npm ci
npm run build
npm test
cd ..
npm install --prefix .local-validation/ui/browser --no-save --package-lock=false playwright@1.56.1 @axe-core/playwright@4.10.2
$env:UI_PHASE = 'after'
node scripts/validation-ui.cjs
```

Le script sert le build sur `127.0.0.1:4201` et intercepte toutes les requêtes API.
Edge est utilisé par défaut ; `EDGE_PATH` permet de définir un autre binaire compatible.
Rapport et captures : `.local-validation/ui/after`, hors Git. L'outillage navigateur
reste local et ne modifie pas les dépendances de production.
Un audit axe-core sans violation sur les règles sélectionnées n'est pas une certification
WCAG complète ; une revue avec lecteurs d'écran et utilisateurs reste utile.

## Historique de l'interface métier initiale

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
# Administration et finition premium — issue #6

Branche `feature/issue-6-admin-ui-premium`, créée depuis `develop` b818392, puis reprise par fast-forward des commits de #21 (toujours ouverte). Aucune fusion dans develop. Cette PR complémentaire dépend de #21 et conserve ses corrections, sa fiche de trajet et son thème.

Cause de l'accès peu visible : lien administrateur réservé au footer avant connexion et libellé générique du dashboard après connexion. L'accès est désormais dans le header public. Les sessions affichent « Administration globale » ou « Administration de mon agence ». Une navigation dédiée réutilise les pages existantes (latérale à 1440 px, empilée sur mobile), sans modifier les guards ni les contrats REST. Les recettes restent dans le dashboard, les restrictions/absences dans les voyageurs ; aucune page factice ni statistique inventée.

L'accueil utilise une photo routière locale de 146 ko, avec dimensions, recadrage responsive, alternative traduite et secours SVG. Source et licence : `frontend/public/images/LICENCES.md`. Aucune affirmation qu'il s'agit d'un véhicule réel de la plateforme. La maquette « Ride Smarter » n'étant pas jointe, la finition suit la direction artistique décrite dans la demande sans prétendre reproduire une image non consultée.

Validations exécutées le 12 novembre 2026 :

- Référence : build Angular et 69 tests réussis ; `mvn -B verify` réussi, 140 tests exécutés et 15 PostgreSQL ignorés.
- `npm ci` réussi : 533 paquets. Build relancé après la fin de l'installation (un lancement trop tôt avait échoué, commande ng temporairement absente).
- Build final réussi : initial 551,15 ko / transfert estimé 138,05 ko. Suite Angular : 72 tests réussis, dont trois nouveaux contrôles de navigation GLOBAL/AGENCE et accès public. Leur montage JIT a nécessité de simuler AuthApiService ; aucun test supprimé. Compilation IntelliJ réussie.
- PostgreSQL : premier essai interrompu par perte de connexion du moteur Docker (1 échec, 3 erreurs). Docker Desktop relancé sans réinitialisation ; nouvelle base isolée et nouvelle exécution : 15 tests réussis, zéro erreur/échec/ignoré.
- Edge : 57 contrôles à 375/768/1440 px, aucun débordement ou erreur JS ; 20 analyses axe-core sans violation détectée sur les règles sélectionnées. APIs simulées pour ces contrôles visuels, distinction conservée avec l'intégration réelle. Revue visuelle des captures accueil et dashboard effectuée.

Captures : [accueil ordinateur](../images/ui/premium-accueil-1440.jpg), [accueil mobile](../images/ui/premium-accueil-375.jpg), [dashboard global](../images/ui/premium-dashboard-1440.jpg), [agence mobile](../images/ui/premium-agence-375.jpg). Les captures précédentes restent disponibles pour comparaison. Aucun changement backend ou PostgreSQL dans ce chantier frontend ; pom.xml et le lock non suivi à la racine restent hors commit.

Int?gration r?elle compl?mentaire : scripts/validation-covoiturage.cjs ex?cut? sur transport_demo_premium_20261112234317, backend et PostgreSQL r?els, donn?es dev isol?es. Connexions GLOBAL/AGENCE, filtrage local et refus de cr?ation hors p?rim?tre, r?servation Angular et paiement 25 ?, quatre paiements/100 ?, concurrence derni?re place, annulation/remboursement, historique et statistiques r?ussis ; douze contr?les responsive r?els sans d?bordement et aucune erreur JavaScript. Le premier lancement de pr?paration a refus? un mot de passe de d?monstration trop long pour BCrypt ; le g?n?rateur local a ?t? corrig? ? 32 octets al?atoires avant cette ex?cution r?ussie. Aucun secret publi?.
# Correction du choix des véhicules — issue #6

Finition demandée : les deux modèles présentés sont désormais **Renault Clio**
et **Peugeot 308**, centralisés dans DataInitializer. Sur la copie locale,
les anciennes voitures de démonstration sont renommées en conservant leurs ID,
relations et disponibilité ; aucun doublon n'est ajouté. Les ressources historiques
restent inchangées. Il s'agit de modèles d'exemple, sans immatriculation ni affirmation
qu'ils représentent une flotte réelle.
Les dix tests ciblés DataInitializer et le packaging Maven réussissent après
ce changement. Le renommage conserve bien les quatre ID des voitures de la
copie, et les neuf connexions ont été revérifiées après préparation.

Branche `fix/issue-6-choix-vehicules`, depuis la branche de validation des comptes
#25. La copie locale contenait cinq ressources historiques indisponibles : le
filtre existant ne proposait donc aucun véhicule. Elles sont conservées avec
leur disponibilité désactivée ; aucune conversion automatique en voiture.

Le formulaire existant utilise désormais l'état du FormControl pour désactiver
le champ pendant le chargement. Les options doivent être disponibles et appartenir
à l'agence sélectionnée. Un seul véhicule est présélectionné ; avec plusieurs
véhicules, le choix est explicite et obligatoire. Les anciens chargements sont
annulés au changement d'agence. Chargement, absence de véhicule et erreur sont
distingués, avec nouvel essai et textes FR/EN ; un administrateur peut rejoindre
la gestion des véhicules depuis l'état vide. Les contrôles backend sont inchangés.

DataInitializer est réutilisé pour préparer deux voitures de démonstration par
agence, explicitement et uniquement sur copie locale existante avec le profil
dev, DEMO_DATA_ENABLED=true et DEMO_PREPARE_VEHICLES=true. L'opération est désactivée
par défaut, idempotente, et refuse transportService. Aucun compte, paiement,
trajet ou ressource historique n'est réécrit. Les bases dev vides reçoivent aussi
deux voitures pour permettre un choix, sans nouvelle table ou dépendance.

Après sauvegarde, la copie transport_schema_copy_20261113181001595 possède deux
voitures de démonstration disponibles dans chacune des deux agences, en plus
des cinq ressources historiques toujours indisponibles. Les neuf connexions
administrateurs/voyageurs ont été revérifiées après cette préparation.

Validations exécutées : build Java réussi, 165 tests recensés dont 148 exécutés
sans échec et 17 tests PostgreSQL/JPA opt-in non activés lors de cette exécution.
Les neuf tests DataInitializer incluent préparation idempotente et refus de la
base métier. Build Angular final réussi et 76 tests réussis ; quatre nouveaux
tests couvrent choix multiple, indisponibilité, réponses tardives et nouvel essai.
Compilation IntelliJ réussie. Aucun test supprimé.

Edge avec backend/PostgreSQL réels : ouverture du menu, deux options de démo,
sélection de la seconde voiture, changement d'agence avec effacement du choix
précédent et rechargement. Administrateurs GLOBAL/AGENCE à 375/768/1440 px : six
contrôles sans débordement et aucune erreur JS. Le serveur Angular de développement
a été relancé après un écran d'erreur résiduel de compilation intermédiaire des
clés de traduction ; les builds finaux et le contrôle navigateur sont réussis.
Le serveur du projet reste sur http://127.0.0.1:4202.

## Bannières automobiles et noms des agences — 14 novembre 2026

Issue #6, branche `feature/issue-6-bannieres-agences-transport`, basée sur le
correctif des villes distinctes de la PR #27. Les PR précédentes restent à revoir
et ne sont pas fusionnées automatiquement. `pom.xml` conserve son diff local
`artifactId=industri`, hors commit ; le package-lock non suivi à la racine est conservé.

Les noms Padel dans les filtres provenaient des données des agences renvoyées
par `/api/agences`, pas du libellé du filtre. DataInitializer dispose maintenant
d’une préparation explicite des noms sur copie locale uniquement : profil dev,
`DEMO_DATA_ENABLED=true`, `DEMO_PREPARE_AGENCIES=true`. Ce dernier flag est faux par
défaut. L’opération refuse `transportService`, conserve les ID, adresses et relations,
et une seconde exécution ne réenregistre pas les agences déjà renommées.
Après sauvegarde de la copie `transport_schema_copy_20261113181001595`, les agences
1 et 2 portent les noms **Agence Transport Élégant** et **Agence Transport Design**.
Les neuf connexions ont été revérifiées après cette préparation. Aucun compte,
mot de passe, paiement ou trajet n’est réécrit par cette opération.

Une nouvelle base de démonstration reçoit directement « Agence Transport Élégant —
DÉMONSTRATION ». Les villes et adresses des agences historiques sont préservées.
Il n’y a aucune migration SQL ni modification de la base métier dans ce chantier.

Le même thème est réutilisé : illustration SVG automobile originale et légère,
accueil illustré, bandeau automobile sur les cartes de trajets, bannière contextuelle
sur les pages internes, cartes et séparateurs harmonisés. Les écrans de connexion
et d’administration utilisent une bannière compacte. Le menu d’administration mobile
conserve les liens et permissions existants ; ouverture, fermeture, Échap et retour
du focus sont contrôlés. Les nouveaux textes passent par les traductions FR/EN.
L’illustration est décorative sur les cartes et bannières ; sa provenance est dans
`frontend/public/images/LICENCES.md`. Aucun ajout de bibliothèque ni contrat API modifié.

Validations exécutées :

- `mvn -B verify` : build réussi, 171 tests recensés, 154 réussis, 17 tests PostgreSQL
  opt-in non activés lors de cette première exécution. Les 12 tests DataInitializer
  incluent deux nouveaux cas : renommage idempotent et refus de la base métier.
- Build Angular de production réussi ; compilation IntelliJ réussie après une
  première demande ayant dépassé son délai, sans erreur de compilation signalée.
- Exécution finale Angular : **78 tests réussis**, 14 fichiers de tests.
- Tests PostgreSQL ciblés exécutés ensuite : **17 réussis, aucun ignoré**
  (`PostgresBaselineTest` et `TransportSchemaValidationTest`) sur une nouvelle base
  isolée pour les écritures et sur la copie restaurée en lecture/validate.
  Les erreurs SQL d’unicité et de clé étrangère attendues par les tests négatifs
  ne sont pas des échecs. Aucun test n’a été supprimé.
- Navigateur Edge, API simulées : 57 affichages aux largeurs 375, 768 et 1440 px,
  sans débordement ni erreur JavaScript ; 20 analyses axe-core sans violation détectée.
  Le passage final inclut le nouveau menu mobile, sa navigation et le retour du focus.
  Cela ne constitue pas une certification d’accessibilité de toutes les interactions.
- Navigateur Edge, API/PostgreSQL réels sur `transport_demo_premium_20261114031416` :
  11 contrôles fonctionnels et 12 contrôles responsive réussis. Réservation Angular,
  paiement de 25 €, confirmation, quatre clients/100 €, concurrence dernière place,
  annulation/remboursement, historique, GLOBAL/AGENCE et refus hors périmètre vérifiés.
  Le premier essai avait dépassé le délai de démarrage du backend ; la relance réussit.
- Sur la copie locale active, six contrôles GLOBAL/AGENCE à 375/768/1440 px : noms
  Transport dans les choix d’agences et le formulaire de création, menu mobile/Échap/focus,
  aucun débordement et aucune erreur JavaScript. Une assertion initiale a été corrigée
  pour attendre la mise à jour du DOM Angular, sans délai fixe ni changement métier.

Captures après modification, avec des données de démonstration et API simulées,
inspectées visuellement ; les captures avant des précédents chantiers sont conservées :

- [Accueil ordinateur](../images/ui/mobilite-accueil-1440.jpg)
- [Accueil mobile](../images/ui/mobilite-accueil-375.jpg)
- [Administration mobile](../images/ui/mobilite-dashboard-375.jpg)

Le serveur de développement du projet a été relancé sur http://127.0.0.1:4202.
Les dumps, secrets, rapports détaillés et captures supplémentaires restent hors Git.

Complément demandé : les noms finaux sont **Agence Transport Élégant** (ID 1)
et **Agence Transport Design** (ID 2). La préparation reconnaît aussi bien les
anciens noms Padel que les noms intermédiaires « Agence Transport Lyon/Paris ».
Les noms cibles sont centralisés dans DataInitializer, sans création d’agence supplémentaire.
Après une nouvelle sauvegarde, le renommage est appliqué à la copie locale active.
Packaging Maven et **13 tests DataInitializer réussis**, compilation IntelliJ réussie,
neuf connexions revérifiées et affichage GLOBAL/AGENCE contrôlé dans Edge à 1440 px
(deux contrôles, aucune erreur JavaScript). Les adresses et rattachements sont conservés.
Le frontend est inchangé dans ce complément ; les résultats Angular ci-dessus
correspondent au chantier visuel précédent. Traçabilité : issue #6, même branche et PR #28.

### Clarification chauffeur et passagers

Le type de compte Voyageur est distinct du rôle dans un trajet. Un voyageur autorisé
qui propose un trajet en devient le chauffeur, identifié depuis sa session ;
un administrateur sélectionne un chauffeur existant. Les quatre places concernent
les autres passagers. Le même utilisateur peut réserver sur les trajets d’autres
chauffeurs. Ces contrôles existent déjà dans TrajetServiceImpl et ReservationServiceImpl ;
ils sont réutilisés sans seconde implémentation ni changement des permissions.

Le formulaire affiche maintenant « Vous êtes le chauffeur de ce trajet : Tom Bernard.
Proposez jusqu’à 4 places passagers à 25 € par personne. » avec le vrai nom de la session.
Les cartes et fiches indiquent « Votre chauffeur » ; les cartes distinguent
« Places passagers disponibles : 4 sur 4 ». La confirmation de création est également
clarifiée. Les variantes anglaises utilisent le même mécanisme de traduction.
README et assertion existante d’affichage des quatre places actualisés.
Build Angular et **78 tests réussis** ; compilation IntelliJ réussie après une première
demande ayant dépassé son délai. Aucun changement backend, DTO, API, schéma ou donnée.

Contrôle Edge avec le backend local : connexion de Tom Bernard, notice française
à 375 et 1440 px sans débordement, traduction anglaise, puis connexion GLOBAL et
sélection du chauffeur toujours présente sans notice attribuant ce rôle à l’administrateur.
Quatre contrôles réussis, aucune erreur JavaScript. Le premier essai rencontrait
une superposition Vite conservée lors de l’édition des traductions ; le serveur du
projet a été relancé sur 4202 et la vérification finale réussit. Aucun trajet n’est
créé ni aucune réservation effectuée par ce contrôle de présentation.
