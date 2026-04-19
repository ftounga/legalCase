# Mini-spec — F-122 / SF-122-02 Quotas OCR par plan + hard cap journalier

## Identifiant
`F-122 / SF-122-02`

## Feature parente
`F-122` — OCR pour PDF scannés (AWS Textract)

## Statut
`draft`

## Date de création
`2026-04-19`

## Branche Git
`feat/SF-122-02-ocr-quotas-hard-caps`

---

## Objectif

Ajouter l'enforcement des quotas OCR pour éviter tout coût AWS non couvert par un paiement utilisateur. Deux gates :
1. **Quota mensuel par plan** : FREE 100 / SOLO 800 / TEAM 3 000 / PRO 10 000 pages/mois.
2. **Hard cap journalier anti-abus** : 500 pages/workspace/jour, tous plans confondus (garde-fou contre un cabinet qui uploade 5 000 pages d'un coup).

Quand un gate est atteint et que l'avocat n'a **aucun pack overage** disponible (SF-122-04 à venir), l'extraction passe `FAILED` avec motif `OCR_QUOTA_EXCEEDED` → F-121 affiche le badge + lien vers `/billing`.

---

## Comportement attendu

### Cas nominal

Avant l'appel Textract (après les checks taille/pages de SF-122-01) :
1. Lire le `planCode` du workspace via `SubscriptionRepository`.
2. Lire `workspace.ocr_pages_used_current_month` (effectif : 0 si mois écoulé).
3. Lire `workspace.ocr_pages_used_current_day` (effectif : 0 si jour écoulé).
4. Estimer les pages consommées : `PDFBox.getNumberOfPages(fileBytes)`.
5. Gates (dans l'ordre, le premier qui échoue emporte le motif) :
   - `current_day + estimatedPages > 500` → `OCR_QUOTA_EXCEEDED` (hard cap journalier)
   - `current_month + estimatedPages > plan_limit` → `OCR_QUOTA_EXCEEDED` (quota mensuel)
6. Si OK → appel Textract normal (SF-122-01).
7. Sur succès, incrémenter les compteurs par **page count réel** retourné par Textract (peut différer légèrement de l'estimation PDFBox).

### Reset des compteurs

Géré **dans la query `incrementOcrUsage`** (pas de scheduled task) :
- Si `ocr_usage_last_reset_date` est dans un mois passé → `ocr_pages_used_current_month = :pages` (reset).
- Si `ocr_usage_last_reset_date` ≠ `:today` → `ocr_pages_used_current_day = :pages` (reset).

Lecture-side (dans `PlanLimitService`) : si `ocr_usage_last_reset_date` est dans un mois passé, `current_month` est considéré comme 0 (même si stocké > 0, l'UPDATE suivant le remettra à zéro).

### Cas d'erreur

| Situation | Comportement | Motif |
|---|---|---|
| Plan FREE, 100 pages déjà consommées, nouveau PDF de 2 pages | Blocked, pas d'appel AWS | `OCR_QUOTA_EXCEEDED` |
| Plan PRO, 9 999 pages consommées, PDF de 5 pages | Blocked | `OCR_QUOTA_EXCEEDED` |
| Plan TEAM, 250 pages aujourd'hui, PDF de 300 pages (total 550 > 500) | Blocked (hard cap jour) | `OCR_QUOTA_EXCEEDED` |
| Plan TEAM, 499 pages aujourd'hui, PDF de 1 page (total 500) | OK (≤ 500) | — |
| Workspace sans subscription | Blocked (défaut = plan FREE) | `OCR_QUOTA_EXCEEDED` si `current_month >= 100` |
| `aws.textract.enabled=false` | Gate pas atteint (OCR skip avant) | `EMPTY_TEXT` (comportement SF-122-01) |

### Enforcement via `creditPurchaseService` (packs overage)

**Hors scope SF-122-02.** Les packs overage OCR arrivent en SF-122-04. Tant qu'ils n'existent pas, le gate est strict : dépassement mensuel → `OCR_QUOTA_EXCEEDED`. En SF-122-04, le gate consultera `creditPurchaseService.getOcrPagesBought(workspaceId)` pour étendre la limite.

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — N/A.
- [x] **Autres pays** — N/A. Quotas identiques tous pays.
- [x] **Autres domaines** — N/A.
- [x] **Autres UI patterns** — Pas de changement UI cette SF (le badge "Non analysable" + tooltip couvre déjà le motif via `extractionFailureLabel`).
- [x] **Autres flows transversaux** — Touche à **Plans / limites** : nouveau gate dans `PlanLimitService`. À traiter dans l'analyse d'impact (ci-dessous).

### Classification

| Cible | Applicable ? | Traitement |
|---|---|---|
| `PlanLimitService` | Oui | **Intégré** — 3 nouvelles constantes + 2 méthodes |
| `OcrService.tryOcr` | Oui | **Intégré** — signature étendue avec workspaceId, gate pré-Textract |
| `ExtractionService` | Oui | **Intégré** — passe workspaceId à OcrService |
| `ExtractionFailureReason` | Oui | + `OCR_QUOTA_EXCEEDED` |
| `extractionFailureLabel` frontend | Oui | + libellé humain |
| `ExtractionNotificationService.humanLabel` | Oui | + case switch exhaustif |

### Nouveau pattern UI ou service partagé

- [x] **Pas de nouveau service partagé.** L'extension est limitée à `PlanLimitService` (service existant). Le gate reste local à `OcrService`.

---

## Critères d'acceptation

- [ ] Constantes ajoutées dans `PlanLimitService` : `FREE_MONTHLY_OCR_PAGES=100`, `SOLO=800`, `TEAM=3_000`, `PRO=10_000`, `DAILY_OCR_PAGES_HARD_CAP=500`
- [ ] Méthode `getMonthlyOcrPages(planCode)` retourne la bonne valeur selon le plan
- [ ] Méthode `isOcrQuotaExceeded(workspaceId, additionalPages)` vraie si l'ajout dépasserait le quota mensuel OU le hard cap journalier
- [ ] La méthode gère le cas "compteur stale" : si `ocr_usage_last_reset_date` est dans un mois passé → current_month effectif = 0
- [ ] La méthode gère le cas "compteur stale journalier" : si `ocr_usage_last_reset_date` ≠ aujourd'hui → current_day effectif = 0
- [ ] `OcrService.tryOcr(fileBytes, workspaceId)` check le quota avant tout appel Textract
- [ ] Quota mensuel dépassé → `OcrResult.failure(OCR_QUOTA_EXCEEDED)`, zéro appel Textract
- [ ] Hard cap journalier dépassé → `OcrResult.failure(OCR_QUOTA_EXCEEDED)`, zéro appel Textract
- [ ] `incrementOcrUsage` réinitialise le compteur mensuel quand le mois change (même atomicité que daily)
- [ ] `ExtractionFailureReason.OCR_QUOTA_EXCEEDED` ajouté + libellé frontend + `humanLabel` backend
- [ ] Build backend vert, 10+ nouveaux tests verts
- [ ] Tests existants F-121 / SF-122-01 restent verts

---

## Périmètre

### Hors scope (explicite)

- Packs overage Stripe — SF-122-04
- Bouton "Acheter des pages OCR" — SF-122-04
- Affichage du compteur dans `/workspace/billing` — SF-122-04
- Mode FORMS ×3 quota — SF-122-03
- Notification email "Vous approchez de votre quota" — V2

---

## Technique

### Composants impactés

| Fichier | Opération |
|---|---|
| `backend/src/main/java/fr/ailegalcase/billing/PlanLimitService.java` | + constantes + 2 méthodes |
| `backend/src/main/java/fr/ailegalcase/document/ExtractionFailureReason.java` | + `OCR_QUOTA_EXCEEDED` |
| `backend/src/main/java/fr/ailegalcase/document/ExtractionNotificationService.java` | + case switch exhaustif |
| `backend/src/main/java/fr/ailegalcase/ocr/OcrService.java` | signature `tryOcr(byte[], UUID)` + gate |
| `backend/src/main/java/fr/ailegalcase/document/ExtractionService.java` | passe workspaceId |
| `backend/src/main/java/fr/ailegalcase/workspace/WorkspaceRepository.java` | query `incrementOcrUsage` étendue (reset mensuel) |
| `backend/src/test/java/fr/ailegalcase/billing/PlanLimitServiceTest.java` | + tests quotas OCR |
| `backend/src/test/java/fr/ailegalcase/ocr/OcrServiceTest.java` | + tests gate quota |
| `backend/src/test/java/fr/ailegalcase/document/ExtractionServiceTest.java` | + 1 test intégration signature étendue |
| `frontend/src/app/core/models/document.model.ts` | + libellé `OCR_QUOTA_EXCEEDED` |

### Endpoints / Tables

Aucun nouveau. Tables : aucune modification de schéma (les 3 colonnes ont été ajoutées par SF-122-01).

### Migration Liquibase

- [ ] Non

---

## Plan de test

### Tests unitaires (PlanLimitServiceTest)

- [ ] U-PLS-OCR-01 — `getMonthlyOcrPages("FREE")` → 100
- [ ] U-PLS-OCR-02 — `getMonthlyOcrPages("SOLO")` → 800
- [ ] U-PLS-OCR-03 — `getMonthlyOcrPages("TEAM")` → 3000
- [ ] U-PLS-OCR-04 — `getMonthlyOcrPages("PRO")` → 10000
- [ ] U-PLS-OCR-05 — `isOcrQuotaExceeded(ws, 5)` avec ws=SOLO current_month=800 → true (dépasserait)
- [ ] U-PLS-OCR-06 — `isOcrQuotaExceeded(ws, 5)` avec ws=SOLO current_month=790 → false (790+5=795 < 800)
- [ ] U-PLS-OCR-07 — compteur mensuel stale (lastReset mois passé) traité comme 0 → pas de dépassement
- [ ] U-PLS-OCR-08 — hard cap journalier : current_day=450, additionalPages=60 → true (510 > 500)
- [ ] U-PLS-OCR-09 — compteur journalier stale (lastReset hier) → current_day effectif = 0

### Tests unitaires (OcrServiceTest)

- [ ] U-OCR-02-01 — quota dépassé → pas d'appel Textract, motif `OCR_QUOTA_EXCEEDED`
- [ ] U-OCR-02-02 — quota OK → appel Textract normal

### Tests (ExtractionServiceTest)

- [ ] U-EXT-OCR-05 — PDF vide + quota OK mais Textract success → DONE + compteur incrémenté (régression SF-122-01 avec nouvelle signature)

### Régressions

- [ ] OcrServiceTest existant (7 TU) adapté à la nouvelle signature, reste vert
- [ ] ExtractionServiceTest existant (11 TU) reste vert
- [ ] Full backend vert

### Isolation workspace

- [x] Applicable — le gate utilise le workspaceId du dossier du document. Chaque workspace a son propre compteur.

---

## Analyse d'impact

### Préoccupations transversales

- [ ] **Auth / Principal** — non touché
- [ ] **Workspace context** — lecture du workspace.ocr_pages_used_* (non modifiés structurellement, juste lus par le gate)
- [x] **Plans / limites** — **TOUCHÉ**. Nouveau gate dans `PlanLimitService`. Composants impactés listés :
  - `OcrService` (nouveau consumer)
  - Pas de nouveau call-site dans `CaseAnalysisCommandService`, `ChunkAnalysisService`, `ReAnalysisCommandService` (ces services ne concernent pas l'OCR)
  - `CreditPurchaseService` non touché cette SF (SF-122-04)
- [ ] **Navigation / routing** — non touché

### Smoke tests E2E

- [x] Aucun smoke E2E directement impacté. `e2e/smoke/auth.spec.ts`, `workspace.spec.ts`, `navigation.spec.ts` non affectés (pas d'auth, pas de workspace resolution, pas de routing).

### Risque de régression

- Faible. Le gate n'intervient que sur le chemin OCR (SF-122-01). Les docs non-PDF et les PDF lisibles ne sont pas affectés.
- Sur staging avec `AWS_TEXTRACT_ENABLED=false`, le gate n'est même pas atteint (OcrService court-circuite avant).
