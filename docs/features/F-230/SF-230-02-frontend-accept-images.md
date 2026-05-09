# Mini-spec — F-230 / SF-230-02 — Frontend : élargir le file input aux images natives

## Identifiant

`F-230 / SF-230-02`

## Feature parente

`F-230` — Upload natif des pièces images (JPG/PNG/HEIC/WebP) sans conversion PDF préalable

## Statut

`ready`

## Date de création

2026-05-09

## Branche Git

`feat/SF-230-02-frontend-accept-images`

---

## Objectif

Élargir l'attribut `accept` du file input dans `case-file-detail.component.html` aux types image courants (JPG/PNG/HEIC/WebP) et ajouter une prévisualisation thumbnail avant upload.

---

## Comportement attendu

### Cas nominal

1. L'avocat clique sur "Ajouter des pièces" → ouverture de la boîte de dialogue système.
2. Le filtre fichiers liste désormais : PDF, DOC, DOCX, TXT, **JPG, JPEG, PNG, HEIC, WebP**.
3. L'avocat sélectionne une ou plusieurs images.
4. Validation côté client :
   - Taille max par image : **10 Mo** (sinon snackbar "Fichier trop volumineux : max 10 Mo").
   - ContentType validé contre la liste autorisée (sinon snackbar "Format non supporté").
5. **Prévisualisation thumbnail** : pour chaque image sélectionnée, affichage d'une vignette avant upload (utilise `URL.createObjectURL(file)` pour générer une URL locale).
6. L'avocat valide → upload normal (`POST /api/v1/case-files/{id}/documents`, multipart).
7. Une fois l'upload terminé, le pipeline backend prend le relais (SF-230-01).
8. La pièce apparaît dans la liste des documents avec son thumbnail correct.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Fichier > 10 Mo | Snackbar erreur, fichier retiré de la sélection avant upload |
| Type non supporté (ex: .gif sélectionné via "tous fichiers" dans la dialog OS) | Snackbar erreur, fichier retiré |
| Erreur serveur 400 (`UNSUPPORTED_FORMAT`) | Snackbar avec message backend |
| Quota OCR atteint (402) | Bandeau quota existant `<app-quota-error-banner>` |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : aucun outil décisionnel touché.
- [x] **Autres pays / domaines** : transversal.
- [x] **Autres UI patterns** : la prévisualisation thumbnail réutilise le pattern existant `DocumentPreviewDialog`.
- [x] **Autres flows transversaux** : aucun.

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] **Le nouveau pattern UI peut-il être réutilisé ?** Oui — la validation taille + thumbnail preview pourrait servir aux uploads d'avatar workspace, aux uploads de pièces dans d'autres écrans. À documenter en backlog si besoin.
- [x] **Patterns concurrents** : aucun pattern concurrent — c'est le premier upload natif d'image dans l'app.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `case-file-detail` upload | Oui | Modification directe |
| Autres uploads (avatar workspace, pièces autres écrans) | Non applicable V1 | Pas d'autre upload aujourd'hui qui mérite ce pattern |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature

---

## Critères d'acceptation

- [ ] L'attribut `accept` du file input dans `case-file-detail.component.html` accepte `.pdf,.doc,.docx,.txt,.jpg,.jpeg,.png,.heic,.webp`.
- [ ] Une image JPG/PNG/HEIC/WebP de < 10 Mo est uploadée avec succès.
- [ ] Une image > 10 Mo est rejetée côté client avec snackbar avant l'envoi serveur.
- [ ] Un fichier `.gif` ou `.svg` (qui passerait outre le filtre OS via "tous fichiers") est rejeté côté client.
- [ ] Une vignette de prévisualisation s'affiche pour chaque image sélectionnée AVANT l'envoi.
- [ ] La vignette utilise `URL.createObjectURL` puis `URL.revokeObjectURL` après upload (pas de fuite mémoire).
- [ ] Tests Jest : sélection JPG accepté, sélection > 10 Mo rejeté, sélection SVG rejeté, vignette générée.

---

## Périmètre

