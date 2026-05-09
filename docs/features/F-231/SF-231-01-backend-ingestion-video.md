# Mini-spec — F-231 / SF-231-01 — Backend : ingestion vidéo MP4/MOV via ffmpeg + Vision multi-frames

## Identifiant

`F-231 / SF-231-01`

## Feature parente

`F-231` — Ingestion et analyse de pièces vidéo (MP4/MOV) — extraction frames clés + Claude Vision multi-frames

## Statut

`ready`

## Date de création

2026-05-09

## Branche Git

`feat/SF-231-01-backend-ingestion-video`

---

## Objectif

Permettre l'ingestion de vidéos MP4/MOV en pièce de dossier : extraction de 5 frames clés via ffmpeg, soumission en batch à Claude Vision (`AnthropicService.analyzeWithImages`), persistance d'une description structurée dans `document_pieces.visual_description`.

---

## Comportement attendu

### Cas nominal

1. L'avocat uploade `.mp4` ou `.mov` (`video/mp4`, `video/quicktime`) via `POST /api/v1/case-files/{id}/documents` (multipart). Le frontend remplit le header `X-Video-Duration-Seconds` (lu via HTML5 `videoElement.duration`).
2. `DocumentController` valide :
   - `contentType` ∈ {`video/mp4`, `video/quicktime`} → autorisé
   - `X-Video-Duration-Seconds` ≤ 60 → autorisé. Sinon → 400 `VIDEO_TOO_LONG`.
   - Taille fichier ≤ 100 Mo → autorisé. Sinon → 413 `PAYLOAD_TOO_LARGE`.
   - Quota mensuel vidéo (cf. SF-231-03) → 402 `VIDEO_QUOTA_EXCEEDED` si dépassé.
3. Document persisté en S3, `DocumentUploadedEvent` émis.
4. `ExtractionService.parseText` reconnaît `video/mp4` ou `video/quicktime` → route vers `extractFromVideo(byte[], String contentType)`.
5. `extractFromVideo` :
   a. Écrit les bytes vidéo dans un fichier temporaire `/tmp/legalcase-video-{uuid}.mp4`.
   b. Lance `VideoFrameExtractor.extract5Frames(tmpFile)` qui appelle `ffmpeg` via `ProcessBuilder` :
      - 5 frames extraites à 10/30/50/70/90% de la durée
      - Sortie en PNG dans `/tmp/legalcase-video-{uuid}-frame-{n}.png`
      - Timeout 30 s sur la commande ffmpeg
   c. Lit les 5 frames en mémoire (List<byte[]>).
   d. Appel `AnthropicService.analyzeWithImages(model=claude-haiku-4-5, systemPrompt, frames, "image/png", userText, maxTokens=2000)` avec un prompt système expliquant qu'il s'agit d'extraits temporels d'une vidéo et qu'il faut produire une description structurée de la scène et des événements identifiables.
   e. La description retournée est stockée comme `extractedText` ET `document_pieces.visual_description` (selon F-148 pattern).
   f. Cleanup : suppression des fichiers temporaires (try-with-resources).
6. Pipeline IA aval consomme la description comme texte standard.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Vidéo > 60 s | Rejet upload | 400 `VIDEO_TOO_LONG` |
| Fichier > 100 Mo | Rejet upload | 413 `PAYLOAD_TOO_LARGE` |
| Quota mensuel dépassé | Rejet upload | 402 `VIDEO_QUOTA_EXCEEDED` |
| ffmpeg échoue (vidéo corrompue) | Extraction FAILED | `VIDEO_EXTRACTION_FAILED` |
| ffmpeg timeout (> 30 s) | Extraction FAILED | `VIDEO_EXTRACTION_TIMEOUT` |
| Vision API échoue | Extraction FAILED | `VISION_API_ERROR` (cohérent F-148) |
| Workspace différent | 403 | (couche existante) |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : aucun outil décisionnel touché.
- [x] **Autres pays / domaines** : transversal — usages métier diversifiés (famille violence conjugale, travail accidents filmés, immigration témoignage).
- [x] **Autres UI patterns** : aucun côté backend.
- [x] **Autres flows transversaux** : touche `ExtractionService` (déjà étendu par SF-230-01 pour images). Pas de conflit attendu si SF-230-01 mergée d'abord, sinon résolution de merge triviale.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Pipeline OCR Textract | Non applicable | Vidéos n'ont pas d'OCR au sens classique |
| Pipeline Vision (F-148) | Oui | `analyzeWithImages` réutilisé tel quel avec batch de 5 frames |
| Détection sous-pièces (F-145) | Non applicable | Une vidéo = une pièce |
| Quotas | Oui | Nouveau quota vidéo (SF-231-03) |
| Dockerfile backend | Oui | Ajout `apt-get install -y ffmpeg` |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [x] Subfeature parallèle SF-231-03 pour les quotas

