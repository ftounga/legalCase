# Pièce 05 — Photo de la porte forcée

## Spécifications techniques

- **Fichier final** : `05-porte-forcee.jpg`
- **Format** : JPG photo téléphone
- **Dimensions** : ratio 4:3 portrait, 3024 × 4032 environ
- **Qualité** : photo prise dans des conditions stressantes — flou de bougé léger, exposition pas optimale, mais lecture de la scène possible
- **Routage attendu dans la vidéo** : **Legal Vision**

## Rôle dans la vidéo

Deuxième des trois vignettes du **bloc 6 (48–62s)**. C'est le cas où l'image **n'a aucun texte exploitable** — seulement une scène à décrire et qualifier. Aucun OCR ne peut traiter cette pièce. Seul Legal Vision peut l'analyser.

## Description de la scène à composer ou photographier

La photo représente la **porte palière du domicile de Sophie MARTIN**, prise par elle-même dans la matinée du 4 mai 2026, juste après l'altercation et avant l'intervention du serrurier.

### Éléments visuels à inclure

1. **Porte d'appartement parisien classique** — bois clair (chêne ou hêtre verni), cadre style ancien, hauteur standard ~2m10
2. **Numéro d'appartement visible** — plaque métallique avec "12" ou "Apt 4" — élément qui ancre dans le réel
3. **Dégâts visibles** :
   - **Chambranle endommagé au niveau de la serrure** : le bois est fendu sur ~15 cm, éclats clairs visibles (bois fraîchement cassé, plus clair que le reste)
   - **Trace de coups répétés à hauteur d'épaule** (~1m50) : creux dans le bois, laque écaillée
   - **Trace de coup de pied bas de porte** (~30 cm du sol) : marque de semelle légèrement visible
   - **Serrure déformée** : la têtière de la serrure (plaque métallique) est légèrement sortie de son logement
4. **Tapis de palier** légèrement déplacé devant la porte — suggère mouvement violent
5. **Détail au sol** : un petit éclat de bois tombé sur le palier — détail qui authentifie la scène
6. **Lumière** : éclairage de palier (ampoule jaune classique) → couleurs chaudes, ombres marquées
7. **Cadrage** : photo prise du palier, en pied, montrant la porte entière du seuil au linteau

### Ce qu'on ne veut PAS

- Pas de photo de cinéma ou trop léchée
- Pas de Sophie dans le cadre (elle prend la photo, elle n'est pas dedans)
- Pas de sang, pas de violence explicite
- Pas de date affichée à l'écran (l'horodatage EXIF du fichier suffit pour la chronologie)

## Métadonnées EXIF à embarquer dans le fichier final

Le motion designer doit injecter dans le JPG des métadonnées EXIF cohérentes :
- Date de prise de vue : 2026-05-04 09:14:23
- Modèle d'appareil : iPhone 13 (ou équivalent générique)
- GPS : coordonnées d'un point fictif dans le 11ᵉ arrondissement de Paris (rue des Lilas, fictive — choisir un point quelconque sans bâtiment réel reconnaissable)

Ces métadonnées sont **utilisées par LegalCase** pour la chronologie automatique et seront affichées dans la vidéo.

## Démonstration bloc 6 — Legal Vision sur la photo

Sur la vignette photo de la porte (2ᵉ position dans les 3 vignettes côte à côte), apparaît une **carte de synthèse Legal Vision** :

```
┌──────────────────────────────────────────────────┐
│ 🚪 Photo — Porte palière du domicile             │
├──────────────────────────────────────────────────┤
│ Date EXIF : 04/05/2026 — 09:14                   │
│ GPS : 11ᵉ arrondissement de Paris                 │
│                                                  │
│ Description (Legal Vision) :                     │
│ Porte palière en bois présentant des dégâts      │
│ multiples — chambranle fendu au niveau de la     │
│ serrure (15 cm), trace de coups à hauteur        │
│ d'épaule, marque de semelle en bas de porte,     │
│ serrure partiellement déboîtée. Éclat de bois    │
│ tombé sur le palier. Compatible avec une         │
│ tentative d'effraction par coups d'épaule        │
│ et de pied.                                      │
│                                                  │
│ Source : 05-porte-forcee.jpg                     │
└──────────────────────────────────────────────────┘

Badge : Legal Vision (violet électrique)
```