### Hors scope (explicite)

- Drag-and-drop (V2)
- Crop / rotation côté client (V2)
- Conversion HEIC → JPG côté client (le serveur via Textract gère HEIC nativement)
- Vidéos (couvert par SF-231-02)
- Backend upload images (couvert par SF-230-01)

---

## Technique

### Composants Angular modifiés

- `case-file-detail.component.html` (ligne 106) — élargir `accept` :
  ```html
  <input #fileInput type="file"
         accept=".pdf,.doc,.docx,.txt,.jpg,.jpeg,.png,.heic,.webp"
         multiple hidden (change)="onFileSelected($event)">
  ```
- `case-file-detail.component.ts` — méthode `onFileSelected` étendue :
  - Validation taille (10 Mo par fichier image)
  - Validation contentType vs liste blanche
  - Génération thumbnail via `URL.createObjectURL` pour les images
- `case-file-detail.component.html` — bloc UI prévisualisation des thumbnails sélectionnés (avant le clic upload)

### Constantes (à exposer)

```typescript
const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/heic', 'image/webp'];
const MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024; // 10 Mo
```

### Composants Angular créés

- Aucun nouveau composant — modification du composant existant.

---

## Plan de test

### Tests unitaires (Jest)

- [ ] `case-file-detail.component.spec.ts` — `onFileSelected` accepte un JPG < 10 Mo
- [ ] `case-file-detail.component.spec.ts` — `onFileSelected` rejette un JPG > 10 Mo (vérifier snackbar appelé)
- [ ] `case-file-detail.component.spec.ts` — `onFileSelected` rejette un fichier `.svg` (vérifier snackbar)
- [ ] `case-file-detail.component.spec.ts` — `onFileSelected` génère une thumbnail URL pour les images
- [ ] `case-file-detail.component.spec.ts` — les URLs object sont révoquées après upload

### Isolation workspace

- [x] Non applicable — la sécurité est gérée backend.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [x] **Navigation / routing frontend** — non touché, mais le composant de détail dossier est utilisé partout
- [ ] Aucune

### Composants / endpoints existants potentiellement impactés

| Composant | Impact | Test |
|-----------|--------|------|
| `case-file-detail.component` | Modification directe — risque de régression sur upload PDF/DOC | Tests Jest existants doivent rester verts |
| `DocumentService` (côté frontend) | Aucun changement de signature | — |

### Smoke tests E2E concernés

- [ ] `e2e/smoke/case-file.spec.ts` (si existant) — upload PDF doit toujours fonctionner
- [ ] `e2e/smoke/upload-image.spec.ts` (à créer) — upload JPG → vignette → succès

---

## Dépendances

### Subfeatures bloquantes

- **SF-230-01 backend** doit être mergée pour que l'upload soit traité côté serveur. Mais pour les tests Jest frontend (qui mockent le backend), pas de blocage de dev.

### Contrat API importé

- `POST /api/v1/case-files/{id}/documents` (multipart) — inchangé, signature identique au flow PDF actuel.
- Réponse 201 + `Document` JSON.
- Erreurs : 400 (`UNSUPPORTED_FORMAT`), 402 (`OCR_QUOTA_EXCEEDED`), 413 (`PAYLOAD_TOO_LARGE`).

---

## Notes et décisions

- **Limite 10 Mo client** : choix arbitraire pour éviter d'envoyer des images trop lourdes (photo iPhone 4K = 5-8 Mo en HEIC, on a de la marge). Côté serveur, la vraie limite est celle de `spring.servlet.multipart.max-file-size` (à vérifier — actuellement probablement 100 Mo pour les PDF).
- **HEIC affichage thumbnail** : les navigateurs ne décodent pas HEIC nativement. Pour le thumbnail preview, on affichera un placeholder "Image HEIC — sera traitée à l'upload" pour ces fichiers.
- **Pas de drag-and-drop V1** : laissé volontairement pour ne pas mélanger 2 chantiers (D&D nécessite refactoring du composant). À mettre au backlog si retour utilisateur.
