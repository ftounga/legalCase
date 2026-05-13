# Pièce 02 — Main courante du commissariat (PDF multi-documents)

## Spécifications techniques

- **Fichier final** : `02-main-courante-commissariat.pdf`
- **Format** : PDF scanné, **10 pages au total**, contenant **4 sous-pièces distinctes**
- **Dimensions** : A4 portrait
- **Résolution** : 200 DPI (scan administratif standard, pas optimal mais lisible)
- **Qualité** : noir et blanc, légèrement contrasté, parfois inclinaison de 1-2°, points de poussière de scanner visibles
- **Routage attendu dans la vidéo** : **Legal OCR** + démonstration de la **détection de sous-pièces**

## Rôle dans la vidéo

Cette pièce est le héros du **bloc 3 (18–28s)** — détection automatique de sous-pièces. La cliente a déposé un seul PDF que le commissariat lui a remis (10 pages agrafées qu'elle a scannées d'un coup). LegalCase doit identifier que ce PDF contient **4 documents distincts** et les séparer en cartes individuelles.

Animation à l'écran : le PDF "se déplie" comme un éventail en 4 cartes filles, chacune avec son propre badge de catégorie et son propre badge Legal OCR.

## Composition du PDF (10 pages, 4 sous-pièces)

### Sous-pièce A — Procès-verbal de dépôt de plainte (pages 1-3)

**Catégorie détectée** : Procès-verbal
**Caractéristique visuelle** : en-tête "POLICE NATIONALE — Commissariat du 11ᵉ arrondissement", cadre administratif, références numérotées en marge, formulaire pré-imprimé rempli à la machine.

Contenu textuel :

```
POLICE NATIONALE
Direction Territoriale de la Sécurité de Proximité de Paris
Commissariat de Police du 11ᵉ arrondissement
12-14 passage Charles-Dallery — 75011 PARIS

PROCÈS-VERBAL DE DÉPÔT DE PLAINTE
Réf : 2026/PJ/05/0847

L'an deux mille vingt-six, le quatre mai à neuf heures vingt-cinq,
Nous, Brigadier-chef Pascal MOREAU, OPJ,
en notre commissariat, recevons :

DÉCLARANTE
Nom : MARTIN
Prénom : Sophie, Élise
Née le : 08/03/1991 à Nantes (44)
Nationalité : française
Profession : infirmière
Domicile : 12 rue des Lilas, 75011 Paris
Téléphone : 06.XX.XX.XX.XX

NATURE DES FAITS
Violences conjugales — tentative d'effraction du domicile
Faits commis dans la nuit du 3 au 4 mai 2026 vers 23h10,
au domicile de la déclarante.

DÉCLARATION DE LA VICTIME
"Mon mari, M. Julien VASSEUR, contre lequel j'ai pris des
distances depuis trois semaines, est venu au domicile conjugal
hier soir vers 23h. Je lui avais demandé par message de ne
pas venir. Il a sonné, j'ai refusé d'ouvrir. Il a alors tenté
de forcer la porte palière à plusieurs reprises, en donnant
des coups d'épaule et de pied. La porte a cédé partiellement
au niveau du chambranle. Je me suis réfugiée chez ma voisine
de palier, Mme LECLERC, qui a appelé Police-Secours. Mon mari
est parti avant l'arrivée des fonctionnaires. Je joins à la
présente plainte des messages que j'ai reçus de mon mari dans
les jours précédents, dont certains contiennent des menaces
explicites."

OBSERVATIONS DE L'OPJ
La déclarante présente un état d'agitation et un hématome
visible au bras droit. Elle est invitée à consulter sans délai
un médecin pour constatation médicale et délivrance d'un
certificat. La porte du domicile présente effectivement des
traces de forcement, constatées sur photos versées au dossier.

Lecture faite, la déclarante persiste et signe.

Signature de la déclarante : [signature manuscrite]
Signature de l'OPJ : [signature et tampon]
```

### Sous-pièce B — Feuillet de déposition manuscrit complémentaire (pages 4-5)

**Catégorie détectée** : Déposition complémentaire
**Caractéristique visuelle** : feuillet papier libre, écriture manuscrite (déposition que la cliente a rédigée elle-même au commissariat), parfois ratures, soulignements au stylo.

