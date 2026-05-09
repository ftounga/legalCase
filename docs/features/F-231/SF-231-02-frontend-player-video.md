# Mini-spec — F-231 / SF-231-02 — Frontend : élargir upload vidéo + player HTML5 + frames preview

## Identifiant

`F-231 / SF-231-02`

## Feature parente

`F-231` — Ingestion et analyse de pièces vidéo (MP4/MOV) — extraction frames clés + Claude Vision multi-frames

## Statut

`ready`

## Date de création

2026-05-09

## Branche Git

`feat/SF-231-02-frontend-player-video`

---

## Objectif

Élargir l'attribut `accept` du file input à `.mp4,.mov`, valider la durée côté client (≤ 60 s), envoyer le header `X-Video-Duration-Seconds` au backend, et afficher un player HTML5 + thumbnail strip des frames extraites dans le `DocumentPreviewDialog`.

---

## Comportement attendu

### Cas nominal

1. L'avocat sélectionne `.mp4` ou `.mov` via le file input.
2. Validation côté client :
   - Taille ≤ 100 Mo (sinon snackbar).
   - Lecture de la durée via HTML5 :
     ```typescript
     const video = document.createElement('video');
     video.src = URL.createObjectURL(file);
     video.onloadedmetadata = () => {
       const duration = video.duration; // secondes
       URL.revokeObjectURL(video.src);
       // valider duration <= 60
     };
     ```
   - Si durée > 60 s → snackbar "Vidéo trop longue : 60 s max" + retrait de la sélection.
3. Vignette de prévisualisation : capture de la frame à 50% via `<canvas>` + `video.currentTime = duration/2`.
4. Upload : `POST /api/v1/case-files/{id}/documents` avec header `X-Video-Duration-Seconds: <durée arrondie>`.
5. Suivi extraction asynchrone (pattern existant — polling SSE).
6. Une fois extraction DONE, dans `DocumentPreviewDialog` (réutilisé du flow images / PDF), affichage :
   - Player HTML5 standard `<video controls>` avec source S3 du document (URL signée).
   - **Thumbnail strip** : 5 vignettes des frames extraites (récupérées du backend qui les a stockées en S3 sous `case-files/{id}/documents/{docId}/frames/frame-1.png`...).
   - **Sidebar Vision** : badge violet "Legal Vision" + texte de `visual_description` au-dessus du player.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Vidéo > 100 Mo | Snackbar erreur + retrait |
| Durée > 60 s | Snackbar erreur + retrait |
| Format non supporté | Snackbar erreur |
| Backend 400 `VIDEO_TOO_LONG` | Snackbar erreur |
| Backend 402 `VIDEO_QUOTA_EXCEEDED` | Bandeau quota existant |
| Frames non disponibles dans S3 (extraction encore en cours) | Skeleton loader |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : aucun.
- [x] **Autres pays / domaines** : transversal.
- [x] **Autres UI patterns** : le player vidéo HTML5 est nouveau dans l'app — premier usage. Documenter pour réutilisation future.
- [x] **Autres flows transversaux** : aucun.

### Cas spécifique : nouveau pattern UI

- [x] **Le player vidéo peut-il être réutilisé ?** Oui — usages futurs envisageables : démos en onboarding, vidéos guides UI, etc. À documenter.
- [x] **Patterns concurrents** : aucun. Premier player.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `case-file-detail` upload | Oui | Modification directe (en complément de SF-230-02) |
| `DocumentPreviewDialog` | Oui | Extension pour player + thumbnail strip |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature

---

## Critères d'acceptation

- [ ] L'attribut `accept` du file input intègre `.mp4,.mov` (en complément des images de SF-230-02).
- [ ] Validation côté client : durée ≤ 60 s détectée via HTML5 video metadata.
- [ ] Validation côté client : taille ≤ 100 Mo.
- [ ] Header `X-Video-Duration-Seconds` envoyé au backend lors de l'upload.
- [ ] Vignette preview générée (frame à 50% via canvas).
- [ ] `DocumentPreviewDialog` affiche un player HTML5 `<video controls>` quand le contentType est vidéo.
- [ ] `DocumentPreviewDialog` affiche les 5 thumbnails des frames extraites.
- [ ] `DocumentPreviewDialog` affiche le badge "Legal Vision" + `visual_description` (réutilise pattern F-148 SF-148-02 existant).
- [ ] Tests Jest : sélection MP4 valide, sélection MP4 trop longue rejetée, sélection MP4 trop volumineuse rejetée, header durée envoyé, thumbnail générée.