---

## Critères d'acceptation

- [ ] `ExtractionService.parseText` accepte `video/mp4` et `video/quicktime` et délègue à `extractFromVideo`.
- [ ] `VideoFrameExtractor.extract5Frames` extrait 5 PNG via ffmpeg à 10/30/50/70/90% de la durée.
- [ ] Les 5 frames sont envoyées en batch unique à `AnthropicService.analyzeWithImages` avec mediaType `image/png`.
- [ ] La description retournée est stockée dans `document_extractions.extracted_text` ET `document_pieces.visual_description`.
- [ ] Vidéo > 60s rejetée (400 `VIDEO_TOO_LONG`).
- [ ] Vidéo > 100 Mo rejetée (413 `PAYLOAD_TOO_LARGE`).
- [ ] Quota dépassé → 402 `VIDEO_QUOTA_EXCEEDED` (intégration SF-231-03).
- [ ] Fichiers temporaires supprimés en fin de traitement (try-finally).
- [ ] Dockerfile backend installe ffmpeg.
- [ ] Tests UT `VideoFrameExtractor` avec mock de `ProcessBuilder`.
- [ ] Tests IT pipeline complet upload MP4 → extraction → `visual_description` rempli.
- [ ] Isolation workspace vérifiée.

---

## Périmètre

### Hors scope (explicite)

- Audio / transcription parole (V2 — Whisper).
- Vidéos > 60 s (V2 — chunking temporel).
- Detection automatique de scene change pour adapter les timestamps de frames (V2).
- Export PDF des frames + description (V2).
- Frontend (couvert par SF-231-02).
- Quotas (couvert par SF-231-03).

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Header obligatoire |
|---------|-----|------|--------------------|
| POST | `/api/v1/case-files/{id}/documents` | Oui | `X-Video-Duration-Seconds` (entier) si contentType vidéo |

### Tables impactées

| Table | Opération |
|-------|-----------|
| `documents` | INSERT (existant, contentType élargi) |
| `document_extractions` | INSERT (existant) |
| `document_pieces` | INSERT + UPDATE `visual_description` (existant via F-148) |

### Migration Liquibase

- [ ] Aucune migration de schéma — réutilisation des colonnes F-148 existantes (`visual_description`, `vision_enriched_at`, `vision_model`).

### Composants Java créés

- `fr.ailegalcase.video.VideoFrameExtractor` — service Spring `@Component`, méthode `extract5Frames(File videoFile) : List<byte[]>` qui exécute ffmpeg via ProcessBuilder, lit les 5 PNG produits, retourne les bytes.
- `fr.ailegalcase.video.VideoFrameExtractorProperties` — `@ConfigurationProperties("app.video")` : `maxDurationSeconds=60`, `maxSizeMb=100`, `frameCount=5`, `ffmpegPath=/usr/bin/ffmpeg`, `ffmpegTimeoutSeconds=30`.

### Composants Java modifiés

