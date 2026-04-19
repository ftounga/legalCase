# Mini-spec — F-122 / SF-122-07 Choix explicite OCR à l'upload

## Identifiant `F-122 / SF-122-07`  · Statut `draft`  · Date `2026-04-19`
## Branche `feat/SF-122-07-ocr-opt-in-choice`

---

## Objectif

Donner à l'avocat le contrôle explicite sur la consommation du quota OCR :
ajouter une case à cocher "Activer l'OCR si scan" (cochée par défaut) dans
la modale d'upload, permettant d'opt-out si le doc ne mérite pas de brûler
le quota (archivage, partage client, test).

Regrouper proprement avec la case FORMS existante (SF-122-03) sous un
bloc "Options OCR" avec dépendance visuelle (FORMS grisé si OCR décoché).

---

## Comportement

### Modale d'upload — bloc Options OCR

```
⚙️ Options OCR
 ☑️ Activer l'OCR si le document est un scan
    (recommandé, consomme votre quota OCR)

 ☐ Analyse approfondie formulaire administratif
    (CERFA, préfecture — compte ×3 dans le quota)
```

La 2ème checkbox est **grisée et non cliquable** si la 1ère est décochée.

### Upload

`POST /api/v1/case-files/{id}/documents` accepte maintenant 2 params multipart :
- `ocrEnabled: boolean` (défaut `true`) — NOUVEAU SF-122-07
- `ocrFormsMode: boolean` (défaut `false`) — existant SF-122-03

Persistés sur `Document` dans les colonnes `ocr_enabled` / `ocr_forms_mode`.

### Extraction

`ExtractionService.extract()` : avant d'appeler `OcrService.tryOcr()`, check
`docRef.isOcrEnabled()`. Si `false` → skip OCR, extraction `FAILED` motif
`EMPTY_TEXT` comme avant F-122 (pas d'appel AWS, pas de consommation quota).

### Cas limite

| Situation | Comportement |
|---|---|
| PDF lisible (PDFBox succès) | `ocrEnabled` ignoré — l'OCR n'est pas déclenché |
| PDF scanné + `ocrEnabled=false` | Pas d'OCR → FAILED EMPTY_TEXT → badge "Non analysable" visible |
| PDF scanné + `ocrEnabled=false` + bouton retry plus tard | ✅ retry éligible (OCR jamais tenté, metadata "internal" seul). L'avocat peut réactiver a posteriori |
| `ocrEnabled=false` + `ocrFormsMode=true` | `ocrFormsMode` ignoré (cohérence — pas d'OCR = pas de mode FORMS) |
| Non-PDF (docx, txt) | Flags ignorés (OCR ne s'applique pas) |

---

## Critères d'acceptation

- [ ] Migration 085 : `documents.ocr_enabled BOOLEAN NOT NULL DEFAULT TRUE`
- [ ] `Document.ocrEnabled` exposé (getter/setter)
- [ ] `POST /documents` accepte `ocrEnabled` (optionnel, défaut `true`)
- [ ] `DocumentService.upload(..., ocrEnabled)` persiste
- [ ] `ExtractionService` skip OCR si `ocrEnabled=false` → `EMPTY_TEXT` direct
- [ ] Modale frontend : checkbox #1 cochée par défaut, libellé clair
- [ ] Checkbox #2 FORMS grisée visuellement si #1 décochée (attribute `disabled` + CSS)
- [ ] `DocumentService` frontend passe `ocrEnabled` dans formData
- [ ] Tests backend (2 TU : OCR skip / OCR appelé) + tests existants verts

## Hors scope

- Workspace-level setting OFF global (si besoin plus tard, SF séparée)
- UI billing compteur OCR (SF-122-04-UI toujours en attente)
- API async Textract > 5 Mo / 11 pages (SF-122-08)

---

## Technique

| Fichier | Opération |
|---|---|
| `backend/src/main/resources/db/changelog/migrations/085-add-ocr-enabled-to-documents.xml` | NOUVEAU |
| `backend/src/main/java/fr/ailegalcase/document/Document.java` | + champ ocrEnabled |
| `backend/src/main/java/fr/ailegalcase/document/DocumentController.java` | + param multipart |
| `backend/src/main/java/fr/ailegalcase/document/DocumentService.java` | overload avec ocrEnabled |
| `backend/src/main/java/fr/ailegalcase/document/ExtractionService.java` | gate ocrEnabled avant OCR |
| `backend/src/test/java/fr/ailegalcase/document/ExtractionServiceTest.java` | + 1 TU skip si désactivé |
| `frontend/src/app/core/services/document.service.ts` | param optionnel ocrEnabled |
| `frontend/src/app/case-files/case-file-detail/case-file-detail.component.{ts,html,scss}` | signal + 2 checkboxes groupées |

---

## Analyse d'impact

- [ ] Auth / workspace / navigation : non touchés
- [x] Plans / limites : flag ocrEnabled consomme moins de quota — pas de changement structurel, juste une voie "skip" supplémentaire. `PlanLimitService` inchangé.

Aucun smoke E2E concerné.
