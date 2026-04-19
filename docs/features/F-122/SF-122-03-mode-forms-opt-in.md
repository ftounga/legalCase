# Mini-spec — F-122 / SF-122-03 Mode FORMS opt-in (formulaires administratifs)

## Identifiant
`F-122 / SF-122-03`

## Feature parente
`F-122` — OCR pour PDF scannés (AWS Textract)

## Statut
`draft`

## Date de création
`2026-04-19`

## Branche Git
`feat/SF-122-03-mode-forms-opt-in`

---

## Objectif

Permettre à l'avocat d'activer au moment de l'upload un mode OCR "Analyse approfondie" qui extrait aussi les **champs de formulaires administratifs** (CERFA, déclarations préfecture, formulaires URSSAF) en plus du texte et des tableaux. Textract facturant 4,3× plus cher en mode FORMS+TABLES vs TABLES seul, le document compte **×3 dans le quota OCR** (absorbe le ratio avec marge 30 %).

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre la modale d'upload, sélectionne 1 ou N fichiers.
2. Une **checkbox** apparaît : _"Analyse approfondie (formulaire administratif CERFA / préfecture) — consomme ×3 de votre quota OCR"_, **décochée par défaut**.
3. Il coche si le batch contient un/des formulaire(s). Tous les fichiers du batch partagent le flag.
4. `POST /case-files/{id}/documents` transporte un champ multipart `ocrFormsMode: "true" | "false"` (défaut false).
5. Backend persiste `documents.ocr_forms_mode = true` au moment de la création du Document.
6. Pendant l'extraction (SF-122-01) sur PDF à texte vide :
   - Si `ocr_forms_mode = false` : Textract `FeatureType.TABLES` (comportement actuel).
   - Si `ocr_forms_mode = true` : Textract `FeatureType.TABLES, FeatureType.FORMS` (mode approfondi).
7. Gate quota (SF-122-02) : si forms mode, vérifier `additionalPages * 3` contre les limites mensuel + journalier (zéro coût AWS si quota dépassé).
8. Après succès OCR, incrémenter le compteur de `pageCount * 3` si forms mode, `pageCount` sinon.

### Cas limite

