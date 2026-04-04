# Mini-spec — F-95 / SF-95-01 Export Word (.docx) de la synthèse

## Identifiant

`F-95 / SF-95-01`

## Feature parente

`F-95` — Export Word (.docx) de la synthèse

## Statut

`draft`

## Date de création

2026-04-01

## Branche Git

`feat/SF-95-01-docx-export`

---

## Objectif

Permettre à l'avocat d'exporter la synthèse d'un dossier en fichier `.docx` éditable depuis la page synthèse, via un nouveau `DocxExportService` 100% frontend (symétrique à `PdfExportService`).

---

## Comportement attendu

### Cas nominal

1. L'avocat est sur la page synthèse d'un dossier (route `/case-files/:id`) avec une analyse chargée.
2. Il clique sur le bouton **Exporter (.docx)** (à côté du bouton PDF existant).
3. Le service `DocxExportService.export(caseFile, synthesis)` est appelé.
4. Un fichier `.docx` est généré client-side et téléchargé immédiatement.
5. Nom du fichier : `[titre-du-dossier]-synthese-vN.docx` (même convention que le PDF).

### Structure du document .docx généré

| Section | Contenu |
|---------|---------|
| Page de titre | Titre du dossier, version, type (STANDARD/ENRICHED), date d'export |
| Timeline | Liste chronologique des événements |
| Faits établis | Liste avec source si présente |
| Points juridiques | Liste avec source si présente |
| Risques identifiés | Liste avec source si présente |
| Score de risque | Niveau + valeur numérique si présent |
| Questions ouvertes | Liste |
| Pièces manquantes | Liste (si présentes) |
| Points de procédure | Liste (si présents — `procedureChecks` non dans `CaseAnalysisResult` → ignoré) |

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Synthèse null au clic | Bouton désactivé (comme pour PDF) |
| Erreur d'import dynamique de la lib | `MatSnackBar` : "Erreur lors de la génération du document Word." |
| Titre du dossier vide | Nom de fichier fallback : `synthese-vN.docx` |

---

## Critères d'acceptation

- [ ] Un bouton "Exporter (.docx)" est visible dans `SynthesisComponent` à côté du bouton PDF
- [ ] Le bouton est désactivé si `synthesis()` est null
- [ ] Un clic déclenche le téléchargement d'un fichier `.docx`
- [ ] Le fichier contient toutes les sections de la synthèse (timeline, faits, points, risques, questions, pièces manquantes)
- [ ] Le nom du fichier suit la convention `[titre]-synthese-vN.docx`
- [ ] La lib `docx` est importée dynamiquement (pas de bundle initial alourdi)
- [ ] En cas d'erreur de génération, un snackbar d'erreur est affiché
- [ ] Au moins 3 tests unitaires couvrent `DocxExportService` (nominal, sans pièces manquantes, buildFileName)

---

## Périmètre

### Hors scope (explicite)

- Export côté backend — 100% frontend uniquement
- Modification du design du .docx après livraison (hors périmètre SF-95-01)
- Export .docx depuis la liste des dossiers
- Intégration OneDrive / Google Drive
- `procedureChecks` : non exposé dans `CaseAnalysisResult` → ignoré dans cette version

---

## Technique

### Endpoint(s)

Aucun — 100% frontend.

### Tables impactées

Aucune.

### Migration Liquibase

- [x] Non applicable

### Composants Angular

- `DocxExportService` (nouveau) — génère et télécharge le `.docx` via la lib `docx`
- `SynthesisComponent` — injecte `DocxExportService`, ajoute bouton + méthode `exportDocx()`

### Dépendance npm

```
npm install --save docx
```

La lib `docx` (https://docx.js.org) est importée dynamiquement dans `DocxExportService.export()` pour éviter d'alourdir le bundle principal.

---

## Plan de test

### Tests unitaires

- [ ] `DocxExportService.buildFileName(caseFile, synthesis)` → `[titre]-synthese-v2.docx`
- [ ] `DocxExportService.buildFileName` avec titre vide → `synthese-v1.docx`
- [ ] `SynthesisComponent` — clic bouton .docx → `docxExportService.export()` appelé
- [ ] `SynthesisComponent` — bouton désactivé si `synthesis() === null`
- [ ] `SynthesisComponent` — erreur génération → `snackBar.open()` appelé avec message d'erreur

### Tests d'intégration

Non applicable (100% frontend, pas d'endpoint).

### Isolation workspace

- [x] Non applicable — lecture seule de données déjà chargées en mémoire.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — subfeature isolée, impact limité à SynthesisComponent.

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — pas de route ni guard modifié.

---

## Dépendances

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- Symétrie intentionnelle avec `PdfExportService` : même signature `export(caseFile, synthesis)`, même import dynamique, même gestion d'erreur via snackbar.
- La lib `docx` génère du OOXML côté client via `Packer.toBlob()`. Le blob est converti en URL objet (`URL.createObjectURL`) puis téléchargé via un lien `<a>` temporaire (même pattern que `exportZip` dans `CaseFileDetailComponent`).
- `procedureChecks` n'est pas dans `CaseAnalysisResult` (il est géré séparément via `ProcedureCheckService`). Il est exclu de cette version.
