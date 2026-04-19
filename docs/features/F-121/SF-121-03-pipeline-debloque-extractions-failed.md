# Mini-spec — F-121 / SF-121-03 Débloquer la pipeline UI quand toutes les extractions échouent

## Identifiant
`F-121 / SF-121-03`

## Feature parente
`F-121` — Gestion visible des extractions échouées

## Statut
`draft`

## Date de création
`2026-04-19`

## Branche Git
`feat/SF-121-03-pipeline-debloque-extractions-failed`

---

## Objectif

Quand **toutes** les extractions d'un dossier échouent (PDF scannés sans OCR, formats non supportés, corruption), la pipeline "Analyse des documents" (F-112) doit afficher un état `FAILED` explicite **au lieu de rester en `PENDING` à vie**. Aujourd'hui, le placeholder frontend `DOCUMENT_ANALYSIS` posé à l'upload attend un job backend qui n'existera jamais (SF-121-01 ne déclenche plus `ExtractionDoneEvent` sur FAILED) → polling infini, sablier pointillé permanent.

Bug reproduit 2026-04-19 staging, dossier `a075c2b8...` (MEA AVOCATS), 9/9 PDF scannés.

---

## Comportement attendu

### Cas nominal

À l'upload d'un ou plusieurs documents :
1. Le placeholder `DOCUMENT_ANALYSIS PENDING` est posé (comportement actuel conservé).
2. Le polling `loadAnalysisJobs` vérifie toutes les 3 s :
   - **s'il existe un job backend `DOCUMENT_ANALYSIS`** → comportement actuel, la pipeline suit le job (progression, DONE, FAILED).
   - **s'il n'existe pas de job** ET **tous les documents du dossier ont `extractionStatus === 'FAILED'`** → le placeholder est remplacé par un job virtuel `DOCUMENT_ANALYSIS` avec `status = 'FAILED'`, `totalItems = N`, `processedItems = N`, `progressPercentage = 100`. `docAnalysisPending` passe à `false`. Le polling s'arrête.
3. La pipeline step 2 "Analyse des documents" passe en rouge/failed (icône `error`, barre rouge 100%), conformément au design F-112 (`status=FAILED` → rouge).

### Cas mixte (partiel)

Si une partie seulement des documents a échoué (ex. 5 FAILED + 4 DONE) et que le backend a créé un job `DOCUMENT_ANALYSIS` → comportement actuel inchangé (le job suit son cours sur les 4 docs analysables). Le placeholder virtuel n'est posé **que** si **100 %** des documents sont FAILED (pas de documents `DONE`).

### Cas limite

| Situation | Comportement attendu |
|---|---|
| Aucun document uploadé (dossier vide) | Pas de placeholder posé (comportement actuel) |
| Upload en cours (`uploading() === true`) | Pas de détection (attendre la fin de l'upload) |
| Au moins un doc avec `extractionStatus === 'DONE'` | Pas de placeholder virtuel — on attend le job backend normal |
| Au moins un doc avec `extractionStatus === 'PENDING' | 'PROCESSING'` | Pas de placeholder virtuel — on continue à poller normalement |
| Job backend `DOCUMENT_ANALYSIS` existe déjà | Le job backend l'emporte (pas d'override par le virtuel) |
| L'utilisateur supprime un document FAILED puis uploade un doc lisible | Le placeholder virtuel disparaît, le pipeline normal reprend |

### Message utilisateur

Le tooltip / légende de la step "Analyse des documents" en FAILED affiche : **"Aucun document analysable — extractions échouées (formats non lisibles ou scans sans couche texte). Retirez les documents non analysables ou uploadez des PDF avec texte sélectionnable."**

