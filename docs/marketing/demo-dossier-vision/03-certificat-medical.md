# Pièce 03 — Certificat médical (photo téléphone dégradée)

## Spécifications techniques

- **Fichier final** : `03-certificat-medical.jpg`
- **Format** : JPG photo téléphone (pas un scan — vraie photo)
- **Dimensions** : ratio 4:3 portrait, résolution 3024 × 4032 environ (typique smartphone moderne)
- **Qualité visée** : **dégradée volontairement** — voir détails ci-dessous
- **Routage attendu dans la vidéo** :
  - **Legal OCR mode dégradé** au bloc 4 (28–38s)
  - **Édition par l'avocat** au bloc 5 (38–48s)

## Rôle dans la vidéo

Cette pièce porte **deux moments forts** de la vidéo :

1. **Bloc 4 — Legal OCR sur le dégueu** (28–38s) : démontrer que LegalCase extrait du texte structuré malgré la qualité catastrophique de la photo (cadrage, doigt visible, tampon écrasé, ombre)
2. **Bloc 5 — Édition de l'extrait par l'avocat** (38–48s) : démontrer que l'avocat **garde la main**. Une zone du certificat (la durée d'ITT) a été mal lue par le moteur OCR à cause d'un tampon qui chevauche le chiffre. L'avocat clique, voit l'aperçu original à côté, corrige, valide.

## Contenu textuel du certificat (à reproduire en photo)

Document à imprimer puis photographier en conditions dégradées :

```
                    Hôpital Saint-Antoine — APHP
              Service d'Accueil des Urgences (SAU)
              184 rue du Faubourg Saint-Antoine
                       75012 PARIS

                CERTIFICAT MÉDICAL INITIAL

Je soussigné, Docteur Anaïs DELAUNAY, médecin urgentiste,
inscrite au Conseil de l'Ordre sous le n° 75/123/456,

certifie avoir examiné ce jour, le 4 mai 2026 à 11h40,

Madame Sophie MARTIN
née le 8 mars 1991
demeurant 12 rue des Lilas, 75011 Paris,

qui déclare avoir été victime de violences au domicile
conjugal dans la nuit du 3 au 4 mai 2026.

LÉSIONS CONSTATÉES À L'EXAMEN
- Hématome contusiforme face postérieure du bras droit,
  diamètre 7 cm environ, coloration rouge-violacée
- Excoriation superficielle main droite (bord cubital)
- État de stress aigu — pleurs, tremblements,
  reviviscence des faits

EXAMENS COMPLÉMENTAIRES
- Radiographie bras droit : pas de lésion osseuse visible
- Examen neurologique : sans particularité

CONCLUSIONS
Au vu de l'examen clinique et de l'état psychologique
de Mme MARTIN, je fixe l'incapacité totale de travail
(ITT) au sens pénal à HUIT (8) JOURS.

Recommandation d'un suivi psychologique en CMP de secteur.

                            Fait à Paris, le 4 mai 2026
                            Dr Anaïs DELAUNAY
                            [signature manuscrite]
                            [tampon professionnel rond]
```

## Dégradations visuelles à appliquer (par le motion designer)

Cette photo est prise par Sophie MARTIN avec son téléphone, juste après être sortie des urgences. Elle est en état de choc. Les dégradations doivent l'illustrer :

