# SF-149-01 — Édition manuelle des extraits OCR

## Objectif
Permettre à l'avocat de corriger manuellement le texte extrait d'un document
(OCR Textract ou extraction classique) quand le résultat est dégradé
(manuscrit, scan de basse qualité, pièce d'identité) pour que le pipeline IA
consomme le texte corrigé dès la prochaine analyse.

## Comportement nominal
- Onglet "Texte extrait" dans le `DocumentPreviewDialog` :
  - Par défaut : `<pre>` en lecture seule (comportement actuel).
  - Bouton "Modifier l'extrait" → bascule en textarea éditable (texte
    complet du document, pas filtré par pièce).
  - Boutons "Enregistrer" (PUT) ou "Annuler" (revient en lecture).
- Au 1er enregistrement : backup automatique de la version OCR d'origine
  dans `extracted_text_original`. Les edits suivants n'écrasent pas ce
  backup.
- Badge jaune "Modifié le JJ/MM/AAAA HH:MM" affiché en permanence dès que
  `text_edited_at` est renseigné.
- Bouton "Réinitialiser" (visible uniquement si `text_edited_at` non null) :
  restaure `extracted_text_original`, remet à null le backup et le timestamp.

## Cas d'erreur
- **Status != DONE** : 409 "Cannot edit — extraction not DONE".
- **extractedText null** : 400 "extractedText is required".
- **Reset alors qu'aucun edit** : 409 "nothing to reset".
- **Limite** : 500 000 caractères (2,5× limite d'affichage).
- **Permissions** : contrôlé par workspace (même pattern que Preview).

## Critères d'acceptation
- [x] Migration `102-add-manual-edit-to-extractions.xml` ajoute les 2 colonnes
- [x] `DocumentExtraction` expose `extractedTextOriginal` + `textEditedAt`
- [x] `DocumentExtractionEditService.editText()` : écrit + backup si 1er edit
- [x] `DocumentExtractionEditService.resetToOriginal()` : restaure + nettoie
- [x] `PUT /api/v1/case-files/{cfId}/documents/{docId}/extraction`
- [x] `POST /api/v1/case-files/{cfId}/documents/{docId}/extraction/reset`
- [x] `DocumentPreviewResponse.textEditedAt` exposé à l'API
- [x] Frontend : boutons Modifier/Enregistrer/Annuler + badge + Reset
- [x] Frontend : confirmation avant reset ("Vos modifications seront perdues")
- [x] 6 tests unitaires `DocumentExtractionEditServiceTest` verts
- [x] 24 tests `DocumentPreviewDialogComponent` verts (non-régression)

## Plan de test
- **Unit backend** : `DocumentExtractionEditServiceTest` (6 tests)
  - 1er edit : backup pris
  - 2e edit : backup préservé
  - edit null : 400
  - edit status != DONE : 409
  - reset : restaure + clean
  - reset sans edit : 409
- **Unit frontend** : couvert indirectement par le build + tests existants
  non régressés
- **Intégration (staging)** : voir section "Validation" du message final
- **Isolation workspace** : `loadAndAuthorize()` applique le même contrôle
  que `DocumentPreviewService`

## Hors périmètre
- **Granularité par pièce** : l'édition est au document entier (plus simple,
  évite la complexité de réécrire les marqueurs `=== PAGE N ===`).
- **Re-extraction OCR après edit** : gardée au comportement actuel (non
  gérée spécifiquement dans cette SF ; à traiter si devient un problème).
- **Historique des edits** : non conservé, seule la dernière version + la
  version d'origine sont stockées.

## Tables / endpoints / composants impactés
**Backend** :
- `document_extractions` : +2 colonnes (`extracted_text_original`,
  `text_edited_at`)
- `DocumentExtraction` entity : +2 champs
- `DocumentExtractionEditService` : nouveau service
- `DocumentController` : +2 endpoints
- `EditExtractionRequest` : nouveau DTO
- `DocumentPreviewResponse` : +1 champ `textEditedAt`
- Migration Liquibase 102

**Frontend** :
- `DocumentPreview` model : +1 champ `textEditedAt`
- `DocumentService` : +2 méthodes `editExtraction`, `resetExtraction`
- `DocumentPreviewDialogComponent` : signals edit + méthodes
- Template : toolbar + textarea + badge
- Styles : toolbar, badge, textarea jaune pâle

## Impact par domaine métier
Transversale — la feature s'applique identiquement aux 3 domaines (droit
du travail, immigration, famille) et aux 2 pays (FR / BE). L'OCR n'est pas
sensible au domaine. Aucune adaptation par domaine ou pays.

## Analyse de cohérence transversale
- **DocumentPreviewDialog existant** : unique point de consommation de
  `extractedText`. Ajout non destructeur.
- **Pipeline IA (chunking, chunk analysis)** : lit toujours
  `extraction.getExtractedText()`. La prochaine analyse consomme
  automatiquement le texte corrigé — aucune modification nécessaire.
- **F-145 pièces détectées** : les pages (via marqueurs `=== PAGE N ===`)
  peuvent dévier si l'avocat supprime des lignes. Limite documentée dans
  "Hors périmètre". Pas de bloqueur V1.
- **F-148 vision** : indépendant (enrichit les pièces, pas le texte OCR).
- **Auth / workspace** : réutilise `CurrentUserResolver` + contrôle
  workspace strict (même pattern que `DocumentPreviewService`).

## Nouveau pattern UI ou service partagé
- **DocumentExtractionEditService** : service dédié, pas de duplication
  avec `DocumentPreviewService` (responsabilités claires). Pas de pattern
  réutilisable à extraire.
- **Badge "Modifié le X"** : style local au dialog, pas assez large pour
  justifier un composant partagé. Si le pattern apparaît ailleurs
  (F-145-11 reclassification pièce manuelle), on le promouvra.
