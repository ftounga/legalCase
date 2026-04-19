# Mini-spec — F-122 / SF-122-01 Backend Textract fallback (mode TABLES synchrone)

## Identifiant
`F-122 / SF-122-01`

## Feature parente
`F-122` — OCR pour PDF scannés (AWS Textract)

## Statut
`draft`

## Date de création
`2026-04-19`

## Branche Git
`feat/SF-122-01-backend-textract-fallback`

---

## Objectif

Rendre les PDF scannés (images sans couche texte) à nouveau analysables en appelant AWS Textract (mode `AnalyzeDocument` feature `TABLES`) en fallback quand `PDFTextStripper` renvoie un texte vide. Sur succès, le texte OCR est persisté, l'extraction passe `DONE`, la pipeline IA reprend. Compteur `ocr_pages_used` incrémenté (mensuel + journalier) — **sans enforcement de quota** (SF-122-02 suivra).

---

## Comportement attendu

### Cas nominal

1. Upload d'un PDF scanné (contentType = `application/pdf`).
2. `ExtractionService.extract()` → `PDFTextStripper` → texte vide (ou blank).
3. `OcrService.fallback(fileBytes, documentId, workspaceId)` appelé :
   - Check taille ≤ 5 MB et ≤ 11 pages → sinon `OCR_UNSUPPORTED_SIZE`
   - Appel `textractClient.analyzeDocument(...)` synchrone avec feature `TABLES`
   - Extraction du texte depuis les `Block` type `LINE`
   - Calcul du page count via `blocks.stream().mapToInt(b -> b.page() orElse 1).max()`
   - Incrémente `workspaces.ocr_pages_used_current_month += pageCount`
   - Incrémente `workspaces.ocr_pages_used_current_day += pageCount` (avec reset journalier automatique si date changée)
4. Retour : texte OCR ou échec.
5. Si succès : `extraction.setExtractedText(ocrText)`, status `DONE`, publish `ExtractionDoneEvent` → pipeline IA reprend normalement.
6. Si échec : status `FAILED` avec motif approprié (`OCR_FAILED`, `OCR_UNSUPPORTED_SIZE`, ou `EMPTY_TEXT` si OCR n'a rien trouvé non plus).

### Cas d'erreur

| Situation | Comportement | Motif |
|---|---|---|
| Texte vide + doc > 5 MB | Pas d'appel Textract | `OCR_UNSUPPORTED_SIZE` |
| Texte vide + doc > 11 pages | Pas d'appel Textract | `OCR_UNSUPPORTED_SIZE` |
| Texte vide + contentType non-PDF (docx, txt) | Pas d'appel Textract (OCR ne s'applique pas) | `EMPTY_TEXT` (comportement SF-121-01 conservé) |
| Texte vide + Textract lève `UnsupportedDocumentException` ou `BadDocumentException` | Pas de retry | `OCR_FAILED` |
| Texte vide + Textract lève `ThrottlingException` / `ProvisionedThroughputExceededException` | Pas de retry V1 (à revoir en V2) | `OCR_FAILED` |
| Texte vide + Textract renvoie 0 blocks (image sans texte) | Rien à persister | `EMPTY_TEXT` (OCR a trouvé du vide aussi — doc probablement non-textuel) |
| Texte vide + AWS SDK timeout / network | Exception propagée puis catchée, FAILED | `OCR_FAILED` |
| `textract.enabled: false` en config | OCR skip, comportement SF-121-01 | `EMPTY_TEXT` |

### Non-applicable (hors scope SF-122-01)

- Pas de quota enforcement (SF-122-02) — on **trace** l'usage sans le bloquer.
- Pas de mode FORMS (SF-122-03) — uniquement TABLES par défaut.
- Pas de packs overage (SF-122-04).
- Pas de bouton de rétrocompat (SF-122-05) — les docs déjà FAILED restent FAILED pour l'instant.

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — N/A, modification du pipeline d'extraction partagé tous domaines/pays.
- [x] **Autres pays** — N/A. OCR agit sur le contenu binaire, indépendant du pays.
- [x] **Autres domaines** — N/A.
- [x] **Autres UI patterns** — N/A (backend only).
- [x] **Autres flows transversaux** — Touche à **Workspace** (nouvelles colonnes de compteurs). Pas de changement Auth/Principal, pas de changement du workspace context lui-même. Cf. section "Analyse d'impact".

### Classification

| Cible | Applicable ? | Traitement |
|---|---|---|
| `ExtractionService.extract` | Oui | Intégré dans cette SF — branche OCR sur texte vide |
| `ExtractionFailureReason` enum | Oui | + `OCR_FAILED`, `OCR_UNSUPPORTED_SIZE` |
| `extractionFailureLabel()` frontend | Oui | Ajouter libellés — SF-122-03 (côté UI FORMS) OU un petit ajout frontend ici. **Décision** : ajouté ici (petit changement, cohérence UX immédiate). |
| `ChunkingService` | Non | Consomme déjà `ExtractionDoneEvent` — inchangé |
| `EmailService` / F-121-02 | Non | Reçoit déjà `ExtractionFailedEvent` avec nouveau motif — libellé à ajouter |
| Frontend `document.model.ts` labels | Oui | Ajouter 2 motifs |

