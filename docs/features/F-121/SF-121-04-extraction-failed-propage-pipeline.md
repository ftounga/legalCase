# Mini-spec — F-121 / SF-121-04 Propager toute extraction échouée sur la step "Analyse des documents"

## Identifiant
`F-121 / SF-121-04`

## Feature parente
`F-121` — Gestion visible des extractions échouées

## Statut
`draft`

## Date de création
`2026-04-19`

## Branche Git
`feat/SF-121-04-extraction-failed-propage-pipeline`

---

## Objectif

Faire basculer la step 2 "Analyse des documents" de la pipeline F-112 en `FAILED` **dès qu'un seul** document du dossier a `extractionStatus === 'FAILED'`, que ce soit **à l'upload**, **au refresh** ou en cas d'**échec partiel** (mix FAILED + DONE). Affiche clairement "N / M non analysables" dans le compteur. Couvre les 2 gaps restants de SF-121-03 :
1. **Refresh** de la page (docAnalysisPending=false) — aujourd'hui grey waiting
2. **Échec partiel** (3 FAILED / 6 DONE) — aujourd'hui step 2 green DONE, masque le problème → risque juridique (synthèse raisonnant dans le vide sur les pièces clés manquantes).

Règle simple : **≥ 1 doc FAILED ⟹ step 2 FAILED**. Pas de distinction "tous failed" vs "partiel".

---

## Comportement attendu

### Cas nominal

Sur un dossier avec au moins 1 doc `extractionStatus === 'FAILED'` :
1. La step 2 "Analyse des documents" passe en **rouge/FAILED** (icône `error`, barre rouge 100 %).
2. Le compteur affiche **"N non analysables / M"** où N = nb docs FAILED, M = nb docs total.
3. Aucune autre step de la pipeline n'est affectée (step 3-5 continuent sur les docs lisibles si la synthèse se lance normalement côté backend).
4. Le polling s'arrête une fois l'état FAILED posé.

Se déclenche dans 3 contextes :
- **Upload direct** : polling tick détecte la condition dans les 3-6 s après upload.
- **Refresh** : `loadAnalysisJobs` + `loadDocuments` posent le virtuel après chargement initial.
- **Suppression / ajout de doc** : si le dossier ne contient plus aucun FAILED, le virtuel disparaît (comportement backend normal reprend).

### Cas limite

| Situation | Comportement |
|---|---|
| Aucun doc FAILED | Pipeline normale (backend job backend l'emporte) |
| Job backend DOCUMENT_ANALYSIS existe en PROCESSING/DONE + ≥ 1 doc FAILED | Frontend override step 2 en FAILED (visuellement) — le backend continue sur les docs lisibles en parallèle |
| Job backend DOCUMENT_ANALYSIS FAILED + docs FAILED | Comportement backend respecté, même affichage |
| 0 documents | Pas de virtuel posé (safeguard) |
| Upload en cours (`uploading() === true`) | Pas de détection (attendre la fin) |
| Doc avec `extractionStatus === 'PENDING' | 'PROCESSING'` (extraction pas encore terminée) | Pas de détection tant que le statut n'est pas stable (on attend FAILED ou DONE) |

### Compteur

Le compteur de la step 2 quand status=FAILED sur jobType=DOCUMENT_ANALYSIS affiche **"N non analysables / M"** pour signifier explicitement "N docs illisibles sur M". Implémenté dans `AnalysisPipelineComponent.counter()` avec un override localisé.

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — N/A. La pipeline UI est partagée tous domaines / tous pays.
- [x] **Autres pays** — N/A.
- [x] **Autres domaines** — N/A.
- [x] **Autres UI patterns** — Le pattern "override du counter selon jobType + status" est localisé à `AnalysisPipelineComponent`. Pas d'autre endroit où on fait ça actuellement. Pas d'extension nécessaire.
- [x] **Autres flows transversaux** — Aucun.

### Classification

| Cible | Applicable ? | Traitement |
|---|---|---|
| `AnalysisPipelineComponent.counter()` | Oui | **Intégré dans cette SF** — override pour FAILED DOCUMENT_ANALYSIS |
| Notifications F-113 / emails SF-121-02 | Non | Déjà faits per doc FAILED |
| Backend `ChunkingService` | Non | Inchangé — continue sur les docs DONE |

### Nouveau pattern UI ou service partagé

- [x] **Aucun nouveau pattern partagé.** L'override du compteur dans `AnalysisPipelineComponent.counter()` est un cas spécifique jobType+status, pas une refonte du système d'affichage. Le helper `checkAndPostVirtualFailedIfNeeded()` est local à `CaseFileDetailComponent`.

---

## Critères d'acceptation

- [ ] À l'upload, si ≥ 1 doc passe FAILED dans les 3-6 s, step 2 devient rouge avec compteur "N non analysables / M"
- [ ] Au refresh d'un dossier existant avec ≥ 1 doc FAILED et aucun job backend, même comportement
- [ ] En échec partiel (3 FAILED + 6 DONE, backend job DONE), step 2 est override en FAILED avec "3 non analysables / 9"
- [ ] En échec total (9 FAILED), step 2 est FAILED avec "9 non analysables / 9"
- [ ] Si aucun doc n'est FAILED, la pipeline fonctionne normalement (pas de virtuel posé, backend l'emporte)
- [ ] Polling s'arrête une fois l'état FAILED posé
- [ ] Le compteur "N non analysables / M" n'apparaît que sur DOCUMENT_ANALYSIS FAILED — les autres FAILED (CASE_ANALYSIS, QUESTION_GENERATION, etc.) gardent l'ancien format "N / M"
- [ ] Build frontend vert, suite de tests complète verte

