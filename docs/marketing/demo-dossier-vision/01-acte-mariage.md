# Pièce 01 — Acte de mariage

## Spécifications techniques

- **Fichier final** : `01-acte-mariage.pdf`
- **Format** : PDF natif (pas un scan — généré directement depuis un éditeur)
- **Dimensions** : A4 portrait, 210 × 297 mm
- **Résolution** : vectoriel, texte sélectionnable
- **Qualité** : impeccable
- **Routage attendu dans la vidéo** : pipeline standard (ni OCR ni Vision activés)

## Rôle dans la vidéo

Cette pièce sert de **baseline** pour montrer que LegalCase ne sur-traite pas un fichier déjà propre. Dans le bloc 2 (moteur de routage), elle traverse le convoyeur sans badge OCR ni Vision — directement vers "indexation standard".

À l'écran : badge discret gris "Texte natif détecté".

## Contenu textuel à intégrer

```
RÉPUBLIQUE FRANÇAISE
Mairie du 11ᵉ arrondissement de Paris
Place Léon-Blum — 75011 PARIS

EXTRAIT D'ACTE DE MARIAGE

N° d'acte : 2018/1147

Le quatorze juillet deux mille dix-huit, à quinze heures trente,
ont été célébrés en mairie du 11ᵉ arrondissement de Paris
les mariages civils suivants :

ÉPOUX
Nom : VASSEUR
Prénoms : Julien, Marc, Antoine
Né le : 22 octobre 1989 à Lyon (Rhône)
Profession : ingénieur logiciel
Domicilié : 12 rue des Lilas, 75011 Paris

ÉPOUSE
Nom : MARTIN
Prénoms : Sophie, Élise
Née le : 8 mars 1991 à Nantes (Loire-Atlantique)
Profession : infirmière
Domiciliée : 12 rue des Lilas, 75011 Paris

RÉGIME MATRIMONIAL
Communauté réduite aux acquêts (régime légal)
Aucun contrat de mariage préalable

TÉMOINS
- Marc DUBREUIL, 45 ans, demeurant à Paris (12ᵉ)
- Anne LECLERC, 38 ans, demeurant à Vincennes (94)

Officier d'état civil : Mme Hélène RICHARD, adjointe au maire

Délivré conformément aux registres
le 12 mai 2026 par le service de l'état civil
de la mairie du 11ᵉ arrondissement de Paris.

Cachet de la mairie    Signature de l'officier d'état civil
[tampon rond bleu]     [signature manuscrite]
```

## Mise en forme

- En-tête centrée avec armoiries de la République stylisées (Marianne)
- Texte justifié, marges 25 mm
- Police corps : Times New Roman 11pt ou équivalent
- Tampon rond bleu en bas à gauche (texte "MAIRIE DU XIᵉ ARR. — PARIS — RÉPUBLIQUE FRANÇAISE")
- Signature manuscrite stylisée en bas à droite

## Éléments à mettre en évidence pour la vidéo

Dans le bloc 2, quand cette pièce traverse le convoyeur de routage, mettre en évidence à l'écran (animation fugitive 1s) :
- "PDF natif détecté"
- "Texte sélectionnable"
- "Routage : indexation standard"

Pas d'extraction OCR, pas de Vision. C'est le contrepoint pédagogique des autres pièces.

---

## Génération automatique de la pièce

### Méthode : script HTML → PDF (Chrome headless)

Copier le code HTML ci-dessous dans un fichier `01-acte-mariage.html`, puis exécuter une commande Chrome headless pour le rendre en PDF natif.

### Code HTML à enregistrer comme `01-acte-mariage.html`

