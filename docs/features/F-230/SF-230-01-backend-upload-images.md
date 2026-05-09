# Mini-spec — F-230 / SF-230-01 — Backend : upload natif des images JPG/PNG/HEIC/WebP

## Identifiant

`F-230 / SF-230-01`

## Feature parente

`F-230` — Upload natif des pièces images (JPG/PNG/HEIC/WebP) sans conversion PDF préalable

## Statut

`ready`

## Date de création

2026-05-09

## Branche Git

`feat/SF-230-01-backend-upload-images`

---

## Objectif

Élargir le pipeline d'extraction backend pour accepter et traiter directement les images JPG, PNG, HEIC et WebP sans nécessiter de conversion PDF préalable côté avocat.

---

## Comportement attendu

### Cas nominal

1. L'avocat uploade une image (`image/jpeg`, `image/png`, `image/heic`, `image/webp`) via `POST /api/v1/case-files/{id}/documents`.
2. `DocumentController` accepte et persiste l'image en S3 (logique existante inchangée).
3. `ExtractionService.onDocumentUploaded` est déclenché en async post-commit (pattern existant).
4. `ExtractionService.parseText` reconnaît le contentType image et route vers le nouveau handler `extractFromImage`.
5. `extractFromImage` :
   - Si `docRef.isOcrEnabled()` → appel direct à `OcrService.tryOcr(fileBytes, workspaceId, formsMode)` (Textract accepte JPG/PNG/HEIC/WebP nativement).
   - Si OCR échoue ou texte vide ET `VISION_ENABLED` actif → `VisionEnrichmentService` traite l'image (signaux F-148 standards).
6. Texte extrait et description visuelle stockés normalement (`document_extractions.extracted_text` + `document_pieces.visual_description` si Vision déclenchée).
7. Pipeline IA aval (chunking, analyse) consomme l'extraction comme pour un PDF.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| ContentType image non supporté (ex: image/svg+xml) | `IllegalArgumentException` → `UNSUPPORTED_FORMAT` | 400 |
| Image corrompue (bytes non décodables) | `ExtractionFailureReason.CORRUPTED` | extraction FAILED |
| Image trop grande pour Textract (> limite Textract) | `ExtractionFailureReason.OCR_LIMIT_EXCEEDED` | extraction FAILED |
| Workspace différent de l'uploadeur | Géré par couche existante (CaseFile.workspace check) | 403 |
| Quota OCR mensuel dépassé | Géré par OcrService existant | 402 PAYMENT_REQUIRED |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : aucun outil décisionnel n'est impacté — seul le pipeline d'extraction est étendu, en amont des outils.
- [x] **Autres pays** : transversal — aucune adaptation FR/BE.
- [x] **Autres domaines** : transversal — bénéficie aux 3 domaines (travail, immigration, famille).
- [x] **Autres UI patterns** : non concerné côté backend (UI gérée par SF-230-02).
- [x] **Autres flows transversaux** : aucun — pipeline isolé.

### Niveaux de vérification à couvrir

- [x] **DTO backend** : aucun changement de signature `POST /case-files/{id}/documents`.
- [x] **Service / logique métier** : `ExtractionService.parseText` étendu, `OcrService` réutilisé tel quel.
- [x] **Entité JPA + schéma DB** : aucun changement de schéma — les colonnes `Document.contentType` (String) et `DocumentExtraction.extractedText` (TEXT) absorbent les nouveaux types.
- [x] **Tests existants** : `ExtractionServiceTest` à étendre pour les 4 nouveaux content types.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Pipeline OCR Textract (F-122) | Oui | Réutilisé tel quel (Textract accepte JPG/PNG/HEIC/WebP) |
| Pipeline Vision (F-148) | Oui | Réutilisé tel quel (signaux F-148 + `analyzeWithImages` PNG/JPEG) |
| Détection sous-pièces (F-145) | Non applicable | Une image = une pièce, pas de découpage interne |
| Quotas (F-122 packs OCR) | Oui | 1 image = 1 page comptée dans le quota OCR existant |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature

---

## Critères d'acceptation

- [ ] `ExtractionService.parseText` accepte les contentTypes `image/jpeg`, `image/png`, `image/heic`, `image/webp` et délègue à `extractFromImage`.
- [ ] `extractFromImage` route vers OCR Textract direct (pas de rasterisation PDF intermédiaire).
- [ ] Si OCR retourne texte vide ou échoue ET `VISION_ENABLED=true` ET signaux F-148 (whitelist domaine ou OCR pauvre), Vision est appelée sur l'image originale.
- [ ] Une image uploadée devient une `Document` + `DocumentExtraction.extractedText` rempli normalement.
- [ ] L'isolation workspace est respectée (test IT vérifie qu'un user du workspace A ne peut pas accéder à une image uploadée par workspace B).
- [ ] Quota OCR : 1 image = 1 page comptabilisée (vérifié dans test).
- [ ] `unsupported content type` retourné pour `image/svg+xml`, `image/gif`, etc. → extraction marquée FAILED avec `UNSUPPORTED_FORMAT`.

