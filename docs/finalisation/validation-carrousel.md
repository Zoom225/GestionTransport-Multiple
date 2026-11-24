# Validation du carrousel à six secondes

La comparaison utilise le candidat `d74caeb` et la correction `3abe08b`. Chromium est lancé avec des API simulées. Les captures à 320 px montrent l’accueil au début du cycle.

- [Capture avant correction](../images/ui/carrousel-avant-320.jpg).
- [Capture après correction](../images/ui/carrousel-apres-320.jpg).

Les tests Angular vérifient précisément la transition entre 5 999 et 6 000 ms. Le navigateur vérifie l’avance automatique, les commandes de pause et reprise, le clavier, le mouvement réduit, l’onglet masqué et le visuel de secours. Après un clic de reprise, le script déplace le pointeur hors du carrousel pour que la pause au survol se termine.

Environnement de validation du 24 novembre 2026, Windows 11 et Chromium. Exécuter le script `node scripts/validation-ui.cjs` avec `UI_PHASE=after`, `UI_WIDTHS=320,375,768,1024,1440` et `UI_CAROUSEL=true`.

| Contrôle | Résultat |
| --- | --- |
| Vérifications responsive | 80 affichages des espaces public, voyageur et administration aux cinq largeurs, aucun débordement. Le script complet compte 95 affichages, sans débordement ni erreur JavaScript relevée. |
| Zoom natif à 200 % | Dix parcours vérifiés, aucun débordement. |
| Axe-core | Vingt analyses ciblées, aucune violation détectée dans ce périmètre. |
| Carrousel | Sept contrôles réussis. |
| Parcours principal | Recherche, fiche, demande, paiement simulé, confirmation et historique vérifiés avec des réponses API simulées. |

Cette preuve couvre un ensemble ciblé de scénarios. L’acceptation globale de l’accessibilité inclut une revue manuelle et des parcours réels.
