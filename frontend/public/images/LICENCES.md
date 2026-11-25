# Ressources visuelles Transport

`route-partagee.jpg` : Mike van Schoonderwalt, « A Road between Two Fields ».
Source : https://www.pexels.com/photo/a-road-between-two-fields-5505757/
Licence Pexels : https://www.pexels.com/license/ (consultée le 12 novembre 2026).
Version locale compressée, largeur 1200 px, 145 986 octets.
Image d'ambiance routière : elle ne représente ni un véhicule de la flotte ni un trajet disponible et sa localisation belge n'est pas affirmée.
L'illustration SVG locale demeure le secours en cas d'échec de chargement.

`mobilite-transport.svg` est une illustration vectorielle originale créée pour
ce projet : automobile générique, route et silhouettes urbaines, sans logo ni
photo d'un véhicule réel. Elle est décorative dans les bannières et les cartes.
La photographie Pexels ci-dessus reste utilisée comme fond de la présentation du service.

## Carrousel automobile — issue #29

Licence et pages sources vérifiées le 14 novembre 2026 :
[licence Pexels](https://www.pexels.com/license/) et
[conditions Pexels](https://www.pexels.com/terms-of-service/).
La licence autorise le téléchargement, l’utilisation sur ce site et les adaptations
de format/cadrage. Elle n’autorise pas à suggérer que les personnes ou les marques
représentées approuvent le service, à revendre les photos non modifiées ou à les
redistribuer comme banque d’images. Les crédits sont conservés même si leur mention
n’est pas imposée par cette licence.

| Fichiers locaux `car-{nom}-*.webp` | Photographe | Source Pexels |
| --- | --- | --- |
| `car-route-*` | Kelvin Linhares | [28502253 — berline sur une route](https://www.pexels.com/photo/sleek-silver-car-on-scenic-country-road-28502253/) |
| `car-famille-*` | Mikhail Nilov | [8323226 — famille près d’une voiture](https://www.pexels.com/photo/a-family-standing-near-parked-car-while-looking-at-the-camera-8323226/) |
| `car-partage-*` | Clément Proust | [18378381 — conducteur et passager](https://www.pexels.com/photo/people-traveling-on-a-road-18378381/) |
| `car-bruxelles-*` | Hakan Yasar | [14599371 — voitures dans une rue de Bruxelles](https://www.pexels.com/photo/cars-parked-along-street-14599371/) |

Chaque photographie possède trois versions paysage 640×360, 1024×576 et 1920×1080,
et une version mobile recadrée 640×800. Compression WebP qualité 0,8 et cadrages
reproductibles par `scripts/optimiser-photos-carousel.cjs`, avec Edge/Playwright
comme outillage externe au frontend. Aucune dépendance de production ajoutée.
Les originaux de travail ne sont pas publiés dans le dépôt.

Ces photos illustrent l’ambiance du service : elles ne représentent ni les véhicules
enregistrés, ni les personnes inscrites, ni des offres de trajet. Seule la photo de
Bruxelles est localisée en Belgique par sa source ; les autres n’ont pas cette attribution.
Les crédits de la photo courante sont consultables dans le carrousel, et les cartes
portent la mention « Photos d’illustration · hors flotte réelle ».
Le SVG original local demeure le secours en cas d’échec d’une photo du carrousel.

## Refonte premium

- `car-suv-{640,1024,1536,mobile}.webp` : SUV fictif sur une route en forêt.
- `car-electrique-{640,1024,1536,mobile}.webp` : voiture électrique fictive sur une route côtière.

Ces deux visuels ont été générés avec l’outil intégré ImageGen d’OpenAI pour cette interface, selon les [conditions d’utilisation d’OpenAI](https://openai.com/policies/terms-of-use/). Ils ne proviennent pas d’une photothèque et ne représentent aucun véhicule réservable. Le carrousel les signale comme visuels générés. Les autres diapositives sont la berline de Kelvin Linhares et la photo de Mikhail Nilov, sous licence Pexels et créditées dans l’interface.

Les variantes paysage sont recadrées en 16:9, la version mobile en 640×800. Compression WebP. Le visuel `car-suv-mobile.webp` pèse 71 046 octets.

Prompts ImageGen utilisés :

1. Use case: photorealistic-natural. Asset: wide automotive website banner photo. Fictional unbranded premium graphite silver SUV, believable proportions and clean bodywork, full vehicle clearly framed in right half, front three-quarter view on a quiet European paved road by fir trees and distant rocky slopes. Left third deep blue evergreen shadows and uncluttered road for overlay. Cool early morning, polished automotive editorial, physically plausible reflections. Landscape 16:9 framing, leave room around car for a mobile crop. No logos, no badge, blank plates, no text, no people, no watermark.
2. Use case: photorealistic-natural. Asset: wide automotive website banner photo. Fictional unbranded contemporary electric crossover in ice blue silver on a believable quiet European coastal paved road, full vehicle centered-right in front three-quarter view. Dark navy shadows left, low cliffs and pale blue sea in background, soft overcast cyan light, realistic wheels and reflections, professional automotive editorial. Landscape 16:9 with room for responsive crop. No logos, badge, text, people, or watermark.
