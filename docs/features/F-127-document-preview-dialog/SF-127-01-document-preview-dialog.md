# Mini-spec — F-127 / SF-127-01 Aperçu document avant analyse (texte + page rendue)

## Identifiant
`F-127 / SF-127-01`

## Feature parente
`F-127` — Aperçu document avant analyse

## Statut `draft`  · Date `2026-04-19`  · Branche `feat/SF-127-01-document-preview-dialog`

---

## Objectif

Permettre à l'avocat de vérifier visuellement qu'une extraction (classique ou OCR) a produit un contenu exploitable avant de lancer une analyse de dossier payante. Un clic sur l'icône "œil" dans la liste des documents ouvre un dialog avec le texte brut extrait (ce que Claude verra) et un rendu visuel de la 1re page du PDF.

---

## Comportement

### Nouvelle colonne "Aperçu"

Dans le tableau des documents de la page `case-file-detail`, entre la colonne "Type" et la colonne "Actions", ajouter une colonne "Aperçu" avec une icône œil cliquable (`mat-icon: visibility`). Disabled et grisée si l'extraction est `PENDING` ou `PROCESSING`.

### Dialog `DocumentPreviewDialogComponent`

**Header**
- Nom du fichier (titre)
- Ligne meta : type MIME, taille humaine, X pages, date d'upload

**Statut extraction (badge)**
- Vert "Extraction classique" — extraction texte natif
- Bleu "OCR Textract" — extraction via OCR (détecté via `extractionMetadata` ou `ocrPagesUsed > 0`)
- Rouge "Échec" — `extractionStatus=FAILED` + motif lisible

**Stats ligne**
- `X caractères extraits`
- `Y pages OCR consommées` (si applicable)

**Contenu — 2 onglets Material**
- **Onglet 1 "Texte extrait"** (par défaut) : bloc scrollable, police monospace, `white-space: pre-wrap`. Si texte vide → gros bandeau rouge "Ce document ne sera pas pris en compte par l'IA".
- **Onglet 2 "Aperçu page"** : rendu canvas de la 1re page du PDF via `pdfjs-dist`. Chargé dynamiquement (import dynamique) uniquement quand l'onglet est activé — évite de gonfler le bundle initial.

**Bouton fermer** en bas à droite.

### Backend

Nouveau endpoint `GET /api/v1/case-files/{caseFileId}/documents/{documentId}/preview` :
- Workspace isolation : via `caseFile.workspace == user.primaryWorkspace`
- Retourne `DocumentPreviewResponse` : `fileName, mimeType, fileSize, pageCount, uploadedAt, extractionStatus, extractionMethod (CLASSIC|OCR|NONE), extractedText (null si non DONE), charCount, ocrPagesUsed (0 si classique), failureReason (null si DONE)`
- Texte **tronqué à 200 000 caractères** côté serveur (protection mémoire, affichable côté UI avec mention "… tronqué à 200 000 caractères")

### Cas d'erreur