---

## Périmètre

### Hors scope (explicite)

- Extension du frontend `accept` du file input (couvert par SF-230-02).
- Drag-and-drop multi-fichiers (V2).
- Conversion HEIC → JPG côté serveur (Textract gère HEIC nativement).
- Quotas vidéo (couvert par SF-231-03).
- Détection de sous-pièces dans une image (non applicable).

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées |
|-------|-------------|-------------|----------------------------|
| `contentType` | Oui | — | `image/jpeg`, `image/png`, `image/heic`, `image/webp` (en plus des PDF/DOC/DOCX/TXT existants) |
| `fileBytes` | Oui | 10 Mo (limite serveur) | bytes décodables comme image |

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files/{id}/documents` | Oui | LAWYER |

→ Endpoint INCHANGÉ. Seule l'enveloppe interne `parseText` accepte plus de types.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `documents` | INSERT | Existant — `content_type` accepte les nouvelles valeurs |
| `document_extractions` | INSERT | Existant |
| `document_pieces` | INSERT (via détection auto) + UPDATE `visual_description` si Vision | Existant — si Vision déclenchée, F-148 nourrit la description |

### Migration Liquibase

- [x] **Non applicable** — pas de changement de schéma

### Composants Java modifiés

- `ExtractionService.parseText` — ajout des 4 cases image dans le switch
- `ExtractionService` — nouvelle méthode privée `extractFromImage(byte[] fileBytes, String contentType, Document docRef)` qui route vers OCR + Vision

### Composants Java créés

- Aucun nouveau composant — réutilisation totale de `OcrService` et `VisionEnrichmentService`

---

## Plan de test

### Tests unitaires

- [ ] `ExtractionServiceTest` — `parseText` accepte `image/jpeg` et appelle `extractFromImage`
- [ ] `ExtractionServiceTest` — `parseText` accepte `image/png` et appelle `extractFromImage`
- [ ] `ExtractionServiceTest` — `parseText` accepte `image/heic` et appelle `extractFromImage`
- [ ] `ExtractionServiceTest` — `parseText` accepte `image/webp` et appelle `extractFromImage`
- [ ] `ExtractionServiceTest` — `parseText` rejette `image/svg+xml` avec `IllegalArgumentException`
- [ ] `ExtractionServiceTest` — `extractFromImage` appelle `OcrService.tryOcr` quand `isOcrEnabled()=true`
- [ ] `ExtractionServiceTest` — `extractFromImage` ne tente pas d'extraction texte natif (pas de PDFTextStripper)

### Tests d'intégration

- [ ] `POST /api/v1/case-files/{id}/documents` avec image JPG → 201 + extraction asynchrone DONE
- [ ] `POST /api/v1/case-files/{id}/documents` avec image PNG → 201 + extraction asynchrone DONE
- [ ] `POST /api/v1/case-files/{id}/documents` avec image SVG → 201 + extraction asynchrone FAILED (`UNSUPPORTED_FORMAT`)
- [ ] Pipeline complet : upload JPG → extraction OCR → Vision déclenchée si signal 1 (peu de texte) → `visual_description` rempli

### Isolation workspace

- [x] Applicable — IT vérifie qu'un user du workspace A reçoit 403 sur l'image du workspace B

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [x] **Plans / limites** — quota OCR `pages_ocr_monthly` étendu pour comptabiliser les images
- [ ] Navigation / routing frontend
- [ ] Aucune

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `ExtractionService.parseText` | Switch étendu — risque de régression sur PDF/DOC/DOCX/TXT existants | Tests UT existants doivent rester verts |
| `OcrService.tryOcr` | Appelé sur bytes image directement (au lieu de bytes PDF) | Vérifier que Textract accepte les bytes images sans transformation |
| `PlanLimitService.checkOcrPagesQuota` | 1 image = 1 page | Test UT |

### Smoke tests E2E concernés

- [ ] `e2e/smoke/upload.spec.ts` (si existant — sinon créer test nominal) — upload JPG → extraction terminée

---

## Dépendances

### Subfeatures bloquantes

- Aucune — F-148 (Vision) et F-122 (OCR) déjà Terminées.

### Questions ouvertes impactées

- Aucune.

---

## Notes et décisions

- **Pas de rasterisation intermédiaire** : pour les images natives, on n'enrobe PAS l'image dans un PDF avant d'appeler Textract. Textract accepte directement les bytes image. Économie : pas d'overhead PDF, pas de dépendance PDFBox sur le path image.
- **HEIC** : format Apple courant. Textract le supporte nativement depuis 2023.
- **WebP** : format Google courant pour le web. Textract le supporte.
- **Comptage quota** : 1 image = 1 page OCR, cohérent avec les autres consommations.
- **Vision** : pas d'appel Vision systématique sur image — on respecte les signaux F-148 (whitelist domaine + OCR pauvre). Une image de carte d'identité où l'OCR fonctionne bien ne déclenchera pas Vision (économie).