---

## Périmètre

### Hors scope (explicite)

- Édition vidéo côté client (cut, trim) — V2.
- Player avec annotations / timestamps cliquables — V2.
- Backend ingestion (couvert par SF-231-01).
- Quotas (couvert par SF-231-03).

---

## Technique

### Composants Angular modifiés

- `case-file-detail.component.html` — `accept` étendu à `.pdf,.doc,.docx,.txt,.jpg,.jpeg,.png,.heic,.webp,.mp4,.mov`.
- `case-file-detail.component.ts` — méthode `onFileSelected` étendue :
  - Pour les vidéos : lecture asynchrone de la durée via HTML5
  - Validation durée ≤ 60 s
  - Header `X-Video-Duration-Seconds` ajouté à la requête HTTP
- `document-preview-dialog.component.ts/html` — branches conditionnelles selon contentType :
  - Si vidéo → afficher `<video controls>` + thumbnail strip + Vision sidebar
  - Sinon → comportement existant (image / PDF)

### Constantes

```typescript
const ALLOWED_VIDEO_TYPES = ['video/mp4', 'video/quicktime'];
const MAX_VIDEO_SIZE_BYTES = 100 * 1024 * 1024; // 100 Mo
const MAX_VIDEO_DURATION_SECONDS = 60;
```

### Endpoint(s) consommé(s)

- `POST /api/v1/case-files/{id}/documents` — multipart, header `X-Video-Duration-Seconds` pour vidéos
- `GET /api/v1/case-files/{id}/documents/{docId}/frames` — nouveau, retourne URLs signées S3 des 5 frames (à voir avec SF-231-01 si endpoint existant ou à créer)

> Note : si SF-231-01 n'expose pas d'endpoint dédié frames, on peut soit (a) créer un endpoint `GET /documents/{id}/frames` qui retourne 5 URLs signées, (b) inclure les URLs dans le DTO `Document` retourné par `GET /documents/{id}`. Coordination à faire avec SF-231-01.

---

## Plan de test

### Tests unitaires (Jest)

- [ ] `case-file-detail.component.spec.ts` — sélection MP4 valide → upload avec header durée
- [ ] `case-file-detail.component.spec.ts` — sélection MP4 > 60s → snackbar erreur, pas d'upload
- [ ] `case-file-detail.component.spec.ts` — sélection MP4 > 100Mo → snackbar erreur, pas d'upload
- [ ] `document-preview-dialog.component.spec.ts` — contentType vidéo → player HTML5 affiché
- [ ] `document-preview-dialog.component.spec.ts` — contentType vidéo → thumbnail strip 5 frames affichée
- [ ] `document-preview-dialog.component.spec.ts` — `visual_description` affichée au-dessus du player

### Isolation workspace

- [x] Non applicable côté frontend.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [x] **Navigation / routing frontend** — composant détail dossier touché (déjà touché par SF-230-02)

### Composants impactés

| Composant | Impact | Test |
|-----------|--------|------|
| `case-file-detail.component` | Extension validation upload | Tests Jest existants |
| `document-preview-dialog.component` | Branche conditionnelle vidéo | Tests Jest |

### Smoke tests E2E

- [ ] `e2e/smoke/upload-video.spec.ts` (à créer) — upload MP4 → preview player visible

---

## Dépendances

### Subfeatures bloquantes

- **SF-231-01 backend** doit être mergée pour traitement réel. Pour les tests Jest (qui mockent), pas de blocage de dev.

### Contrat API importé

- `POST /api/v1/case-files/{id}/documents` (header `X-Video-Duration-Seconds` requis pour vidéos) — défini dans SF-231-01.
- `GET /api/v1/case-files/{id}/documents/{docId}/frames` ou champ `frameUrls: string[]` dans `DocumentDto` — à coordonner avec SF-231-01.

---

## Notes et décisions

- **Pas de Picture-in-Picture** ni shortcut clavier dans le player V1 — le player HTML5 standard avec `controls` couvre les besoins.
- **Thumbnail strip** : layout horizontal 5 vignettes ~80×60 px, cliquable → ouvre la frame full size.
- **Limite 60 s côté client** : si l'avocat tente d'uploader plus, on lui dit gentiment via snackbar avant l'envoi serveur (économie réseau + UX).