| Situation | Comportement |
|---|---|
| Upload avec flag `true` mais PDF lisible (PDFBox extrait du texte) | Flag ignoré — pas d'OCR déclenché, pas de consommation quota |
| Upload avec flag `true` mais doc > 5 Mo / > 11 pages | `OCR_UNSUPPORTED_SIZE` (pas d'appel AWS, pas de consommation) |
| Upload non-PDF avec flag `true` | Flag persisté mais ignoré (OCR non applicable sur docx/txt) |
| Flag omis dans la requête | Traité comme `false` (défaut) |
| Upload batch de 5 fichiers, checkbox cochée | Les 5 fichiers ont `ocr_forms_mode=true` |
| Quota mensuel restant < 3× pageCount | `OCR_QUOTA_EXCEEDED` — même gate que SF-122-02 avec multiplier |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — N/A.
- [x] **Autres pays** — N/A. FORMS marche pour CERFA (FR) et formulaires belges équivalents.
- [x] **Autres domaines** — N/A. Utile surtout en immigration (titres de séjour, récépissés), potentiellement travail (certificat de travail pré-imprimé).
- [x] **Autres UI patterns** — La modale d'upload existe déjà (`case-file-detail.component.html`). La checkbox s'y ajoute, pas de nouveau composant.
- [x] **Autres flows transversaux** — Touche **Plans/limites** : le gate quota multiplie par 3 en mode FORMS. Analyse d'impact ci-dessous.

### Classification

| Cible | Applicable ? | Traitement |
|---|---|---|
| `PlanLimitService.isOcrQuotaExceeded` | Oui | Intégré — accepte déjà `additionalPages`, le caller multiplie par 3 |
| `OcrService.tryOcr` | Oui | Intégré — nouvelle signature avec `formsMode` |
| `Document` entity + repo | Oui | + colonne `ocr_forms_mode` (migration 083) |
| `DocumentController` upload endpoint | Oui | + param multipart |
| Modale upload frontend | Oui | + checkbox |

### Nouveau pattern UI ou service partagé

- [x] **Pas de nouveau pattern partagé**. La checkbox est locale à la modale d'upload existante. Le flag est un simple booléen persisté — aucune logique réutilisable ailleurs.

---

## Critères d'acceptation

- [ ] Migration 083 ajoute `ocr_forms_mode BOOLEAN NOT NULL DEFAULT FALSE` sur `documents`
- [ ] `Document` entity expose `ocrFormsMode` (getter/setter)
- [ ] `POST /api/v1/case-files/{id}/documents` accepte param multipart `ocrFormsMode` (optionnel, défaut false)
- [ ] `DocumentService.upload*` persiste le flag
- [ ] `OcrService.tryOcr(fileBytes, workspaceId, formsMode)` — nouvelle signature
- [ ] Feature types Textract : `TABLES` seul si formsMode=false, `TABLES + FORMS` si true
- [ ] Gate quota multiplie `additionalPages × 3` si formsMode=true
- [ ] Incrément `pageCount × 3` si formsMode=true après succès OCR
- [ ] Modale upload frontend : checkbox "Analyse approfondie" décochée par défaut, libellé avec tooltip expliquant ×3
- [ ] `DocumentService.uploadWithProgress` frontend passe `ocrFormsMode: boolean`
- [ ] Build backend vert + build frontend vert, tests existants + 5+ nouveaux tests

---

## Périmètre

### Hors scope (explicite)

- Checkbox per-file (flexible) — V1 = batch-level
- Détection auto "ce doc est un formulaire" via IA — trop complexe pour V1
- Prévisualisation du coût OCR avant upload — SF-122-04 (billing UI)
- Packs overage — SF-122-04
- Bouton rétrocompat avec choix mode — SF-122-05

---

## Technique

### Composants impactés

| Fichier | Opération |
|---|---|
| `backend/src/main/resources/db/changelog/migrations/083-add-ocr-forms-mode-to-documents.xml` | NOUVEAU |
| `backend/src/main/java/fr/ailegalcase/document/Document.java` | + champ |
| `backend/src/main/java/fr/ailegalcase/document/DocumentController.java` | + param upload |
| `backend/src/main/java/fr/ailegalcase/document/DocumentService.java` | persist flag |
| `backend/src/main/java/fr/ailegalcase/ocr/OcrService.java` | signature + feature types + multiplier |
| `backend/src/main/java/fr/ailegalcase/document/ExtractionService.java` | lit flag + passe à OcrService + multiplier incrément |
| `frontend/src/app/case-files/case-file-detail/case-file-detail.component.html` | checkbox |
| `frontend/src/app/case-files/case-file-detail/case-file-detail.component.ts` | signal + passage |
| `frontend/src/app/core/services/document.service.ts` | param upload |
| Tests backend & frontend correspondants |

### Migration Liquibase

- [x] Oui — `083-add-ocr-forms-mode-to-documents.xml` : `addColumn` + rollback `dropColumn`

---

## Plan de test

### Backend
- [ ] U-DOC-FORMS-01 — upload avec `ocrFormsMode=true` → Document persisté avec flag true
- [ ] U-DOC-FORMS-02 — upload sans param → flag false (défaut)
- [ ] U-OCR-03-01 — tryOcr formsMode=true → AnalyzeDocumentRequest avec TABLES+FORMS
- [ ] U-OCR-03-02 — tryOcr formsMode=false → AnalyzeDocumentRequest avec TABLES seul
- [ ] U-OCR-03-03 — gate quota formsMode=true : pages effectives = additionalPages × 3
- [ ] U-EXT-OCR-06 — Document avec formsMode=true → ExtractionService multiplie incrément par 3

### Frontend
- [ ] Checkbox cochée par défaut = false
- [ ] Toggle checkbox → signal mis à jour → upload passe `ocrFormsMode=true`

### Régressions
- [ ] Tests existants F-121 / SF-122-01 / SF-122-02 adaptés (signature OcrService), verts

---

## Analyse d'impact

- [ ] **Auth / Principal** — non touché
- [ ] **Workspace context** — inchangé (le flag est sur Document, pas Workspace)
- [x] **Plans / limites** — TOUCHÉ (multiplier ×3 sur les 2 gates). Composants : OcrService (seul consumer) + PlanLimitService (pass-through déjà en place).
- [ ] **Navigation / routing** — non touché

Aucun smoke E2E impacté.