### Nouveau pattern UI ou service partagé

- [x] **Nouveau service** : `OcrService` (Spring `@Service`). Point d'entrée unique pour tout appel Textract futur. **Pas de réutilisation envisagée ailleurs** dans le produit V1 — OCR est strictement lié à l'extraction de documents.
- [x] **Pas de nouveau pattern UI** — le badge "Non analysable" existant (SF-121-02) couvre déjà les nouveaux motifs via `extractionFailureLabel()`.

---

## Critères d'acceptation

- [ ] PDF scanné ≤ 5 MB / ≤ 11 pages avec texte vide → OCR appelé → texte retourné, extraction `DONE`
- [ ] Compteur `ocr_pages_used_current_month` incrémenté du `pageCount`
- [ ] Compteur `ocr_pages_used_current_day` incrémenté avec reset auto si la date a changé
- [ ] Migration Liquibase 082 crée les 3 nouvelles colonnes sur `workspaces`
- [ ] PDF > 5 MB ou > 11 pages → pas d'appel Textract → FAILED motif `OCR_UNSUPPORTED_SIZE`
- [ ] Non-PDF (docx, txt) avec contenu vide → pas d'appel Textract → FAILED motif `EMPTY_TEXT` (comportement SF-121-01 conservé)
- [ ] Textract lève exception → FAILED motif `OCR_FAILED` + log ERROR avec détail
- [ ] Textract retourne 0 blocks → FAILED motif `EMPTY_TEXT`
- [ ] `textract.enabled: false` → OCR skip, comportement SF-121-01 exact
- [ ] `ExtractionDoneEvent` publié si OCR DONE, pipeline IA reprend
- [ ] Build backend vert, nouveaux tests OcrService + intégration ExtractionService verts
- [ ] Frontend : `extractionFailureLabel` étendu avec 2 nouveaux motifs, label humain explicite

---

## Périmètre

### Hors scope (explicite)

- Enforcement de quota mensuel/journalier — SF-122-02
- Mode FORMS (checkbox UI + backend ×3) — SF-122-03
- Packs overage Stripe — SF-122-04
- Bouton rétrocompat dossier "Relancer avec OCR" — SF-122-05
- API asynchrone Textract (`StartDocumentAnalysis`) — reste en sync V1. Documents > 5 MB ou > 11 pages partent en `OCR_UNSUPPORTED_SIZE`. Couverture >90 % des cas réels cabinets. Async = itération future si besoin.
- Retry sur throttling — V2
- Métriques Prometheus / dashboard usage OCR — au plus tard SF-122-04 dans la section billing

---

## Technique

### Dépendance Maven

Ajouter au `backend/pom.xml` :
```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>textract</artifactId>
</dependency>
```
(la BOM AWS SDK 2.x est déjà gérée via `bomImport` dans dependencyManagement)

### Configuration

`application.yml` (défauts) :
```yaml
aws:
  textract:
    enabled: false
    region: eu-west-3
    max-size-mb: 5
    max-pages: 11
```

Override par profil :
- `application-dev.yml` : `enabled: false` (pas de coût AWS en dev)
- `application-staging.yml` / `application-prod.yml` : `enabled: true`

### Composants impactés