Contenu (à reproduire en écriture manuscrite stylisée) :

```
Précisions complémentaires apportées par mes soins,
le 4 mai 2026 au commissariat.

Je tiens à préciser que mon mari avait déjà eu un comportement
agressif le 18 avril 2026, en cassant un verre lors d'une
dispute. Je ne l'avais pas signalé à l'époque pensant qu'il
s'agissait d'un événement isolé.

Notre voisin du dessus, M. ROCHE, dispose d'une caméra de
sonnette qui filme le palier. Il m'a indiqué qu'il pourrait
me transmettre la vidéo des faits du 3 mai au soir.

J'ai dû faire intervenir un serrurier en urgence dans la
matinée du 4 mai pour remplacer la serrure de ma porte.

                                Sophie MARTIN
                                [signature]
```

### Sous-pièce C — Photocopie de la carte nationale d'identité (page 6)

**Catégorie détectée** : Pièce d'identité
**Caractéristique visuelle** : photocopie noir et blanc d'une CNI française, qualité moyenne (texte légèrement flou, photo d'identité reconnaissable mais peu nette).

Contenu de la CNI (recto + verso sur la même page) :

```
RÉPUBLIQUE FRANÇAISE
CARTE NATIONALE D'IDENTITÉ

Nom : MARTIN
Prénoms : Sophie, Élise
Né(e) le : 08.03.1991 à NANTES (44)
Sexe : F
Taille : 1m68
Adresse : 12 RUE DES LILAS — 75011 PARIS

N° de la carte : XXXXXXXXXXXXXX
Délivrée le : 14.06.2019
Expire le : 13.06.2029

Préfecture : PARIS

[photo d'identité noir et blanc, photocopie]
[bandes MRZ en bas]
```

### Sous-pièce D — Formulaire de plainte signé (pages 7-10)

**Catégorie détectée** : Formulaire de plainte
**Caractéristique visuelle** : formulaire administratif type Cerfa, cases cochées, champs remplis à la main, en-tête République Française, cadre en bas pour signature et tampon du commissariat.

Contenu : reprise structurée des faits déjà décrits dans le PV (sous-pièce A), au format formulaire avec :
- Cases pré-imprimées : "Violences" ☒, "Menaces" ☒, "Tentative d'effraction" ☒
- Champ "Date des faits" : 03-04 mai 2026
- Champ "Lieu" : 12 rue des Lilas, 75011 Paris
- Champ "Auteur(s) présumé(s)" : VASSEUR Julien (lien matrimonial : époux)
- Signature manuscrite Sophie MARTIN
- Tampon du commissariat
- Signature OPJ

## Éléments à mettre en évidence pour la vidéo

Quand le PDF arrive dans le moteur de routage (bloc 3, 18–28s) :

