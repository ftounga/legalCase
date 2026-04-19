# Mini-spec — F-122 / SF-122-05 Bouton "Relancer avec OCR" (rétrocompat)

## Identifiant `F-122 / SF-122-05`  · Statut `draft`  · Date `2026-04-19`
## Branche `feat/SF-122-05-ocr-retry-retrocompat`

---

## Objectif

Permettre à l'avocat de relancer l'OCR sur les documents d'un dossier qui ont déjà échoué (extractionStatus=FAILED, motifs EMPTY_TEXT ou OCR_FAILED) — sans avoir à les supprimer et re-uploader. Évite à la fois la perte de données (le fichier reste en S3) et la consommation quota silencieuse au déploiement de F-122.

---

## Comportement

### Endpoints

**GET `/api/v1/case-files/{id}/ocr-retry-preview`** — renvoie :
```json
{
  "failedDocsCount": 9,
  "estimatedPages": 45,
  "monthlyRemaining": 755,
  "packsRemaining": 0,
  "canRetry": true
}
```
- `estimatedPages` = somme des `PDFBox.getNumberOfPages()` des docs FAILED éligibles
- `canRetry` = `failedDocsCount > 0 && estimatedPages ≤ monthlyRemaining + packsRemaining`

**POST `/api/v1/case-files/{id}/ocr-retry`** — relance :
- Identifie les docs `FAILED` avec `failureReason ∈ { EMPTY_TEXT, OCR_FAILED }`
- Supprime leurs `DocumentExtraction` (cascade)
- Republie `DocumentUploadedEvent` par doc → `ExtractionService` retente → `OcrService.tryOcr` (avec gate quota, peut repartir en FAILED motif OCR_QUOTA_EXCEEDED)
- Réponse `{ retryedCount: N }`

### UI

Bandeau discret en haut du dossier (`case-file-detail`) quand ≥ 1 doc `FAILED` avec motif éligible :

> ⚠️ **N documents non analysables** — [Relancer avec OCR (~M pages · X restantes)] [Ignorer]

Clic → `MatDialog` confirmation détaillant l'impact quota. Si confirmation → POST + snackbar "N documents en cours de re-extraction".

### Rate limit

Au niveau endpoint : max 1 retry par dossier / 10 min. Stockage simple via cache in-memory `Map<caseFileId, Instant>` (perd au redémarrage, acceptable V1).

### Cas limite

| Situation | Comportement |
|---|---|
| Aucun doc FAILED éligible | `failedDocsCount=0, canRetry=false`, bandeau masqué |
| Quota insuffisant | `canRetry=false`, bouton grisé avec tooltip "Achetez un pack OCR" |
| Docs FAILED avec motif UNSUPPORTED_FORMAT / CORRUPTED / OCR_UNSUPPORTED_SIZE | Exclus du retry (OCR ne les aidera pas) |
| 2 retries consécutifs < 10 min | 2ème retry 429 Too Many Requests |
| `aws.textract.enabled=false` | Endpoint toujours disponible — OCR retournera EMPTY_TEXT pour chaque doc (comportement safe) |

---

## Critères d'acceptation

- [ ] `GET /case-files/{id}/ocr-retry-preview` fonctionnel, isolation workspace
- [ ] `POST /case-files/{id}/ocr-retry` supprime les extractions FAILED + republie events
- [ ] Rate limit 1/10 min par dossier (in-memory, pattern `Map<UUID, Instant>`)
- [ ] Filtre sur motifs éligibles (EMPTY_TEXT / OCR_FAILED seulement)
- [ ] Frontend : bandeau conditionnel + bouton + dialog confirmation + snackbar
- [ ] Quota insuffisant → bouton grisé, dialog bloqué
- [ ] Tests : 5+ backend, 2+ frontend

## Hors scope
- Relance OCR sur un seul doc (batch uniquement pour V1)
- Notification email post-retry — déjà géré par F-121-02 sur chaque doc

---

## Technique

| Fichier | Opération |
|---|---|
| `backend/src/main/java/fr/ailegalcase/ocr/OcrRetryController.java` | NOUVEAU |
| `backend/src/main/java/fr/ailegalcase/ocr/OcrRetryService.java` | NOUVEAU |
| `backend/src/main/java/fr/ailegalcase/ocr/OcrRetryPreviewResponse.java` | NOUVEAU |
| `backend/src/main/java/fr/ailegalcase/document/DocumentExtractionRepository.java` | + findByDocument_CaseFileIdAndExtractionStatus (ou équivalent) |
| Frontend : bandeau + dialog dans case-file-detail |
| Tests backend + frontend |

### Analyse d'impact

- [x] **Plans / limites** : lecture seule via `PlanLimitService.getMonthlyOcrPages` + gate existant. Aucun changement structurel.
- [ ] Auth / workspace / navigation / routing : non touchés.