- Document introuvable → 404
- Workspace mismatch → 404 (pas 403, évite l'énumération)
- PDF.js échoue à charger la page → fallback message "Aperçu visuel indisponible"
- Extraction PENDING/PROCESSING → bouton œil disabled côté front, 409 backend si forcé

---

## Critères d'acceptation

- [ ] Colonne "Aperçu" présente dans la liste des documents, icône œil cliquable
- [ ] Icône disabled si extraction PENDING/PROCESSING
- [ ] Clic ouvre un `MatDialog` avec header + badge statut + stats
- [ ] Onglet "Texte extrait" affiche le contenu réel de `document_extractions.extracted_text`
- [ ] Si texte vide : bandeau rouge visible
- [ ] Onglet "Aperçu page" rend la 1re page du PDF à ~600 px de large
- [ ] Texte tronqué à 200 000 car côté backend avec mention côté UI
- [ ] Workspace isolation : un document d'un autre workspace renvoie 404
- [ ] Pas de régression sur les autres colonnes ni sur les autres actions (download, delete)

---

## Plan de test

### Unitaires backend
- `DocumentPreviewServiceTest` (nouveau) :
  - Document DONE classique → response avec text + method=CLASSIC
  - Document DONE via OCR → method=OCR + ocrPagesUsed > 0
  - Document FAILED → failureReason renseigné, text null
  - Texte > 200k caractères → tronqué à 200 000
  - Document d'un autre workspace → 404
- `DocumentControllerTest` (nouveau ou complété) : endpoint intégration happy path + 404

### Unitaires frontend
- `DocumentPreviewDialogComponent.spec.ts` :
  - Rendu header avec metadata
  - Badge vert si `extractionMethod=CLASSIC`, bleu si OCR, rouge si FAILED
  - Bandeau alerte si `extractedText` null ou vide
  - Onglet par défaut = "Texte extrait"
- `case-file-detail.component.spec.ts` : nouveau test — clic sur bouton preview ouvre le dialog avec le bon documentId

### Isolation workspace
- Couverte par le test backend "document d'un autre workspace → 404"

---

## Tables / endpoints / composants impactés

### Backend
- `DocumentPreviewResponse.java` — NOUVEAU record
- `DocumentPreviewService.java` — NOUVEAU (ou méthode dans `DocumentService`)
- `DocumentController.java` — endpoint `GET /{documentId}/preview`
- Tests unitaires backend

### Frontend
- `document-preview.service.ts` — NOUVEAU (`getPreview(caseFileId, documentId)`)
- `document-preview-dialog/document-preview-dialog.component.ts` — NOUVEAU
- `case-file-detail.component.ts` — nouvelle colonne + handler
- `case-file-detail.component.html` — nouvelle `<ng-container matColumnDef="preview">`
- `package.json` — ajout `pdfjs-dist`
- Tests frontend

### Migration DB
- Aucune — tout existe déjà (`document_extractions.extracted_text`, `extraction_metadata`)

---

## Hors périmètre

- Aperçu multi-pages (seulement la 1re page en V1, évite complexité UI et coût rendu)
- Zoom / rotation sur l'aperçu
- Recherche dans le texte extrait (V2 si demande)
- Aperçu pour formats non-PDF (.docx, .txt) — le texte extrait reste consultable, mais pas de rendu visuel (extension V2)
- Tronçage intelligent du texte en chunks avec navigation — V2

---

## Analyse de cohérence transversale

| Cible | Applicable | Classement |
|---|---|---|
| Autres pays (Belgique) | Oui | **Intégrée** — pas de spécificité pays |
| Autres domaines | Oui | **Intégrée** — feature workspace-level |
| Super-admin view document | **Backlog** — un super-admin pourrait vouloir voir le texte extrait d'un doc d'un workspace client en cas de support. Hors scope V1. |
| Autres endpoints documents (download, delete) | Non applicable | Endpoint lecture seule distinct |

**Analyse d'impact cross-cutting** :
- [x] **Auth / Principal** — nouveau endpoint utilise `@AuthenticationPrincipal OidcUser` + `Principal` : pattern identique à `DocumentController.download` existant. Pas de changement de principal type, réutilisation stricte.
- [ ] Workspace context — non touché (même pattern que endpoints existants)
- [ ] Plans / limites — non touché (lecture, pas de quota)
- [ ] Navigation / routing — non touché (dialog, pas de route)

Smoke E2E : aucun concerné (action utilisateur non critique, pas sur le chemin auth/navigation).

---

## Nouveau pattern UI ou service partagé

- [x] **`DocumentPreviewDialogComponent`** — nouveau composant standalone. Réutilisable dans le futur depuis :
  - Super-admin (consultation support — backlog)
  - Dashboard décisionnel (drill-down sur source IA — backlog)
  - Pour l'instant utilisé uniquement depuis `case-file-detail`. Standalone + exportable.
- [x] **`pdfjs-dist`** — nouvelle dépendance npm. Import dynamique uniquement quand le dialog est ouvert → pas d'impact bundle initial.
- [x] Pas de service partagé nouveau côté backend (endpoint ciblé, pas de helper transversal).