1. **Vignette initiale** : "main-courante-commissariat.pdf — 10 pages — 1 fichier"
2. **Animation** : le PDF s'ouvre, une analyse rapide passe page par page (curseur d'analyse stylisé), puis le PDF se "déplie" en 4 cartes filles côte à côte
3. **Chaque carte fille** affiche :
   - Son numéro (A / B / C / D)
   - Sa catégorie auto-détectée (Procès-verbal / Déposition complémentaire / Pièce d'identité / Formulaire de plainte)
   - Son range de pages (1-3 / 4-5 / 6 / 7-10)
   - Son badge **Legal OCR** (indigo nuit)
4. **Compteur final** : "1 PDF déposé → 4 pièces identifiées et indexées séparément"

Voix off du bloc 3 (rappel) : *"Et quand un document en contient plusieurs, LegalCase les sépare."*

---

## Génération automatique de la pièce

### Méthode : 4 fichiers HTML rendus en PDF, puis fusionnés en un PDF unique de 10 pages

Cette pièce a deux étapes distinctes :

1. **Étape 1 — génération du PDF natif des 4 sous-pièces** (script HTML → PDF + fusion) : produit un PDF natif propre de 10 pages.
2. **Étape 2 — application de l'effet "scan administratif"** (post-traitement par le motion designer) : transforme le PDF natif en simulation de scan (légère inclinaison, bruit de scanner, baisse de contraste).

### Étape 1 — Génération des 4 sous-pièces

#### Code HTML — `02a-pv-plainte.html` (sous-pièce A, 3 pages)

```html
<!DOCTYPE html>
<html lang="fr"><head><meta charset="UTF-8"><title>PV plainte</title>
<style>
  @page { size: A4; margin: 22mm; }
  body { font-family: 'Courier New', Courier, monospace; font-size: 10.5pt; line-height: 1.55; color: #111; }
  .header { text-align: center; border-bottom: 1.5px solid #000; padding-bottom: 4mm; margin-bottom: 6mm; }
  .header h1 { font-size: 12pt; font-weight: bold; letter-spacing: 1px; }
  .header .sub { font-size: 9pt; line-height: 1.3; }
  .ref { text-align: right; font-size: 9pt; margin-bottom: 6mm; }
  h2 { text-align: center; font-size: 12pt; letter-spacing: 1.5px; margin: 8mm 0 4mm; text-decoration: underline; }
  .section { margin-bottom: 5mm; }
  .section h3 { font-size: 10.5pt; font-weight: bold; letter-spacing: 1px; margin-bottom: 2mm; }
  .field-line { margin: 1mm 0; }
  .quote { background: #f4f4f4; padding: 4mm 6mm; border-left: 3px solid #888; margin: 3mm 0; font-style: italic; }
  .signature-block { margin-top: 12mm; display: flex; justify-content: space-between; }
  .signature-block .sig { font-family: 'Brush Script MT', cursive; font-size: 16pt; }
</style></head><body>
<div class="header">
  <div class="sub">POLICE NATIONALE</div>
  <div class="sub">Direction Territoriale de la Sécurité de Proximité de Paris</div>
  <h1>Commissariat de Police du 11ᵉ arrondissement</h1>
  <div class="sub">12-14 passage Charles-Dallery — 75011 PARIS</div>
</div>

<h2>PROCÈS-VERBAL DE DÉPÔT DE PLAINTE</h2>
<div class="ref">Réf : 2026/PJ/05/0847</div>

<p>L'an deux mille vingt-six, le quatre mai à neuf heures vingt-cinq, Nous, Brigadier-chef Pascal MOREAU, OPJ, en notre commissariat, recevons :</p>

<div class="section">
  <h3>DÉCLARANTE</h3>
  <div class="field-line">Nom : MARTIN</div>
  <div class="field-line">Prénom : Sophie, Élise</div>
  <div class="field-line">Née le : 08/03/1991 à Nantes (44)</div>
  <div class="field-line">Nationalité : française</div>
  <div class="field-line">Profession : infirmière</div>
  <div class="field-line">Domicile : 12 rue des Lilas, 75011 Paris</div>
</div>

<div class="section">
  <h3>NATURE DES FAITS</h3>
  <p>Violences conjugales — tentative d'effraction du domicile. Faits commis dans la nuit du 3 au 4 mai 2026 vers 23h10, au domicile de la déclarante.</p>
</div>

<div class="section">
  <h3>DÉCLARATION DE LA VICTIME</h3>
  <div class="quote">
    « Mon mari, M. Julien VASSEUR, contre lequel j'ai pris des distances depuis trois semaines, est venu au domicile conjugal hier soir vers 23h. Je lui avais demandé par message de ne pas venir. Il a sonné, j'ai refusé d'ouvrir. Il a alors tenté de forcer la porte palière à plusieurs reprises, en donnant des coups d'épaule et de pied. La porte a cédé partiellement au niveau du chambranle. Je me suis réfugiée chez ma voisine de palier, Mme LECLERC, qui a appelé Police-Secours. Mon mari est parti avant l'arrivée des fonctionnaires. Je joins à la présente plainte des messages que j'ai reçus de mon mari dans les jours précédents, dont certains contiennent des menaces explicites. »
  </div>
</div>

<div class="section">
  <h3>OBSERVATIONS DE L'OPJ</h3>
  <p>La déclarante présente un état d'agitation et un hématome visible au bras droit. Elle est invitée à consulter sans délai un médecin pour constatation médicale et délivrance d'un certificat. La porte du domicile présente effectivement des traces de forcement, constatées sur photos versées au dossier.</p>
</div>

<p>Lecture faite, la déclarante persiste et signe.</p>

<div class="signature-block">
  <div>Signature de la déclarante :<br><span class="sig">S. Martin</span></div>
  <div>Signature de l'OPJ :<br><span class="sig">P. Moreau</span></div>
</div>
</body></html>
```

#### Code HTML — `02b-deposition-manuscrite.html` (sous-pièce B, 2 pages)

```html
<!DOCTYPE html>
<html lang="fr"><head><meta charset="UTF-8"><title>Déposition</title>
<style>
  @page { size: A4; margin: 22mm; }
  body { font-family: 'Caveat', 'Bradley Hand', 'Comic Sans MS', cursive; font-size: 16pt; line-height: 1.6; color: #1a1a4a; }
  .top-line { font-style: italic; margin-bottom: 8mm; }
  p { margin-bottom: 5mm; }
  .signature { text-align: right; margin-top: 16mm; font-family: 'Brush Script MT', cursive; font-size: 22pt; }
</style></head><body>
<div class="top-line">Précisions complémentaires apportées par mes soins, le 4 mai 2026 au commissariat.</div>

<p>Je tiens à préciser que mon mari avait déjà eu un comportement agressif le 18 avril 2026, en cassant un verre lors d'une dispute. Je ne l'avais pas signalé à l'époque pensant qu'il s'agissait d'un événement isolé.</p>

<p>Notre voisin du dessus, M. ROCHE, dispose d'une caméra de sonnette qui filme le palier. Il m'a indiqué qu'il pourrait me transmettre la vidéo des faits du 3 mai au soir.</p>

<p>J'ai dû faire intervenir un serrurier en urgence dans la matinée du 4 mai pour remplacer la serrure de ma porte.</p>

<div class="signature">Sophie MARTIN<br>S. Martin</div>
</body></html>
```

> Pour la sous-pièce B, l'idéal est d'utiliser une police manuscrite type **Caveat** (Google Fonts gratuite). Si la police n'est pas disponible localement, ajouter en haut du HTML : `<link href="https://fonts.googleapis.com/css2?family=Caveat&display=swap" rel="stylesheet">`

#### Code HTML — `02c-cni.html` (sous-pièce C, 1 page)

```html
<!DOCTYPE html>
<html lang="fr"><head><meta charset="UTF-8"><title>CNI</title>
<style>
  @page { size: A4; margin: 25mm; }
  body { font-family: Arial, sans-serif; font-size: 10pt; color: #444; filter: grayscale(1) contrast(0.85); }
  .photocopie-note { text-align: center; font-size: 9pt; color: #888; margin-bottom: 8mm; font-style: italic; }
  .cni { width: 130mm; margin: 0 auto; border: 1.5px solid #555; padding: 5mm; background: #f8f8f0; }
  .cni-header { text-align: center; font-weight: bold; font-size: 11pt; letter-spacing: 1px; border-bottom: 1px solid #555; padding-bottom: 2mm; margin-bottom: 4mm; }
  .cni-body { display: flex; gap: 4mm; }
  .cni-photo { width: 30mm; height: 38mm; background: linear-gradient(135deg, #aaa, #777); border: 1px solid #555; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 8pt; text-align: center; padding: 2mm; }
  .cni-fields { flex: 1; font-size: 9pt; line-height: 1.5; }
  .field-line { display: flex; }
  .field-line .label { width: 28mm; color: #666; }
  .mrz { font-family: 'Courier New', monospace; font-size: 9pt; letter-spacing: 1px; margin-top: 4mm; padding-top: 3mm; border-top: 1px dashed #555; word-break: break-all; }
</style></head><body>
<div class="photocopie-note">— Photocopie noir et blanc, qualité moyenne —</div>
<div class="cni">
  <div class="cni-header">RÉPUBLIQUE FRANÇAISE — CARTE NATIONALE D'IDENTITÉ</div>
  <div class="cni-body">
    <div class="cni-photo">[ photo<br>identité<br>noir et blanc ]</div>
    <div class="cni-fields">
      <div class="field-line"><span class="label">Nom :</span><span>MARTIN</span></div>
      <div class="field-line"><span class="label">Prénoms :</span><span>Sophie, Élise</span></div>
      <div class="field-line"><span class="label">Né(e) le :</span><span>08.03.1991 à NANTES (44)</span></div>
      <div class="field-line"><span class="label">Sexe :</span><span>F</span></div>
      <div class="field-line"><span class="label">Taille :</span><span>1m68</span></div>
      <div class="field-line"><span class="label">Adresse :</span><span>12 RUE DES LILAS — 75011 PARIS</span></div>
      <div class="field-line"><span class="label">N° carte :</span><span>XXXXXXXXXXXXXX</span></div>
      <div class="field-line"><span class="label">Délivrée le :</span><span>14.06.2019</span></div>
      <div class="field-line"><span class="label">Expire le :</span><span>13.06.2029</span></div>
      <div class="field-line"><span class="label">Préfecture :</span><span>PARIS</span></div>
    </div>
  </div>
  <div class="mrz">IDFRAMARTIN&lt;&lt;SOPHIE&lt;ELISE&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;<br>910308F2906137FRA&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;</div>
</div>
</body></html>
```

#### Code HTML — `02d-formulaire-plainte.html` (sous-pièce D, 4 pages)

```html
<!DOCTYPE html>
<html lang="fr"><head><meta charset="UTF-8"><title>Formulaire plainte</title>
<style>
  @page { size: A4; margin: 20mm; }
  body { font-family: Arial, sans-serif; font-size: 10pt; color: #111; }
  .header-bandeau { background: #1c4a8a; color: #fff; padding: 4mm 6mm; font-weight: bold; font-size: 11pt; letter-spacing: 1px; margin-bottom: 6mm; }
  h2 { text-align: center; font-size: 12pt; letter-spacing: 2px; margin: 4mm 0 6mm; }
  .form-block { border: 1px solid #555; padding: 4mm 5mm; margin-bottom: 4mm; }
  .form-block h3 { font-size: 10.5pt; background: #e9eff5; padding: 1mm 2mm; margin: -4mm -5mm 3mm; }
  .checkbox-line { margin: 1.5mm 0; }
  .checkbox-line .box { display: inline-block; width: 4mm; height: 4mm; border: 1.2px solid #333; text-align: center; vertical-align: middle; line-height: 3.5mm; font-size: 8pt; margin-right: 2mm; }
  .checkbox-line .checked::after { content: "✗"; font-weight: bold; }
  .field-row { margin: 2mm 0; }
  .field-row .label { font-weight: bold; }
  .field-row .input { display: inline-block; border-bottom: 1px solid #777; min-width: 60mm; padding: 0 2mm; }
  .signature-zone { margin-top: 14mm; display: flex; justify-content: space-between; align-items: flex-end; }
  .signature-zone .box { width: 60mm; height: 28mm; border: 1px solid #888; padding: 2mm; font-size: 8pt; color: #888; }
  .signature-zone .sig { font-family: 'Brush Script MT', cursive; font-size: 16pt; color: #1a1a3a; }
  .tampon { width: 36mm; height: 36mm; border: 2px dashed #1c4a8a; border-radius: 50%; color: #1c4a8a; text-align: center; font-size: 7pt; padding: 6mm 2mm; line-height: 1.2; opacity: 0.8; transform: rotate(-5deg); }
</style></head><body>
<div class="header-bandeau">RÉPUBLIQUE FRANÇAISE — Cerfa n° 12345*06</div>
<h2>PLAINTE — VIOLENCES CONJUGALES</h2>

<div class="form-block">
  <h3>1 — Identification du déclarant</h3>
  <div class="field-row"><span class="label">Nom :</span> <span class="input">MARTIN</span></div>
  <div class="field-row"><span class="label">Prénoms :</span> <span class="input">Sophie, Élise</span></div>
  <div class="field-row"><span class="label">Né(e) le :</span> <span class="input">08/03/1991 à Nantes (44)</span></div>
  <div class="field-row"><span class="label">Adresse :</span> <span class="input">12 rue des Lilas, 75011 Paris</span></div>
</div>

<div class="form-block">
  <h3>2 — Nature des faits dénoncés</h3>
  <div class="checkbox-line"><span class="box checked"></span> Violences (art. 222-13 CP)</div>
  <div class="checkbox-line"><span class="box checked"></span> Menaces (art. 222-17 CP)</div>
  <div class="checkbox-line"><span class="box checked"></span> Tentative d'effraction du domicile</div>
  <div class="checkbox-line"><span class="box"></span> Autre (préciser) : ...........</div>
</div>

<div class="form-block">
  <h3>3 — Date et lieu des faits</h3>
  <div class="field-row"><span class="label">Date :</span> <span class="input">03-04 mai 2026</span></div>
  <div class="field-row"><span class="label">Lieu :</span> <span class="input">12 rue des Lilas, 75011 Paris</span></div>
</div>

<div class="form-block">
  <h3>4 — Auteur(s) présumé(s)</h3>
  <div class="field-row"><span class="label">Nom :</span> <span class="input">VASSEUR Julien</span></div>
  <div class="field-row"><span class="label">Lien avec la déclarante :</span> <span class="input">époux</span></div>
</div>

<div class="signature-zone">
  <div>
    <div>Signature de la déclarante :</div>
    <div class="sig">S. Martin</div>
  </div>
  <div class="tampon">
    POLICE NATIONALE<br>
    11ᵉ ARR. PARIS<br>
    ★<br>
    OPJ
  </div>
</div>
</body></html>
```

### Étape 1 — Commandes de génération + fusion

```bash
# Rendu PDF de chaque sous-pièce
for f in 02a-pv-plainte 02b-deposition-manuscrite 02c-cni 02d-formulaire-plainte; do
  chromium --headless --disable-gpu --no-sandbox \
    --print-to-pdf=$f.pdf --no-pdf-header-footer $f.html
done

# Fusion en un PDF unique (poppler-utils requis)
sudo apt install poppler-utils  # si pas déjà installé
pdfunite 02a-pv-plainte.pdf 02b-deposition-manuscrite.pdf \
         02c-cni.pdf 02d-formulaire-plainte.pdf \
         02-main-courante-commissariat-NATIF.pdf

# Vérifier le nombre de pages (doit être 10 ou proche)
pdfinfo 02-main-courante-commissariat-NATIF.pdf | grep Pages
```

### Étape 2 — Application de l'effet "scan administratif"

Le PDF natif obtenu est trop propre. Pour qu'il ressemble à un scan administratif, le motion designer applique une de ces transformations :

#### Option A — ImageMagick (rapide, ligne de commande)

```bash
# Convertir le PDF en images, dégrader, reconstruire en PDF
pdftoppm -r 200 02-main-courante-commissariat-NATIF.pdf page -jpeg

# Pour chaque page, appliquer dégradation scan
for img in page-*.jpg; do
  convert "$img" \
    -rotate "$(awk -v min=-1.5 -v max=1.5 'BEGIN{srand(); print min+rand()*(max-min)}')" \
    -modulate 100,90,100 \
    -level 5%,95% \
    -attenuate 0.4 +noise Gaussian \
    -blur 0x0.3 \
    "scan-$img"
done

# Reconstruire en PDF
img2pdf scan-page-*.jpg -o 02-main-courante-commissariat.pdf
```

#### Option B — Photoshop / After Effects (motion designer, plus contrôlé)

Importer le PDF natif → traiter chaque page :
- Légère rotation aléatoire (-1° à +1°)
- Bruit gaussien faible
- Légère perte de contraste
- Floutage très léger (radius 0.3 px)
- Optionnel : ajouter quelques "points de poussière" simulant un scanner sale

### Vérifications post-génération

- Le PDF final doit faire **10 pages au total** (3 + 2 + 1 + 4)
- Chaque page doit ressembler à un **scan administratif lisible** mais imparfait — pas un PDF natif propre, pas non plus un scan illisible
- Le texte doit rester lisible à l'œil mais nécessite un OCR pour être extrait par programme (test : ouvrir le PDF, essayer de sélectionner du texte au curseur — si la sélection ne fonctionne pas, c'est bien un PDF "image" comme attendu pour une démo OCR)
