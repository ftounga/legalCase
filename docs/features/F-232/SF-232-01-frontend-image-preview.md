# SF-232-01 — Frontend : aperçu visuel des pièces image dans DocumentPreviewDialog

**Feature parente** : F-232 — Aperçu visuel des pièces image (JPG/PNG/HEIC/WebP)
**Type** : frontend pur (Angular)
**Estimation** : ~30 min

## Objectif (1 phrase)

Afficher l'image source dans `DocumentPreviewDialog` pour les pièces dont le `mimeType` commence par `image/`, en parallèle du rendu PDF (pdf.js) et du rendu vidéo (player HTML5 SF-231-02).

## Contexte

`DocumentPreviewDialog` ne sait actuellement rendre visuellement que les PDF (via pdf.js sur canvas) et les vidéos MP4/MOV (player HTML5 ajouté par SF-231-02). Toutes les autres pièces (JPG, PNG, HEIC, WebP — ouvertes par F-230 SF-230-01) tombent sur l'onglet "Texte extrait" sans aucun aperçu visuel — l'avocat voit le texte OCR + la description Vision mais pas l'image elle-même.

**Constat reproduit 2026-05-09** sur dossier "Baston sophie" en staging : 4-5 pièces sur 7 (acte de mariage en JPG, certificat médical, SMS PNG, photo porte forcée, facture serrurier) tombent sur "Aperçu non disponible". Bug pré-existant à F-230 mais devenu très visible depuis l'ouverture de l'upload natif image.

## Comportement nominal

1. L'avocat ouvre `DocumentPreviewDialog` sur une pièce dont `mimeType.startsWith('image/')`.
2. Le dialog affiche, en haut de la zone principale (au-dessus du bloc OCR / Vision déjà présent), un **bloc image** :
   - `<img>` HTML standard avec `[src]` pointé sur l'endpoint canonique `/api/v1/case-files/{cfId}/documents/{docId}/content` (déjà servi pour PDF + vidéo, Range request supporté côté backend).
   - L'image est rendue à sa taille naturelle, contrainte par CSS `max-width: 100%` et `max-height: 70vh`. Centrage horizontal.
   - Fond gris clair `#f5f5f5` autour pour faire ressortir les images sur fond blanc (capture iMessage notamment).
   - Lien `<a target="_blank">` autour de l'image pour ouvrir en plein format dans un nouvel onglet.
3. Le bloc OCR / Vision existant reste visible **en dessous** (texte extrait + description Legal Vision).
4. Aucun onglet `<mat-tab>` supplémentaire — pour les images, l'aperçu est intégré directement, pas dans un onglet (cohérent avec le bloc vidéo SF-231-02).

## Cas d'erreur

| Cas | Comportement |
|-----|--------------|
| Image non chargée (404, S3 expirée) | `<img>` échoue silencieusement — handler `(error)` affiche un placeholder discret « Aperçu indisponible ». |
| `extractionStatus === 'FAILED'` | Le bloc image s'affiche quand même (l'avocat veut voir l'image même si l'OCR a raté). Le bloc d'alerte d'extraction échouée reste visible en-dessous. |
| `extractionStatus !== 'DONE'` (en cours) | Idem — l'image est immédiatement affichable, on ne fait pas attendre l'OCR. |
| HEIC sur navigateur non-Safari | Le navigateur ne décode pas HEIC nativement (sauf Safari). Cas accepté en V1 — le placeholder « Aperçu indisponible » apparaît. Conversion serveur HEIC→JPG hors scope (V2). |

## Critères d'acceptation vérifiables

- [x] CA-01 : Pour `mimeType = 'image/jpeg'`, le `<img>` est rendu avec `[src]` correct, contraint par CSS.
- [x] CA-02 : Pour `mimeType = 'image/png'`, idem CA-01.
- [x] CA-03 : Pour `mimeType = 'application/pdf'`, **aucun `<img>`** n'est rendu (régression interdite — le canvas pdf.js reste seul).
- [x] CA-04 : Pour `mimeType = 'video/mp4'`, **aucun `<img>`** n'est rendu (le bloc vidéo SF-231-02 reste seul).
- [x] CA-05 : Pour une image, le bloc OCR (texte extrait) reste visible en-dessous du bloc image quand `extractionStatus === 'DONE'`.
- [x] CA-06 : Pour une image en `extractionStatus = 'FAILED'`, le bloc image s'affiche quand même + alerte d'échec en-dessous.
- [x] CA-07 : Clic sur l'image ouvre l'image en plein format dans un nouvel onglet (`target="_blank"`).
- [x] CA-08 : Si l'image échoue à charger (404/erreur S3), le placeholder « Aperçu indisponible » est affiché à la place du `<img>`.

