# Mini-spec — F-144 / SF-144-01 Feedback UX contextuel OCR

## Identifiant · `F-144 / SF-144-01`
## Date · `2026-04-21` · Branche · `feat/SF-144-01-ocr-ux-feedback`

## Objectif
Rendre visible à l'avocat **pendant** et **après** qu'un OCR a été déclenché sur son document. Aujourd'hui la barre de progression est générique : ni signal live de l'OCR en cours, ni trace post-extraction qu'un doc a bien été OCRisé. La capacité F-122 (Textract) est donc invisible côté UX.

## Arbitrages techniques constatés à l'inspection
- **L'OCR est un appel synchrone** à Textract dans `ExtractionService.extract()` (OcrService.tryOcr). Pas de progression page-par-page côté API : Textract retourne tout d'un coup.
- **L'extraction se fait en `@Async`** (thread séparé du HTTP), donc on peut utiliser une transaction séparée pour commiter un flag intermédiaire.
- `DocumentExtraction.extractionMetadata` contient déjà `extractor=textract...` après coup → source de vérité "ce doc a été OCRisé".
- Pas de tracking page-par-page réaliste sans modifier `OcrService` pour rasteriser et appeler Textract page par page (hors scope).

## Comportement ciblé
### Pendant l'upload
- Quand `ExtractionService` détecte que le fallback OCR doit être appelé (PDFBox a renvoyé vide, `docRef.isOcrEnabled() == true`), on **commit un flag `ocr_running = true`** sur l'extraction **dans une transaction séparée** (`REQUIRES_NEW`) afin que le polling frontend le voie pendant que Textract travaille.
- Après retour de `ocrService.tryOcr()`, on remet `ocr_running = false` (via la transaction @Async normale, commitée avec le reste).
- Côté frontend : polling existant (F-121) lit le flag → affiche **sous la barre de progression** le texte *"Document scanné — OCR en cours (AWS Textract)"*.

### Après extraction (état persistant)
- Backend : ajouter au DTO `DocumentResponse` un flag **dérivé** `ocrExtracted` = `extractionMetadata contient "extractor=textract"` (pas de nouvelle colonne dédiée, le metadata existe déjà depuis F-122).
- Frontend : afficher un **chip discret `OCR`** (palette gold `#C9A54B`, petite pastille monospace) à côté du nom du document dans la liste quand `ocrExtracted == true`.

### Principe visuel
- Pas d'animation Lottie, pas d'effet pulsé, pas de message marketing.
- Typo Inter, couleur navy/gold du design system, même taille que les autres chips existants (statut, badges).
- La valeur est dans l'information, pas dans le spectacle.

## Critères d'acceptation
- [ ] Migration Liquibase 096 : `document_extractions.ocr_running BOOLEAN NOT NULL DEFAULT FALSE`
- [ ] `DocumentExtraction.ocrRunning` field + getter/setter
- [ ] Nouveau service léger `OcrRunningFlagService` avec `@Transactional(propagation = REQUIRES_NEW)` pour commiter le flag intermédiaire
- [ ] `ExtractionService` appelle `markOcrRunning(id, true)` avant `ocrService.tryOcr()` et `markOcrRunning(id, false)` après
- [ ] `DocumentResponse` DTO expose `ocrRunning` (source : DocumentExtraction) + `ocrExtracted` (dérivé du metadata)
- [ ] Frontend `DocumentListComponent` affiche le texte "Document scanné — OCR en cours (AWS Textract)" sous la barre quand `ocrRunning == true`
- [ ] Frontend affiche le chip `OCR` (gold, discret) à côté du nom du doc quand `ocrExtracted == true`
- [ ] Tests backend U-01 : `ocrRunning` passe true→false dans une séquence d'extraction avec fallback OCR
- [ ] Tests backend U-02 : `ocrExtracted` reflète correctement `extractionMetadata contains "textract"`
- [ ] Tests frontend U-03 : texte "OCR en cours" rendu quand `ocrRunning === true`
- [ ] Tests frontend U-04 : chip `OCR` rendu quand `ocrExtracted === true`

## Plan de test minimal
### Backend
- U-01 : mock `OcrService.tryOcr` → vérifier que `markOcrRunning(id, true)` est appelé avant et `markOcrRunning(id, false)` après (via ArgumentCaptor sur le service)
- U-02 : parser d'extractionMetadata → `ocrExtracted` true si la string contient "extractor=textract", false sinon
- IT : vérifier rétention du flag après rollback partiel (transaction séparée → flag committed même si l'extraction finale rollback)

### Frontend
- U-03 : composant rendu avec `document.ocrRunning = true` → `nativeElement.textContent` contient "OCR en cours"
- U-04 : composant rendu avec `document.ocrExtracted = true` → un élément `.chip-ocr` est présent avec texte "OCR"

## Tables / endpoints / composants impactés
### Backend
- **Migration 096** : ajout colonne `ocr_running BOOLEAN NOT NULL DEFAULT FALSE` sur `document_extractions`
- `DocumentExtraction.java` : +champ
- `OcrRunningFlagService.java` **(nouveau)** : service dédié avec `@Transactional(REQUIRES_NEW)`
- `ExtractionService.java` : 2 appels (markOcrRunning before/after Textract call)
- `DocumentResponse.java` (ou équivalent DTO) : +`ocrRunning: boolean`, +`ocrExtracted: boolean`
- `DocumentMapper` (si existant) : enrichir le mapping

### Frontend
- `Document` model : +`ocrRunning: boolean`, +`ocrExtracted: boolean`
- Composant liste des documents (probablement `DocumentListComponent` ou équivalent dans case-file-detail) :
  - texte contextuel sous barre de progression
  - chip `OCR` inline

## Analyse de cohérence transversale
| Cible | Évaluation | Classement |
|-------|-----------|------------|
| F-122 OCR Textract | Source de la capacité à révéler. Rien à modifier côté OcrService. | Intégré |
| F-121 polling extractions | Le polling existant lit maintenant 2 flags supplémentaires. | Intégré |
| Design System | Chip `OCR` cohérent avec les badges existants (`badge--pending`, etc.). Palette gold déjà utilisée. | Intégré |
| Autres flags similaires sur doc | Pas de flag "extraction_phase" existant — on ajoute le premier. Si futur besoin (ex: "chunking en cours"), pattern réutilisable mais pas généralisable dès maintenant. | Non applicable (pas de dette de convergence) |

Pas de nouveau composant partagé / directive / DTO transversal réutilisable.

## Préoccupations transversales
- **Plans / limites** : aucun impact
- **Auth / Principal** : aucun impact
- **Workspace context** : aucun impact (le doc est déjà isolé par workspace via F-121)
- **Navigation** : aucun impact

## Hors scope
- Progression page-par-page OCR (non faisable sans refonte OcrService en mode rasterized-par-défaut)
- Animation Lottie, effets pulsés, "IA magique" — arbitrage produit explicite
- Rétro-application du chip `OCR` aux documents déjà extraits avant cette SF (leur metadata contient déjà "textract" donc c'est automatique sans backfill)
- Affichage dans la vue détail d'un document (chip ajouté à la liste uniquement pour rester scopé)