L'état `FAILED` est géré nativement par `AnalysisPipelineComponent` (F-112) via `stepStatus(job) === 'FAILED'` → icône `error` + barre rouge 100 %. Rien à modifier côté `AnalysisPipelineComponent`.

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — N/A. La pipeline UI est partagée tous domaines / tous pays — la garde s'applique à tous.
- [x] **Autres pays** — N/A. Le comportement est identique FR / BE (le pays n'intervient pas dans l'extraction).
- [x] **Autres domaines** — N/A (voir ci-dessus).
- [x] **Autres UI patterns** — Le pattern "job virtuel FAILED" est spécifique à cette garde. Pas de reuse envisagée ailleurs (les autres pipelines — CASE_ANALYSIS, QUESTION_GENERATION — ne sont pas déclenchées par des extractions).
- [x] **Autres flows transversaux** — Aucun (pas d'auth, pas de workspace, pas de plan/quota).

### Classification

| Cible | Applicable ? | Traitement |
|---|---|---|
| `AnalysisPipelineComponent` (F-112) | Oui, consomme le job FAILED | Aucun changement — déjà compatible `stepStatus='failed'` |
| `GlobalAnalysisNotificationService` (F-39-02) | Non — pas d'événement SSE à émettre | N/A |
| Notifications in-app F-113 | Non — F-121-02 envoie déjà un email par extraction FAILED | N/A |

### Nouveau pattern UI ou service partagé

- [x] **Aucun nouveau pattern partagé.** La garde est locale à `case-file-detail.component.ts`. Un job virtuel FAILED est une construction limitée à ce composant, consommée par `AnalysisPipelineComponent` sans modification. Pas de service à extraire.

---

## Critères d'acceptation

- [ ] Après upload de N documents, si les N extractions passent FAILED, la step "Analyse des documents" affiche l'icône `error` (rouge) dans les 3-6 s (1-2 cycles de polling).
- [ ] Le polling s'arrête (pas de requête toutes les 3 s une fois l'état FAILED posé).
- [ ] Si au moins 1 doc est DONE ou PENDING/PROCESSING, le placeholder virtuel n'est **pas** posé.
- [ ] Si un job backend `DOCUMENT_ANALYSIS` existe, il l'emporte sur le virtuel.
- [ ] Après suppression d'un doc FAILED et upload d'un doc lisible, le placeholder virtuel disparaît et le pipeline normal reprend.
- [ ] Aucune régression sur les dossiers où extraction = DONE (comportement actuel préservé).
- [ ] Tests unitaires sur la garde `allDocumentsFailed()` + intégration dans `managePolling`.
- [ ] Build frontend vert, 1014+ tests verts.

---

## Périmètre

### Hors scope (explicite)

- Backend — aucune modification. F-121-01 reste en place.
- F-122 OCR — feature séparée qui récupérera ces documents en les relisant via AWS Textract.
- Suppression automatique des documents FAILED — décision explicite de l'avocat, pas d'auto-delete.
- Message email différent — SF-121-02 envoie déjà un email par doc FAILED, suffisant.

---

## Technique

### Composants impactés

| Fichier | Opération |
|---|---|
| `frontend/src/app/case-files/case-file-detail/case-file-detail.component.ts` | Modifier `managePolling` + `loadAnalysisJobs` pour poser le job virtuel |
| `frontend/src/app/case-files/case-file-detail/case-file-detail.component.spec.ts` | Ajouter tests U-CFD-121-03-01 à 04 |

Pas de nouveau fichier.

### Endpoints / Tables

Aucun changement (lecture seule des endpoints existants `getJobs` + `list documents`).

### Migration Liquibase

- [ ] Non

---

## Plan de test

### Tests unitaires (case-file-detail.component.spec.ts)

- [ ] U-CFD-121-03-01 — 9 docs FAILED, aucun job backend → job virtuel FAILED posé, polling stoppé
- [ ] U-CFD-121-03-02 — 9 docs dont 1 DONE → pas de job virtuel, polling continue
- [ ] U-CFD-121-03-03 — 9 docs dont 1 PENDING → pas de job virtuel, polling continue
- [ ] U-CFD-121-03-04 — job backend existe → job virtuel n'écrase pas

### Régressions à surveiller

- [ ] Les tests existants du pipeline (U-CFD-B* et U-CFD-124-*) restent verts.
- [ ] Après une ré-analyse réussie, le dashboard refresh (F-124) continue de fonctionner.

### Isolation workspace

- [x] Non applicable (pas d'accès DB, logique frontend locale au dossier affiché).

---

## Analyse d'impact

- [x] **Aucune préoccupation transversale.** Pas de modification auth/Principal, workspace context, plans/limites, navigation/routing. Modification interne à un composant existant.