## Plan de test minimal

### Tests unitaires Jest (`document-preview-dialog.component.spec.ts`)

| Test | Vérifie |
|------|---------|
| T-IMG-01 — `isImage()` true pour `image/jpeg` | Signal computed retourne `true` quand mimeType commence par `image/`. |
| T-IMG-02 — `isImage()` false pour `application/pdf` | Régression interdite. |
| T-IMG-03 — `isImage()` false pour `video/mp4` | Régression interdite. |
| T-IMG-04 — `imageUrl()` retourne `/api/v1/case-files/{cfId}/documents/{docId}/content` | URL bien construite. |
| T-IMG-05 — Rendu `<img>` présent dans le DOM si `isImage() === true` | Vérifie présence du `[data-testid="image-preview"]`. |
| T-IMG-06 — Rendu `<img>` absent si `isImage() === false` | Régression interdite (pdf, vidéo, txt). |
| T-IMG-07 — Bloc OCR (extracted text) reste visible quand image | Le bloc image n'écrase pas le bloc texte. |
| T-IMG-08 — Handler onImageError() définit le signal `imageLoadFailed` à `true` | Et le placeholder est rendu à la place. |

### Smoke test staging manuel

Après merge + deploy : ouvrir le dossier "Baston sophie" et vérifier les 5 pièces image (acte mariage JPG, certificat médical JPG, SMS PNG, porte forcée JPG, facture serrurier JPG) → toutes doivent afficher l'image en aperçu.

## Tables / endpoints / composants impactés

### Composants modifiés

- `frontend/src/app/case-files/document-preview-dialog/document-preview-dialog.component.ts` — ajout signaux `isImage`, `imageUrl`, `imageLoadFailed` + handler `onImageError()`.
- `frontend/src/app/case-files/document-preview-dialog/document-preview-dialog.component.html` — bloc `@if (isImage()) { ... }` parallèle au bloc `@if (isVideo())` existant.
- `frontend/src/app/case-files/document-preview-dialog/document-preview-dialog.component.scss` — styles `.image-preview-wrapper`, `.image-preview`, `.image-preview-placeholder`.
- `frontend/src/app/case-files/document-preview-dialog/document-preview-dialog.component.spec.ts` — 8 nouveaux tests (T-IMG-01..08).

### Endpoints réutilisés (aucun nouveau)

- `GET /api/v1/case-files/{cfId}/documents/{docId}/content` — déjà servi par `DocumentController` (PDF + vidéo). Sert les bytes natifs avec le bon `Content-Type` (renvoyé par S3). Aucun changement backend nécessaire.

## Ce qui est hors périmètre

- Conversion HEIC → JPG côté serveur ou client (V2).
- Zoom + pan sur l'image (V2 — utiliser une lib type `panzoom` ou `viewerjs`).
- Galerie multi-images (V2 — pour le cas d'un PDF multi-pages converti en images).
- Édition / annotation de l'image (V2).
- Onglet "Aperçu" séparé pour les images (le rendu est intégré directement, pas dans un tab — cohérent vidéo).

## Impact par domaine métier

Transversal — l'aperçu image bénéficie aux 3 domaines (Travail / Immigration / Famille) et aux 2 pays (FR + BE). Aucune adaptation par domaine.

## Analyse de cohérence transversale

| Cible | Verdict |
|-------|---------|
| Autres outils décisionnels | Non concerné — aucun outil décisionnel n'affiche d'image. |
| Autres dialogs frontend | Aucun autre dialog n'affiche de pièce. `DocumentPreviewDialog` est le seul point d'entrée. |
| Préoccupations transversales (auth, workspace, plans, navigation, outil décisionnel) | Aucune. C'est un changement UI cosmétique sur un dialog modal. |
| Cohérence avec le pattern existant `isPdf()` / `isVideo()` | Le nouveau `isImage()` suit strictement le même pattern (computed signal sur `mimeType`). |
| Routing F-IA-04 / TOOL_REGISTRY | Non concerné — pas un outil décisionnel. |
| `decision_tool_visibility_rules` | Non concerné. |

## Nouveau pattern UI ou service partagé

Aucun nouveau composant partagé — `DocumentPreviewDialog` reste le seul endroit qui rend des pièces. Pas de risque de dette de convergence.

## Précédence

- F-230 (upload natif images, Terminée) — fournit le flux qui produit les pièces image affichables.
- F-231 (vidéo, Terminée) — fournit le pattern `<img [src]="...">` qu'on duplique pour l'image.

## Risque

- Très faible — modification additive d'un dialog modal isolé. Aucun changement backend, aucun changement de routing/auth/quota. Pattern strict miroir du bloc vidéo SF-231-02 déjà mergé.
