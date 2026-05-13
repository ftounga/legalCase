# Pièce 07 — Facture du serrurier (photo papier froissé)

## Spécifications techniques

- **Fichier final** : `07-facture-serrurier.jpg`
- **Format** : JPG photo téléphone
- **Dimensions** : ratio 4:3, environ 3024 × 4032
- **Qualité** : photo prise à la va-vite d'un papier froissé sur une table — luminosité difficile, papier qui ne tient pas à plat, ombres
- **Routage attendu dans la vidéo** : **Legal OCR mode dégradé**

## Rôle dans la vidéo

Cette pièce passe rapidement dans le bloc 2 (moteur de routage, 10–18s) puis revient dans la **chronologie** (bloc 7, 62–70s). Son rôle est d'**ancrer la temporalité** : la facture porte la date et l'heure d'intervention du serrurier (4 mai 2026 au matin), ce qui corrobore la nuit des faits et confirme l'urgence.

C'est le contrepoint budget de la pièce 03 — toutes deux sont des **photos téléphone dégradées**, qui passent par Legal OCR mode dégradé. Ensemble elles posent que LegalCase tient sur le réel d'un dossier en panique.

## Contenu textuel de la facture (à reproduire)

```
                  SERRURERIE EXPRESS PARIS
                  24 boulevard Voltaire
                       75011 PARIS
                  Tél : 01.XX.XX.XX.XX
                  SIRET : 812 456 789 00012

─────────────────────────────────────────────

FACTURE N° 2026-0847
Date : 4 mai 2026
Heure d'intervention : 10h45 — 12h20

CLIENT
Mme Sophie MARTIN
12 rue des Lilas
75011 Paris

INTERVENTION
Adresse : 12 rue des Lilas, 75011 Paris (Apt 4, 3ᵉ étage)
Motif : intervention d'urgence — remplacement
serrure suite à tentative d'effraction

DÉTAIL
- Déplacement urgence (intervention < 2h)         95,00 €
- Démontage serrure endommagée                    45,00 €
- Pose serrure 5 points A2P***                   289,00 €
  (Marque Picard ou équivalent — 3 clés fournies)
- Réparation chambranle (mastic bois + ponçage)   75,00 €
- Main d'œuvre (1h35)                            110,00 €

                              ─────────────
                              Sous-total HT  614,00 €
                              TVA 20 %       122,80 €
                              ─────────────
                              TOTAL TTC      736,80 €

Réglé en CB ce jour — 4 mai 2026
Merci de votre confiance.

                              [tampon "PAYÉ"]
                              [signature serrurier]
```

## Dégradations visuelles à appliquer