---

## Périmètre

### Hors scope (explicite)

- Backend — aucun changement
- Propagation de FAILED sur les steps 3-5 (synthèse, questions, enrichie) — la synthèse sur les docs lisibles reste valide
- Bandeau global "N documents non analysables" en tête de page — les badges per-doc + step pipeline suffisent
- F-122 OCR — SF séparée qui récupérera les docs FAILED

---

## Technique

### Composants impactés

| Fichier | Opération |
|---|---|
| `frontend/src/app/case-files/case-file-detail/case-file-detail.component.ts` | Remplacer `detectAllExtractionsFailed` par `checkAndPostVirtualFailedIfNeeded` (règle ≥ 1 FAILED) + 3 call-sites |
| `frontend/src/app/case-files/case-file-detail/case-file-detail.component.spec.ts` | Mettre à jour les 4 tests SF-121-03 + ajouter cas partial |
| `frontend/src/app/case-files/analysis-pipeline/analysis-pipeline.component.ts` | Override `counter()` pour FAILED DOCUMENT_ANALYSIS |
| `frontend/src/app/case-files/analysis-pipeline/analysis-pipeline.component.spec.ts` | Ajouter test counter override |

### Endpoints / Tables

Aucun changement.

### Migration Liquibase

- [ ] Non

---

## Plan de test

### Tests unitaires (case-file-detail)

- [ ] U-CFD-121-04-01 — 9 FAILED / 0 DONE → step 2 FAILED "9 non analysables / 9"
- [ ] U-CFD-121-04-02 — 3 FAILED / 6 DONE (pas de job backend) → virtuel FAILED "3 non analysables / 9"
- [ ] U-CFD-121-04-03 — 3 FAILED / 6 DONE + job backend DONE → override en FAILED avec compteur
- [ ] U-CFD-121-04-04 — 0 FAILED → pas de virtuel, backend l'emporte
- [ ] U-CFD-121-04-05 — 1 PENDING + autres FAILED → pas de virtuel (extractions pas stables)
- [ ] U-CFD-121-04-06 — 0 docs → pas de virtuel (safeguard)

### Tests unitaires (analysis-pipeline)

- [ ] U-AP-04-01 — DOCUMENT_ANALYSIS FAILED → counter "X non analysables / Y"
- [ ] U-AP-04-02 — CASE_ANALYSIS FAILED → counter reste "X / Y" (pas d'override)

### Régressions

- [ ] Tests existants `U-CFD-B*`, `U-CFD-124-*`, `U-CFD-121-03-*` adaptés et verts
- [ ] Full frontend suite verte

### Isolation workspace

- [x] N/A (logique frontend locale)

---

## Analyse d'impact

- [x] Aucune préoccupation transversale (pas d'auth, pas de workspace context nouveau, pas de plan/quota, pas de routing).