1. **Cadrage** : le document n'est pas droit dans le cadre, légère inclinaison de 3-5°. Une marge importante autour du document (~20% de l'image), avec en arrière-plan un coin de table de café visible.
2. **Doigt visible** : un doigt en flou (premier plan) dans le coin inférieur gauche de l'image, qui mord légèrement sur le document — n'occulte aucun texte essentiel mais est clairement visible.
3. **Lumière** : éclairage néon dur (lumière de café ou de salle d'attente), créant une **zone de reflet blanc** dans le coin supérieur droit qui réduit le contraste sur ~10% du document.
4. **Tampon professionnel chevauchant** : le tampon rond du Dr DELAUNAY est apposé en bas du certificat. **Important** : il doit chevaucher partiellement le mot "**HUIT (8)**" dans la conclusion, de sorte que le chiffre 8 soit visuellement ambigu — pourrait être lu "B" ou "0" par un OCR naïf. C'est exactement ce qui justifiera la correction par l'avocat.
5. **Légère ombre de la main qui tient le téléphone** sur la moitié droite du document.
6. **Couleurs** : pas en noir et blanc — la photo capture les couleurs du papier (légèrement crème), de l'encre du tampon (bleu), et des marqueurs jaune fluo qu'on voit que Sophie a tracés à côté de "ITT 8 JOURS" et "violences au domicile conjugal" pendant qu'elle attendait.

## Démonstration bloc 4 (28–38s) — extraction OCR mode dégradé

Voix off (rappel) : *"Le client photographie en urgence. Le tampon est écrasé, le doigt dans le cadre. LegalCase l'extrait quand même."*

À l'écran :
- Zoom sur la photo dégradée (plein écran 1s)
- Animation d'extraction : un cadre de scan progresse de haut en bas
- Le texte structuré apparaît à droite, champs identifiés :
  - Médecin : Dr Anaïs DELAUNAY
  - N° ordre : 75/123/456
  - Date examen : 04/05/2026 à 11h40
  - Patiente : Sophie MARTIN
  - Date faits déclarés : 03-04/05/2026
  - Lésions : hématome bras droit, excoriation main droite, état de stress aigu
  - **ITT : "B JOURS"** ← l'extraction est volontairement fausse à ce stade

## Démonstration bloc 5 (38–48s) — édition par l'avocat

Voix off (rappel) : *"Vous gardez la main. Chaque extrait s'édite à côté de la pièce d'origine."*

À l'écran :
- **Split-screen 50/50** :
  - Gauche : photo originale du certificat, zoom automatique sur la zone du tampon qui chevauche "8 JOURS"
  - Droite : champs structurés extraits, avec le champ "ITT" surligné en orange (indicateur "Confiance faible")
- L'avocat (curseur) clique sur "B JOURS" → champ devient éditable
- Tape "8 JOURS"
- Valide
- **Le badge change** : doré "Pré-rempli par l'IA" → vert "Vérifié par l'avocat"

## Éléments à mettre en évidence

- Le badge **Confiance faible** (orange) sur le champ ITT avant correction est une **vraie fonctionnalité produit** — l'OCR remonte un score de confiance et signale les zones à vérifier. À montrer explicitement.
- L'aperçu zoomé synchronisé entre la pièce originale et le champ extrait est une **mécanique produit clé** — c'est elle qui rend la correction non frustrante.

---

## Génération automatique de la pièce

### Méthode : prompt image generation (DALL-E 3 dans ChatGPT, ou MidJourney)

Cette pièce nécessite une **image photoréaliste** d'un certificat médical photographié dans des conditions dégradées. Aucun script ne sait générer ça — il faut passer par un générateur d'image.

### Prompt à coller dans ChatGPT (DALL-E 3) — version française détaillée

```
Génère une photo réaliste prise au smartphone d'un certificat médical
français imprimé sur papier A4, posé sur une petite table en bois clair
de café ou de salle d'attente. Cadrage portrait 4:3, format vertical.

Le document occupe environ 75 % de l'image, légèrement incliné de 4°
vers la droite. En arrière-plan, on devine un coin de table avec une
texture de bois clair et un bord en métal.

Le certificat porte l'en-tête centré "Hôpital Saint-Antoine — APHP —
Service d'Accueil des Urgences (SAU)", suivi du titre "CERTIFICAT
MÉDICAL INITIAL" en majuscules, puis du corps du texte en français
mentionnant "Dr Anaïs DELAUNAY", "Madame Sophie MARTIN", "examen du
4 mai 2026", "hématome bras droit", "état de stress aigu" et une
conclusion qui fixe l'ITT à HUIT (8) JOURS.

Dégradations à appliquer obligatoirement :
- Un doigt humain en flou très net dans le coin inférieur GAUCHE,
  premier plan, bord du document légèrement mordu (ne pas occulter
  de texte essentiel).
- Un reflet de néon blanc/violacé qui crée une zone surexposée dans
  le coin SUPÉRIEUR DROIT (~15 % de l'image), éclairage de salle
  d'attente médicale.
- Un tampon rond bleu ENCRE BLEUE FONCÉE en bas, marqué "Dr A. DELAUNAY",
  apposé de travers, qui chevauche partiellement le chiffre "8" de la
  mention "HUIT (8) JOURS" — le 8 doit rester partiellement lisible
  mais ambigu (un OCR pourrait le confondre avec B ou 0).
- Une signature manuscrite stylisée bleue à côté du tampon.
- Deux marques de surligneur jaune fluo, tracées à la main (un peu
  bavées) : une autour de "ITT 8 JOURS", l'autre autour de "violences
  au domicile conjugal".
- Une légère ombre de la main qui tient le téléphone sur la moitié
  droite du document.
- Le papier est légèrement froissé sur les bords droits.

Couleurs : papier crème, encre noire pour le corps de texte, encre
bleue pour le tampon et la signature, jaune fluo vif pour les
surlignages, table en bois clair en arrière-plan.

Style : photographie smartphone amateur, pas une image stock léchée.
Mise au point sur le centre du document, légère perte de netteté sur
les bords. Pas de filtre Instagram. Aucun watermark. Aucun logo de
marque réel. Pas de visage humain visible.

Format de sortie : 3024 × 4032 pixels, ratio 4:3 vertical.
```

### Prompt MidJourney (version condensée alternative)

```
realistic smartphone photo of a French medical certificate on a wooden
café table, slight 4° tilt, blurred finger in lower-left corner mordant
the document, harsh neon reflection in upper-right corner, blue round
medical stamp partially overlapping the number "8" in the conclusion,
yellow fluo highlighter marks around "ITT 8 JOURS", manuscript blue
signature, slight wrinkles on right edges, hand shadow on right half,
amateur smartphone quality, no filter, no logos, no humans, 4:3 portrait
--ar 3:4 --v 6 --s 200
```

### Itérations conseillées

1. Première génération → vérifier que le tampon chevauche bien le "8" (point critique pour la démo édition au bloc 5)
2. Si l'IA génère un texte illisible (artefacts) → préciser dans le prompt : "Le texte du certificat doit être complètement lisible et déchiffrable, sauf le chiffre 8 qui est partiellement masqué par le tampon."
3. Si pas de doigt visible → préciser "A clearly visible blurred human finger in the lower-left foreground, partially over the document edge."
4. Garder la meilleure des 4 propositions DALL-E 3 et la sauvegarder en `03-certificat-medical.jpg` (qualité 90, sRGB).

### Métadonnées EXIF à injecter (post-génération)

```bash
exiftool -DateTimeOriginal="2026:05:04 12:48:11" \
         -Make="Apple" \
         -Model="iPhone 13" \
         -GPSLatitude="48.857000" \
         -GPSLongitude="2.380000" \
         03-certificat-medical.jpg
```