- `ExtractionService.parseText` — switch étendu pour `video/mp4` et `video/quicktime` → délègue à `extractFromVideo`.
- `ExtractionService` — nouvelle méthode privée `extractFromVideo(byte[], String)` qui orchestre VideoFrameExtractor + AnthropicService.
- `DocumentController` — ajout validation `X-Video-Duration-Seconds` header pour les contentTypes vidéo (rejet 400 si manquant ou > 60).
- `Dockerfile` du backend — ajout `RUN apt-get install -y ffmpeg`.

---

## Plan de test

### Tests unitaires

- [ ] `VideoFrameExtractorTest` — extraction 5 frames d'une vidéo MP4 valide → 5 fichiers PNG produits, bytes retournés.
- [ ] `VideoFrameExtractorTest` — vidéo corrompue → exception propagée.
- [ ] `VideoFrameExtractorTest` — timeout ffmpeg → `VideoExtractionException` avec motif TIMEOUT.
- [ ] `VideoFrameExtractorTest` — cleanup des fichiers temporaires en fin (vérifier via mock du filesystem).
- [ ] `ExtractionServiceTest` — `parseText("video/mp4")` appelle `extractFromVideo`.
- [ ] `ExtractionServiceTest` — `parseText("video/quicktime")` appelle `extractFromVideo`.

### Tests d'intégration

- [ ] `POST /api/v1/case-files/{id}/documents` avec vidéo MP4 valide + header durée 12s → 201, extraction asynchrone DONE, `visual_description` non null.
- [ ] `POST` sans header `X-Video-Duration-Seconds` pour vidéo → 400.
- [ ] `POST` avec durée 90s → 400 `VIDEO_TOO_LONG`.
- [ ] `POST` avec fichier 150 Mo → 413.
- [ ] Pipeline complet : upload MP4 → frames extraites → Vision appelée → description stockée → consommée par pipeline IA aval.

### Isolation workspace

- [x] Applicable — IT vérifie qu'un user du workspace A reçoit 403 sur la vidéo du workspace B.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [x] **Plans / limites** — nouveau quota vidéo (couplé à SF-231-03)
- [ ] Navigation / routing frontend
- [x] **Infra** — ffmpeg requis dans l'image Docker backend

### Composants potentiellement impactés

| Composant | Impact | Test |
|-----------|--------|------|
| `ExtractionService.parseText` | Switch étendu | Tests UT existants doivent rester verts |
| `DocumentController` | Validation header durée | Test IT |
| `AnthropicService.analyzeWithImages` | Appel batch 5 images PNG (au lieu de 1) | Test UT existant doit rester vert |
| Image Docker backend | Taille augmentée (~50 Mo pour ffmpeg) | Vérifier build CI |

### Smoke tests E2E concernés

- [ ] `e2e/smoke/upload-video.spec.ts` (à créer) — upload MP4 → extraction → description visible

---

## Dépendances

### Subfeatures bloquantes

- Aucune. Compatible avec SF-230-01 (les deux étendent `ExtractionService.parseText` mais sur des content types différents).

### Sous-jacents requis

- `ffmpeg` ≥ 4.x dans l'image Docker
- F-148 Vision active (`VISION_ENABLED=true`) — déjà couvert

---

## Notes et décisions

- **Choix de 5 frames** : compromis entre coût Vision (~0.05 € pour 5 frames Haiku 4.5) et fidélité narrative. À ajuster si retours qualité.
- **Distribution 10/30/50/70/90%** : préfère ces pourcentages plutôt que 0/25/50/75/100 pour éviter les frames noires de transition souvent présentes en début/fin de vidéo de surveillance.
- **Pas d'audio V1** : la grande majorité des cas d'usage juridique vidéo (caméra de surveillance) sont MUETS. Audio à V2 (transcription Whisper).
- **ffmpeg via ProcessBuilder** : alternative à JavaCV (lib Java native ffmpeg). Choix ProcessBuilder pour simplicité et robustesse (lib JavaCV ajoute ~150 Mo de natives, plus de risque d'incompatibilités OS).
- **Modèle Haiku 4.5 par défaut** : plus rapide que Sonnet 4.6 et suffisant pour décrire des scènes vidéo. Configurable via `VISION_MODEL` env var (déjà existant F-148).