| Fichier | Opération |
|---|---|
| `backend/pom.xml` | + dépendance Textract |
| `backend/src/main/resources/application.yml` | + section `aws.textract` |
| `backend/src/main/java/fr/ailegalcase/ocr/OcrService.java` | **NOUVEAU** |
| `backend/src/main/java/fr/ailegalcase/ocr/OcrProperties.java` | **NOUVEAU** (`@ConfigurationProperties`) |
| `backend/src/main/java/fr/ailegalcase/ocr/OcrResult.java` | **NOUVEAU** (record : `text`, `pageCount`, `success`, `motif`) |
| `backend/src/main/java/fr/ailegalcase/document/ExtractionService.java` | Intègre le fallback OCR |
| `backend/src/main/java/fr/ailegalcase/document/ExtractionFailureReason.java` | + 2 enums |
| `backend/src/main/java/fr/ailegalcase/workspace/Workspace.java` | + 3 colonnes |
| `backend/src/main/java/fr/ailegalcase/workspace/WorkspaceRepository.java` | + `incrementOcrUsage(...)` atomique |
| `backend/src/main/resources/db/changelog/migrations/082-add-ocr-usage-to-workspaces.xml` | **NOUVEAU** |
| `backend/src/main/resources/db/changelog/db.changelog-master.xml` | + ref 082 |
| `backend/src/test/java/fr/ailegalcase/ocr/OcrServiceTest.java` | **NOUVEAU** |
| `backend/src/test/java/fr/ailegalcase/document/ExtractionServiceOcrIT.java` | **NOUVEAU** (ou augmentation d'un test existant) |
| `frontend/src/app/core/models/document.model.ts` | + 2 libellés `extractionFailureLabel` |

### Endpoints

Aucun nouveau (le fallback se déclenche dans le flux d'extraction existant).

### Migration Liquibase

- [x] Oui — `082-add-ocr-usage-to-workspaces.xml`
- Colonnes ajoutées :
  - `ocr_pages_used_current_month INT NOT NULL DEFAULT 0`
  - `ocr_pages_used_current_day INT NOT NULL DEFAULT 0`
  - `ocr_usage_last_reset_date DATE NULL` (pour détecter le reset journalier)
- Rollback : `dropColumn` × 3

### Stockage des credentials AWS

- Production / Staging : IAM Role via IRSA (Service Account EKS) — pas de credentials dans le code ni en variable d'environnement. Rôle à créer côté Terraform (hors scope SF-122-01, noté comme prérequis déploiement).
- Dev / tests : `textract.enabled: false` → aucun appel, aucun credential requis.

### API Textract utilisée

- `textractClient.analyzeDocument(AnalyzeDocumentRequest.builder().document(Document.builder().bytes(SdkBytes.fromByteArray(fileBytes)).build()).featureTypes(FeatureType.TABLES).build())`
- Synchrone, 5 MB / 11 pages max (limites AWS)
- Extraction : `response.blocks().stream().filter(b -> b.blockType() == BlockType.LINE).map(Block::text).collect(joining("\n"))`
- Page count : `response.blocks().stream().mapToInt(b -> b.page() != null ? b.page() : 1).max().orElse(1)`

---

## Plan de test

### Tests unitaires (OcrServiceTest)

- [ ] U-OCR-01-01 — PDF ≤ 5 MB / ≤ 11 pages + Textract retourne blocks → `OcrResult.success=true`, texte concaténé, pageCount
- [ ] U-OCR-01-02 — PDF > 5 MB → pas d'appel Textract, `success=false`, motif `OCR_UNSUPPORTED_SIZE`
- [ ] U-OCR-01-03 — PDF > 11 pages (détecté via `PDFBox.getNumberOfPages`) → idem
- [ ] U-OCR-01-04 — Textract lève `UnsupportedDocumentException` → `success=false`, motif `OCR_FAILED`
- [ ] U-OCR-01-05 — Textract renvoie 0 blocks → `success=false`, motif `EMPTY_TEXT`
- [ ] U-OCR-01-06 — `textract.enabled=false` → `success=false` immédiat, motif `EMPTY_TEXT`, pas d'appel SDK

### Tests d'intégration (ExtractionServiceOcrIT)

- [ ] I-ES-OCR-01 — PDF scanné upload → PDFBox empty → OcrService mocké DONE → extraction DONE, ExtractionDoneEvent publié, compteur workspace incrémenté
- [ ] I-ES-OCR-02 — PDF scanné upload → PDFBox empty → OcrService mocké FAILED → extraction FAILED motif OCR_FAILED, ExtractionFailedEvent publié
- [ ] I-ES-OCR-03 — Doc non-PDF (docx vide) → pas d'appel OcrService → comportement SF-121-01 conservé
- [ ] I-ES-OCR-04 — 2 appels OCR consécutifs même workspace même jour → compteur mensuel=N+M, journalier=N+M
- [ ] I-ES-OCR-05 — Compteur journalier : changement de jour simulé → reset à 0 avant incrément

### Régressions

- [ ] Tests existants `ExtractionServiceTest` / IT restent verts
- [ ] Tests F-121 (`U-CFD-121-*`, `ExtractionNotificationService*`) restent verts
- [ ] Build full backend vert

### Isolation workspace

- [x] Applicable — le compteur OCR est attaché au workspace du dossier du document. Le test I-ES-OCR-04 vérifie indirectement l'isolation.

---

## Analyse d'impact

### Préoccupations transversales

- [ ] **Auth / Principal** — non touché
- [ ] **Workspace context** — touché : nouvelles colonnes, nouvelle méthode atomique `incrementOcrUsage` sur `WorkspaceRepository`. Composants consommant le workspace non modifiés. À vérifier : aucun code de résolution du workspace modifié (c'est juste une colonne supplémentaire).
- [ ] **Plans / limites** — non touché V1 (quota enforcement = SF-122-02). Les colonnes existent mais aucun gate. À vérifier : `PlanLimitService` inchangé.
- [ ] **Navigation / routing** — non touché

### Smoke tests E2E

- [x] Aucun smoke E2E impacté (pas d'UI, pas de route, pas de guard touchés). La suite `e2e/smoke/` ne sera pas affectée par cette SF.

### Composants impactés identifiés

- `Workspace` entité + repo → modifiés (3 colonnes + 1 méthode)
- `ExtractionService` → modifié (branche fallback)
- `ExtractionFailureReason` enum → 2 valeurs ajoutées
- `document.model.ts` frontend → 2 libellés ajoutés
- Aucun autre composant

**Risque de régression** : faible. Le flux d'extraction actuel est inchangé sauf sur la branche "texte vide + contentType PDF + OCR enabled" qui est nouvelle. Le dev/test garde `enabled=false` donc comportement identique à aujourd'hui.
