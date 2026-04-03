# Mini-spec — F-107 / SF-107-01 Export PDF de la checklist procédurale

> Statut : `ready`

---

## Identifiant

`F-107 / SF-107-01`

## Feature parente

`F-107` — Export PDF de la checklist procédurale

## Statut

`ready`

## Date de création

2026-04-04

## Branche Git

`feat/SF-107-01-export-pdf-checklist`

---

## Objectif

Permettre à l'avocat d'exporter en PDF la checklist procédurale d'un dossier, avec les statuts (✅/❌/⚠️), les descriptions et les raisons de requalification Claude.

---

## Comportement attendu

### Cas nominal

1. L'avocat est sur la page synthèse d'un dossier et la checklist procédurale contient au moins un point.
2. Il clique sur le bouton **"Exporter PDF"** dans l'en-tête de la section checklist.
3. Le PDF est généré côté client via `pdfmake` et téléchargé immédiatement.
4. Le fichier est nommé `checklist-[titre-dossier]-[date].pdf`.

### Contenu du PDF

- **En-tête** : logo LegalCase, titre du dossier, date d'export
- **Sous-titre** : "Checklist procédurale"
- **Résumé** : `X ✅ vérifiés / Y ❌ non conformes / Z ⚠️ à vérifier`
- **Liste des points** (triés par `ordre`) :
  - Icône de statut + description
  - Si `raison` présente : bloc indenté en italique "Raison IA : [raison]"
- **Pied de page** : "Généré par LegalCase — [date]"

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Checklist vide (0 points) | Bouton masqué — pas d'export possible |
| Erreur pdfmake (import dynamique échoue) | Fail silencieux — pas de crash, pas de toast |

---

## Critères d'acceptation

- [ ] Le bouton "Exporter PDF" est visible dans l'en-tête de la section checklist si `procedureChecks().length > 0`
- [ ] Le bouton est masqué si la checklist est vide
- [ ] Le PDF généré contient le titre du dossier et la date d'export
- [ ] Chaque point affiche son statut (✅ VERIFIED / ❌ NON_COMPLIANT / ⚠️ TO_CHECK) et sa description
- [ ] Les points avec une `raison` non nulle affichent "Raison IA : [raison]" en italique
- [ ] Les points sont triés par `ordre` croissant
- [ ] Le résumé (compteurs par statut) est présent en haut de la liste
- [ ] Le nom du fichier suit le format `checklist-[titre]-[date].pdf`
- [ ] L'export utilise `pdfmake` en import dynamique (pas de bundle initial alourdi)

---

## Périmètre

### Hors scope

- Export Word de la checklist (non demandé)
- Export depuis la liste des dossiers (uniquement depuis la page synthèse)
- Personnalisation du contenu du PDF par l'avocat
- Impression directe (navigateur)

---

## Technique

### Endpoint(s)

Aucun — 100% frontend.

### Tables impactées

Aucune.

### Migration Liquibase

- [x] Non applicable

### Composants Angular impactés

- `SynthesisComponent` — ajout méthode `exportChecklistPdf()` + bouton dans le template
- `PdfExportService` — ajout méthode `exportChecklist(caseFile: CaseFile, checks: ProcedureCheck[])`

### Approche technique

Ajouter `exportChecklist()` dans le `PdfExportService` existant (cohérence avec `export()` et `exportDocx()`). Le service reçoit le `CaseFile` (pour le titre) et le tableau `ProcedureCheck[]`. Utilise les mêmes constantes de couleur et le même pattern d'import dynamique que `export()`.

Mapping des statuts :
- `VERIFIED` → `✅` + couleur `#27AE60`
- `NON_COMPLIANT` → `❌` + couleur `#C0392B`
- `TO_CHECK` → `⚠️` + couleur `#C9973A`

---

## Plan de test

### Tests unitaires (Jest)

- [ ] `PdfExportService.exportChecklist()` — appelé avec une liste non vide → `pdfMake.createPdf().download()` appelé
- [ ] `PdfExportService.exportChecklist()` — points triés par `ordre` dans le document
- [ ] `PdfExportService.exportChecklist()` — point avec `raison` → "Raison IA" présent dans le contenu
- [ ] `PdfExportService.exportChecklist()` — point sans `raison` → pas de bloc "Raison IA"
- [ ] `SynthesisComponent` — bouton visible si `procedureChecks().length > 0`
- [ ] `SynthesisComponent` — bouton masqué si `procedureChecks()` est vide
- [ ] `SynthesisComponent` — clic bouton → `exportChecklistPdf()` appelé

### Tests d'intégration

Non applicable — feature 100% frontend sans appel HTTP.

### Isolation workspace

- [x] Non applicable — aucune donnée chargée, export des données déjà en mémoire.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — ajout d'un bouton et d'une méthode dans un service existant, aucun impact sur auth, routing, workspace ou plans.

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — pas de navigation, pas de route, pas d'auth modifiée.

---

## Dépendances

### Subfeatures bloquantes

- F-96 (checklist procédurale) — statut : **done**
- F-40 (pdfmake en place) — statut : **done**

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- `exportChecklist()` ajouté dans `PdfExportService` plutôt qu'un nouveau service dédié — la feature est simple et le service existant suit déjà le même pattern.
- Import dynamique pdfmake conservé (même pattern que `export()`) pour ne pas alourdir le bundle initial.
- Fail silencieux en cas d'erreur pdfmake — cohérent avec le comportement du reste de l'app.
