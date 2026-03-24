# Mini-spec — F-40 / SF-40-01 Export PDF de la synthèse

---

## Identifiant

`F-40 / SF-40-01`

## Feature parente

`F-40` — Export PDF de la synthèse

## Statut

`ready`

## Date de création

2026-03-24

## Branche Git

`feat/SF-40-01-export-pdf-synthese`

---

## Objectif

Permettre à l'avocat d'exporter la synthèse d'un dossier en PDF structuré et visuellement soigné, directement depuis l'écran de synthèse, sans aucun appel backend supplémentaire.

---

## Comportement attendu

### Cas nominal

1. L'avocat est sur l'écran `/case-files/:id/synthesis` avec une synthèse chargée.
2. Il clique sur le bouton **"Exporter PDF"** (icône `picture_as_pdf`, bouton stroked en haut à droite de l'en-tête).
3. La génération est instantanée (côté client, bibliothèque `pdfmake`).
4. Le navigateur déclenche le téléchargement d'un fichier `synthese-[slug-titre]-[date].pdf`.

### Structure du PDF généré

**Page de couverture**
- Logo AI LegalCase (centré)
- Titre du dossier en Merriweather 28px, bleu marine `#1A3A5C`
- Badge type de synthèse : `Synthèse initiale` ou `Synthèse enrichie` (fond or `#C9973A`, texte blanc)
- Date d'export et version (ex: `v2 — 24/03/2026`)
- Ligne de séparation dorée

**Sections (chacune avec en-tête coloré bleu marine)**
1. **Chronologie** — tableau à 2 colonnes (Date | Événement), ligne alternée gris clair
2. **Faits** — liste numérotée avec puce `•` dorée
3. **Points juridiques** — liste numérotée avec puce `⚖` couleur bleu marine
4. **Risques** — liste avec icône `▲` couleur rouge sobre `#C0392B`, fond légèrement rosé sur chaque item
5. **Questions ouvertes** — liste avec puce `?` couleur gris moyen (si présentes)

**Pied de page** sur chaque page
- Gauche : `AI LegalCase — Confidentiel`
- Droite : `Page X / Y`
- Ligne de séparation fine

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Synthèse non chargée (signal null) | Bouton désactivé |
| Titre du dossier vide | Nom fichier : `synthese-export-[date].pdf` |
| Erreur inattendue pdfmake | SnackBar erreur "Erreur lors de la génération du PDF" |

---

## Critères d'acceptation

- [ ] Bouton "Exporter PDF" visible sur l'écran de synthèse uniquement quand `synthesis()` est non null
- [ ] Clic déclenche le téléchargement sans rechargement de page
- [ ] Fichier nommé `synthese-[slug]-[YYYY-MM-DD].pdf`
- [ ] Page de couverture : logo, titre, badge type, date/version
- [ ] Chronologie : tableau daté lisible
- [ ] Faits, points juridiques, risques, questions : chaque section visible si non vide
- [ ] Risques : fond coloré rouge clair pour chaque item
- [ ] Pied de page : `AI LegalCase — Confidentiel` + pagination `Page X / Y`
- [ ] Bouton désactivé si synthèse non chargée
- [ ] Pas d'appel backend supplémentaire

---

## Périmètre

### Hors scope (explicite)

- Export depuis un autre écran que la synthèse
- Choix des sections à inclure
- Envoi par email du PDF
- Génération PDF côté backend
- Export des messages du chat IA

---

## Technique

### Endpoint(s)

Aucun — génération 100% frontend.

### Tables impactées

Aucune.

### Migration Liquibase

- [x] Non applicable

### Dépendances npm

- `pdfmake` + `@types/pdfmake` — génération PDF côté client

### Composants Angular impactés

- `SynthesisComponent` — ajout bouton + méthode `exportPdf()`
- `PdfExportService` (nouveau) — logique de génération isolée du composant

---

## Plan de test

### Tests unitaires

- [ ] `PdfExportService` — `buildDocument()` retourne un objet valide avec les sections non vides
- [ ] `PdfExportService` — sections vides (`faits: []`) absentes du document généré
- [ ] `PdfExportService` — nom de fichier slugifié correctement (accents, espaces)

### Tests d'intégration

- [ ] Non applicable (pas d'endpoint)

### Isolation workspace

- [ ] Non applicable — la synthèse est déjà chargée dans le composant, appartient au workspace courant

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — ajout d'un bouton et d'un service sur un écran existant, sans toucher auth, routing, workspace context, ni plans/limites

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — pas de route nouvelle, pas de guard modifié

---

## Dépendances

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

**Choix pdfmake vs alternatives :**
- `jsPDF + html2canvas` : screenshot du DOM → qualité variable, polices non respectées, layout approximatif
- `window.print()` : dialogue navigateur, pas de téléchargement programmatique, mise en page dépendante du browser
- `pdfmake` : API déclarative, contrôle total sur layout/couleurs/polices, génération déterministe → retenu

**Polices PDF :** pdfmake utilise Roboto par défaut (proche Inter). Merriweather n'est pas disponible nativement dans pdfmake — les titres utilisent Roboto Bold, visuellement cohérent.

**Logo :** inclus en base64 depuis `assets/legalcase-logo.png` pour l'embarquer dans le PDF.