Note : la qualification juridique ("tentative d'effraction") doit rester **prudente** dans la formulation Legal Vision — "compatible avec une tentative d'effraction" et non "preuve d'une tentative d'effraction". LegalCase décrit, l'avocat qualifie.

---

## Génération automatique de la pièce

### Méthode : prompt image generation (DALL-E 3 dans ChatGPT, ou MidJourney)

### Prompt à coller dans ChatGPT (DALL-E 3) — version française détaillée

```
Génère une photo réaliste prise au smartphone d'une porte palière
d'appartement parisien, montrant des dégâts d'effraction. Cadrage
portrait 4:3, plan en pied du sol au linteau, photo prise depuis le
palier face à la porte.

Élément central : porte d'appartement en bois clair (chêne ou hêtre
verni), style ancien parisien, hauteur ~2m10. Une plaque métallique
discrète avec le numéro "Apt 4" est fixée à hauteur du regard.

Dégâts visibles à représenter avec précision :
- Le chambranle (côté serrure, à droite de la porte) est FENDU sur
  environ 15 cm, avec des éclats de bois clair fraîchement cassé
  visibles (le bois cassé est plus clair que le reste qui est verni).
- Une trace de coup à hauteur d'épaule (~1m50 du sol) : un creux
  dans le bois, la laque écaillée, formant un impact ovale.
- Une marque de coup de pied en bas de porte (~30 cm du sol) : trace
  partielle de semelle de chaussure, légèrement visible sur le bois.
- La têtière de la serrure (plaque métallique latérale) est légèrement
  sortie de son logement, déboîtée.
- Un petit éclat de bois clair posé sur le tapis du palier devant la
  porte, comme tombé pendant les coups.
- Le tapis de palier est légèrement déplacé, formant un pli.

Environnement : palier d'immeuble parisien, mur de gauche peint en
beige clair ou ivoire, sol en parquet ancien ou carrelage type
mosaïque parisienne. Éclairage : ampoule jaune chaude classique de
palier, ombres marquées et chaudes.

Cadrage : la porte est centrée, légèrement décalée à droite. Photo
prise à main levée par une personne (Sophie MARTIN, hors champ),
légère inclinaison de 2°, qualité smartphone réelle (un peu de
flou de bougé sur les bords, photo nette au centre).

Style : photographie smartphone amateur prise dans des conditions
stressantes, pas une image stock professionnelle, aucun filtre.
Aucune personne visible, aucun visage. Aucun logo de marque, aucun
texte autre que le numéro "Apt 4" sur la plaque.

Pas de sang, pas de violence sur personne. La scène est vide,
seuls les dégâts matériels sont visibles.

Format de sortie : 3024 × 4032 pixels, ratio 4:3 vertical.
```

### Prompt MidJourney (version condensée)

```
realistic smartphone photo, Parisian apartment palière door, light oak
wood, "Apt 4" metal plate, doorframe split with 15cm crack and fresh
wood chips, shoulder-height impact mark with peeled lacquer, shoeprint
mark at lower part, lock plate dislodged, small wood chip on doormat,
slightly displaced rug, beige hallway wall, parisian mosaic floor,
warm yellow hallway light, harsh shadows, slight handheld blur,
amateur quality, no people, no blood, no logos, 4:3 portrait
--ar 3:4 --v 6 --s 200
```

### Itérations conseillées

1. Vérifier que les dégâts sont **localisés** (pas une porte entièrement détruite — c'est une tentative d'effraction qui n'a pas abouti)
2. Si l'IA crée une porte trop "moderne" ou trop neuve → préciser : "vintage Parisian apartment door, slightly worn varnish"
3. Si l'IA met une personne ou un visage dans le cadre → repréciser fermement : "ABSOLUTELY no humans, no faces, no body parts visible"
4. Garder la meilleure des 4 propositions et sauvegarder en `05-porte-forcee.jpg`

### Métadonnées EXIF à injecter (post-génération)

```bash
exiftool -DateTimeOriginal="2026:05:04 09:14:23" \
         -Make="Apple" \
         -Model="iPhone 13" \
         -GPSLatitude="48.858500" \
         -GPSLongitude="2.379800" \
         05-porte-forcee.jpg
```