```html
<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="UTF-8">
<title>Extrait d'acte de mariage</title>
<style>
  @page { size: A4; margin: 25mm; }
  body { font-family: 'Times New Roman', Times, serif; font-size: 11pt; line-height: 1.5; color: #111; }
  .header { text-align: center; margin-bottom: 18mm; }
  .header .marianne { font-size: 9pt; letter-spacing: 1px; color: #555; }
  .header h1 { font-size: 16pt; margin: 6mm 0 2mm 0; font-weight: bold; letter-spacing: 1px; }
  .header .mairie { font-size: 11pt; font-style: italic; }
  h2 { text-align: center; font-size: 13pt; margin: 14mm 0 8mm; letter-spacing: 2px; border-top: 1px solid #888; border-bottom: 1px solid #888; padding: 3mm 0; }
  .acte-num { text-align: right; font-size: 10pt; margin-bottom: 6mm; }
  .body-text { text-align: justify; }
  .partie { margin: 6mm 0; }
  .partie h3 { font-size: 11pt; font-weight: bold; letter-spacing: 1px; margin-bottom: 2mm; }
  .partie table { width: 100%; border-collapse: collapse; }
  .partie td { padding: 1mm 0; vertical-align: top; }
  .partie td.label { width: 40mm; color: #444; }
  .footer { margin-top: 16mm; display: flex; justify-content: space-between; align-items: flex-end; }
  .tampon { width: 35mm; height: 35mm; border: 2px solid #1c4a8a; border-radius: 50%; color: #1c4a8a; text-align: center; font-size: 7pt; padding: 6mm 2mm; line-height: 1.2; opacity: 0.85; transform: rotate(-4deg); }
  .signature { font-family: 'Brush Script MT', cursive; font-size: 18pt; color: #1a1a3a; transform: rotate(-3deg); }
</style>
</head>
<body>
<div class="header">
  <div class="marianne">RÉPUBLIQUE FRANÇAISE</div>
  <h1>Mairie du 11ᵉ arrondissement de Paris</h1>
  <div class="mairie">Place Léon-Blum — 75011 PARIS</div>
</div>

<h2>EXTRAIT D'ACTE DE MARIAGE</h2>
<div class="acte-num">N° d'acte : 2018/1147</div>

<p class="body-text">Le quatorze juillet deux mille dix-huit, à quinze heures trente, ont été célébrés en mairie du 11ᵉ arrondissement de Paris les mariages civils suivants :</p>

<div class="partie">
  <h3>ÉPOUX</h3>
  <table>
    <tr><td class="label">Nom :</td><td>VASSEUR</td></tr>
    <tr><td class="label">Prénoms :</td><td>Julien, Marc, Antoine</td></tr>
    <tr><td class="label">Né le :</td><td>22 octobre 1989 à Lyon (Rhône)</td></tr>
    <tr><td class="label">Profession :</td><td>ingénieur logiciel</td></tr>
    <tr><td class="label">Domicilié :</td><td>12 rue des Lilas, 75011 Paris</td></tr>
  </table>
</div>

<div class="partie">
  <h3>ÉPOUSE</h3>
  <table>
    <tr><td class="label">Nom :</td><td>MARTIN</td></tr>
    <tr><td class="label">Prénoms :</td><td>Sophie, Élise</td></tr>
    <tr><td class="label">Née le :</td><td>8 mars 1991 à Nantes (Loire-Atlantique)</td></tr>
    <tr><td class="label">Profession :</td><td>infirmière</td></tr>
    <tr><td class="label">Domiciliée :</td><td>12 rue des Lilas, 75011 Paris</td></tr>
  </table>
</div>

<div class="partie">
  <h3>RÉGIME MATRIMONIAL</h3>
  <p>Communauté réduite aux acquêts (régime légal). Aucun contrat de mariage préalable.</p>
</div>

<div class="partie">
  <h3>TÉMOINS</h3>
  <ul>
    <li>Marc DUBREUIL, 45 ans, demeurant à Paris (12ᵉ)</li>
    <li>Anne LECLERC, 38 ans, demeurant à Vincennes (94)</li>
  </ul>
</div>

<p class="body-text">Officier d'état civil : Mme Hélène RICHARD, adjointe au maire.</p>

<p class="body-text">Délivré conformément aux registres le 12 mai 2026 par le service de l'état civil de la mairie du 11ᵉ arrondissement de Paris.</p>

<div class="footer">
  <div class="tampon">
    MAIRIE DU XIᵉ ARR.<br>
    PARIS<br>
    ★<br>
    RÉPUBLIQUE<br>
    FRANÇAISE
  </div>
  <div class="signature">H. Richard</div>
</div>
</body>
</html>
```

### Commande pour générer le PDF

```bash
# Avec Chrome / Chromium
chromium --headless --disable-gpu --no-sandbox \
  --print-to-pdf=01-acte-mariage.pdf \
  --no-pdf-header-footer \
  01-acte-mariage.html

# Alternative avec weasyprint (Python)
pip install weasyprint
weasyprint 01-acte-mariage.html 01-acte-mariage.pdf
```

### Vérifications post-génération

- Le PDF doit contenir **du texte sélectionnable** (test : ouvrir, sélectionner du texte au curseur — si la sélection fonctionne, c'est un PDF natif). C'est crucial : LegalCase doit pouvoir le détecter comme "texte natif" et bypasser OCR.
- Le tampon rond bleu et la signature doivent être visibles mais sans transformer le PDF en image.