1. **Papier froissé** : la facture a été pliée en 4, puis dépliée à la hâte. Les plis sont visibles, formant une croix au milieu du document. Légères marques d'usure aux coins.
2. **Cadrage** : photo prise du dessus, à main levée, légère inclinaison de 4-6°. Une partie du dessous (table, peut-être un coin de mug, un trousseau de clés) est visible en bordure.
3. **Lumière** : éclairage naturel mais pas idéal — peut-être de la lumière du jour qui crée un contraste sur la moitié droite du papier.
4. **Ombre** : ombre portée du téléphone qui prend la photo, visible dans le tiers inférieur gauche.
5. **Le tampon "PAYÉ" rouge en bas** est mal aligné, se chevauche partiellement avec le total TTC.
6. **Léger flou de bougé** sur la zone du SIRET (la photo n'est pas parfaitement nette partout).

## Démonstration dans la vidéo

### Bloc 2 (moteur de routage, 10–18s)

Cette pièce traverse le convoyeur en compagnie des autres et reçoit le badge **Legal OCR — mode dégradé** (badge indigo nuit avec un petit indicateur "qualité dégradée").

### Bloc 7 (chronologie + source cliquable, 62–70s)

Dans la timeline du dossier MARTIN qui se construit à l'écran, on voit apparaître l'événement :

```
04/05 10:45 — Intervention serrurier d'urgence
              (remplacement serrure 5 points)
              Source : 07-facture-serrurier.jpg
              Coût : 736,80 € — réglé CB
```

Quand le curseur clique dessus → l'écran zoome sur la facture originale (pièce 07 ouverte en aperçu plein écran, avec ses froissements visibles), illustrant la **mécanique source cliquable** depuis la chronologie.

C'est le moment qui montre qu'aucune information dans le dossier n'est "coupée de sa source". Tout reste relié.

## Données extraites attendues à l'écran

Champs structurés que LegalCase doit afficher après extraction OCR :

- Société : SERRURERIE EXPRESS PARIS
- N° facture : 2026-0847
- Date : 04/05/2026
- Heure intervention : 10h45 – 12h20
- Adresse : 12 rue des Lilas, 75011 Paris
- Motif : remplacement serrure suite tentative d'effraction
- Total TTC : 736,80 €
- Mode de règlement : CB

---

## Génération automatique de la pièce

### Méthode : prompt image generation (DALL-E 3 dans ChatGPT, ou MidJourney)

### Prompt à coller dans ChatGPT (DALL-E 3) — version française détaillée

```
Génère une photo réaliste prise au smartphone d'une facture papier A4
française d'un serrurier, posée sur une table de cuisine ou de salon.
Cadrage portrait 4:3.

Le document occupe environ 80 % de l'image, vu du dessus, avec une
légère inclinaison de 5° et une vue plongeante (l'utilisateur est
debout au-dessus de la table).

La facture porte l'en-tête centré "SERRURERIE EXPRESS PARIS — 24
boulevard Voltaire — 75011 PARIS — Tél : 01.XX.XX.XX.XX". Le titre
"FACTURE N° 2026-0847" suivi de la date "4 mai 2026" et "Heure
d'intervention : 10h45 — 12h20".

Le bloc CLIENT mentionne "Mme Sophie MARTIN, 12 rue des Lilas, 75011
Paris". Le motif d'intervention indique "remplacement serrure suite à
tentative d'effraction".

Le tableau du détail montre 5 lignes :
- Déplacement urgence — 95,00 €
- Démontage serrure endommagée — 45,00 €
- Pose serrure 5 points A2P*** — 289,00 €
- Réparation chambranle — 75,00 €
- Main d'œuvre — 110,00 €

Le total TTC affiche 736,80 €.

Dégradations à appliquer :
- Le papier est NETTEMENT FROISSÉ : il a été plié en quatre puis
  déplié, formant une CROIX VISIBLE de plis au milieu du document.
- Légères marques d'usure et froissement aux quatre coins.
- Le document est posé sur une table en bois clair ou stratifié blanc.
- Une partie du dessous (un coin de mug en céramique blanche, un
  trousseau de clés en métal) est visible en bordure inférieure
  gauche de l'image.
- Lumière naturelle de jour mais déséquilibrée : la moitié droite
  est légèrement surexposée par une lumière de fenêtre.
- Une ombre portée du téléphone (qui prend la photo) s'étend dans
  le tiers inférieur gauche du document.
- Un tampon rouge "PAYÉ" mal aligné, légèrement de travers, qui
  chevauche partiellement la ligne du total TTC en bas du tableau.
- Une signature manuscrite stylisée bleue à côté du tampon "PAYÉ".
- Un léger flou de bougé visible sur la zone du SIRET en bas de la
  facture (la photo n'est pas parfaitement nette partout).

Couleurs : papier blanc cassé légèrement jauni, encre noire pour
le corps, encre rouge pour le tampon "PAYÉ", encre bleue pour la
signature, table en bois clair en arrière-plan.

Style : photographie smartphone amateur, prise rapidement dans des
conditions de luminosité difficiles, pas une image stock léchée.
Aucun filtre. Aucun watermark. Aucun logo de marque réel. Pas de
visage humain.

Format de sortie : 3024 × 4032 pixels, ratio 4:3 vertical.
```

### Prompt MidJourney (version condensée)

```
realistic smartphone photo of a French locksmith invoice (FACTURE) on
a light wood table, heavily wrinkled paper with visible cross folds
from being folded in quarters, slight 5° tilt, top-down view, partial
view of a white mug and keys at lower-left edge, daylight uneven with
overexposed right half, phone shadow on lower-left, red "PAYÉ" stamp
slightly tilted overlapping the total line (736,80 €), blue manuscript
signature, slight motion blur on SIRET line, total TTC 736,80 €,
amateur smartphone quality, no filter, no logos, no humans, 4:3 portrait
--ar 3:4 --v 6 --s 200
```

### Itérations conseillées

1. Vérifier que **les plis en croix** sont bien visibles — c'est le détail qui ancre le réalisme du papier froissé
2. Si l'IA produit une facture trop propre → forcer : "The paper is heavily wrinkled and creased, with visible fold lines forming a cross pattern in the middle"
3. Si le tampon "PAYÉ" est mal placé → préciser : "Red 'PAYÉ' stamp positioned in the bottom-right area, slightly rotated, overlapping the total amount line"
4. Garder la meilleure des 4 propositions et sauvegarder en `07-facture-serrurier.jpg`

### Métadonnées EXIF à injecter (post-génération)

```bash
exiftool -DateTimeOriginal="2026:05:04 12:35:42" \
         -Make="Apple" \
         -Model="iPhone 13" \
         -GPSLatitude="48.858100" \
         -GPSLongitude="2.379600" \
         07-facture-serrurier.jpg
```
