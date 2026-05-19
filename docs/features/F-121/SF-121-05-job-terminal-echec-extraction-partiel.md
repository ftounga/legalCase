# Mini-spec — F-121 / SF-121-05 Job DOCUMENT_ANALYSIS terminal sur échec d'extraction partiel

## Identifiant

`F-121 / SF-121-05`

## Feature parente

`F-121` — Gestion visible des extractions échouées

## Statut

`ready`

## Date de création

2026-05-19

## Branche Git

`feat/SF-121-05-job-terminal-echec-extraction-partiel`

---

## Contexte — incident production 2026-05-19

Dossier `stanojevic` (workspace RENVERSEZ, prod). 17 documents uploadés ; 1 (`pièces stanojenic.pdf`,
PDF scanné 3,3 Mo) échoue l'extraction (`OCR_UNSUPPORTED_SIZE`). Le job `DOCUMENT_ANALYSIS` reste
bloqué `PROCESSING` 42 min puis tué par le `ZombieJobResetScheduler` → l'avocate voit un spinner
infini, ne peut ni terminer l'analyse ni supprimer le document fautif. Dossier débloqué manuellement
en prod le 2026-05-19 (relance synthèse + correction de la ligne `analysis_jobs`).

**Cause racine** : `DocumentAnalysisService.updateDocumentAnalysisJob` clôt le job quand
`count(DocumentAnalysis DONE) >= job.totalItems`, où `totalItems = documentRepository.countByCaseFileId`
(= **tous** les documents). Un document dont l'extraction échoue ne produit **jamais** de
`DocumentAnalysis` → le compteur plafonne sous `totalItems` → le job ne se termine jamais.
F-147 a câblé l'échec d'**analyse** sur le job ; F-121-03 a couvert le cas **tous** les documents
échouent ; **personne n'a couvert l'échec d'extraction partiel côté backend**.

---

## Objectif

Un document en échec d'extraction ne doit jamais bloquer le job `DOCUMENT_ANALYSIS` : le job atteint un
état terminal dès que tous les documents sont résolus (analysés **ou** extraction échouée).

---

## Comportement attendu

### Cas nominal

Dossier de `N` documents : `M` réussissent extraction + analyse (`DocumentAnalysis` status `DONE`),
`K` échouent l'extraction (`DocumentExtraction.extractionStatus = FAILED`), `M + K = N`.

- Le job `DOCUMENT_ANALYSIS` passe `DONE` dès que `count(DocumentAnalysis DONE) + count(extractions FAILED) >= totalItems`, **à condition que `count(DocumentAnalysis DONE) >= 1`**.
- `processedItems = min(count(DocumentAnalysis DONE) + count(extractions FAILED), totalItems)`.
- La synthèse dossier reste **déclenchée manuellement** par l'avocat (clic « Analyser le dossier »,
  cf. `CaseAnalysisMessage` / SF-185-04) — cette SF ne ré-introduit aucun auto-trigger.
- Deux points de ré-évaluation du job, idempotents :
  1. fin d'une analyse de document (`finalizeAnalysis` → `updateDocumentAnalysisJob`, existant) ;
  2. **nouveau** : réception d'un `ExtractionFailedEvent` (couvre le cas où le document en échec est
     le dernier à se résoudre — aucune `finalizeAnalysis` ne tourne après lui).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Tous les documents échouent l'extraction (`M = 0`) | Hors scope — pas de job `DOCUMENT_ANALYSIS` créé (aucune analyse préparée) ; affichage couvert par SF-121-03. La garde `done >= 1` empêche tout passage `DONE` parasite. | — |
| Le document en échec d'extraction est le dernier à se résoudre | Le listener `ExtractionFailedEvent` ré-évalue le job → `DONE`. | — |
| Échec d'**analyse** Anthropic (≠ extraction) | Inchangé — `markDocumentAnalysisJobFailed` marque le job `FAILED` (F-147). | — |
| Le job n'existe pas encore quand l'`ExtractionFailedEvent` arrive | Le listener ne fait rien (`findBy…ifPresent`) ; la complétion sera évaluée à la fin de l'analyse d'un autre document. | — |
| Job déjà `DONE` ou `FAILED` | Non ré-ouvert (garde de statut). | — |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** — non applicable : le mécanisme touche le pipeline d'analyse documentaire, pas un outil décisionnel.
- [x] **Autres pays** — non applicable : aucune logique pays.
- [x] **Autres domaines** — non applicable : aucune logique de domaine.
- [x] **Autres UI patterns** — non applicable : SF backend pure, aucun changement frontend.
- [x] **Autres flows transversaux** — scannés ci-dessous (autres job types).

### Autres sites de complétion de job scannés

| Site | Pattern `totalItems` | Vulnérable au même bug ? |
|------|----------------------|--------------------------|
| `DocumentAnalysisService.updateDocumentAnalysisJob` (DOCUMENT_ANALYSIS) | `totalItems = countByCaseFileId` (tous les documents) | **Oui — c'est le bug corrigé ici.** |
| `ChunkAnalysisService` (CHUNK_ANALYSIS) | `totalItems` incrémenté par chunk réellement émis | Non — un document non extractible ne produit aucun chunk, n'incrémente pas `totalItems`. |
| `CaseAnalysisService` / `EnrichedAnalysisService` / `AiQuestionService` | `totalItems = 1` (fixe) | Non — pas de comptage dérivé d'un volume de documents. |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature — seul `DOCUMENT_ANALYSIS` dérive
  `totalItems` d'un comptage de documents pouvant ne jamais produire de sous-résultat. Les autres job
  types ne sont pas affectés (justifié ci-dessus).

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : SF backend pure, aucun composant frontend décisionnel, aucune
  entrée `TOOL_REGISTRY`, aucun endpoint POST/GET décisionnel.

---

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — justification : SF backend pure, ne crée ni ne modifie aucun outil
  décisionnel à champs saisissables.

---

## Critères d'acceptation

- [ ] Job couvrant tous les documents par un mix `≥ 1` analyse `DONE` + `≥ 1` extraction `FAILED` → status `DONE`.
- [ ] Lorsque l'extraction en échec est le dernier événement du dossier, le listener `ExtractionFailedEvent` déclenche le passage `DONE`.
- [ ] `processedItems` clampé à `totalItems` — jamais supérieur (robustesse course concurrente).
- [ ] Job déjà `FAILED` (échec d'analyse, F-147) → **non** repassé `DONE` par l'arrivée d'une extraction `FAILED`.
- [ ] Aucune analyse `DONE` (`done == 0`) → job **non** repassé `DONE`.
- [ ] Non-régression F-147 : un échec d'analyse Anthropic marque toujours le job `FAILED`.
- [ ] Non-régression nominale : un dossier dont tous les documents s'analysent sans échec → job `DONE` comme avant.

---

## Périmètre

### Hors scope (explicite)

- Aucun changement frontend — F-121-04 affiche déjà le compteur « N non analysables / M ».
- Aucune migration DB.
- Amélioration de l'OCR des gros PDF scannés (`OCR_UNSUPPORTED_SIZE`) — relève de F-122, chantier séparé.
- Correction du bug Vision « media type `image/png` déclaré pour des octets JPEG » — bug indépendant, hors périmètre.
- Politique « échec d'analyse Anthropic → job `FAILED` » (F-147) — inchangée.
- Aucune ré-introduction d'auto-trigger de la synthèse dossier (supprimé par SF-185-04).

---

## Contraintes de validation

Aucun champ utilisateur. La SF modifie une logique de transition d'état de job interne. Sans objet.

---

## Technique

### Endpoint(s)

Aucun — aucun endpoint créé ni modifié.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `analysis_jobs` | UPDATE | Transition de statut `PROCESSING → DONE` du job `DOCUMENT_ANALYSIS` ; `processed_items` recalculé. |
| `document_extractions` | SELECT | Comptage des extractions `FAILED` par dossier. |

### Migration Liquibase

- [x] Non applicable.

### Composants impactés

- `DocumentAnalysisService.updateDocumentAnalysisJob` — la complétion compte aussi les extractions `FAILED` ; garde `done >= 1` ; garde de statut (`PROCESSING` uniquement).
- `DocumentAnalysisService` — nouveau handler `@TransactionalEventListener(phase = AFTER_COMMIT)` sur `ExtractionFailedEvent`, déléguant à une méthode `@Transactional` de ré-évaluation du job (réutilise la logique de `updateDocumentAnalysisJob`). `ExtractionService.extract` étant `@Transactional`, l'événement après-commit se déclenche bien et l'extraction `FAILED` est visible.
- `DocumentExtractionRepository` — ajout `long countByDocumentCaseFileIdAndExtractionStatus(UUID caseFileId, ExtractionStatus status)` (dérivé, cohérent avec `existsByDocumentCaseFileIdAndExtractionStatusIn` existant).

### Composants Angular

Aucun.

---

## Plan de test

### Tests unitaires (`DocumentAnalysisServiceTest`)

- [ ] `updateDocumentAnalysisJob` — `done = totalItems` (aucun échec) → job `DONE` (non-régression).
- [ ] `updateDocumentAnalysisJob` — `done + failedExtractions = totalItems`, `done >= 1` → job `DONE`.
- [ ] `updateDocumentAnalysisJob` — `done + failedExtractions < totalItems` → job reste `PROCESSING`.
- [ ] `updateDocumentAnalysisJob` — `done = 0`, `failedExtractions = totalItems` → job **non** `DONE`.
- [ ] `updateDocumentAnalysisJob` — `done + failedExtractions > totalItems` (course) → `processedItems` clampé à `totalItems`.
- [ ] `updateDocumentAnalysisJob` — job déjà `FAILED` → non repassé `DONE`.
- [ ] Handler `ExtractionFailedEvent` — job existant `PROCESSING`, complétion atteinte → job `DONE`.
- [ ] Handler `ExtractionFailedEvent` — aucun job pour le dossier → aucune exception, aucun effet.

### Tests d'intégration

- [ ] `DocumentAnalysisService` IT — dossier `N` documents dont 1 extraction `FAILED`, `N-1` analyses `DONE` : le job `DOCUMENT_ANALYSIS` atteint `DONE` sans dépendre du `ZombieJobResetScheduler`.

### Isolation workspace

- [x] Non applicable — la SF modifie une transition d'état interne au pipeline ; tous les accès restent indexés par `caseFileId` déjà résolu dans son workspace en amont. Aucun nouveau point d'accès cross-workspace.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — pas d'impact auth / Principal, workspace context, plans / limites, navigation / routing. La SF modifie une logique de transition d'état de job côté worker.

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — la SF ne touche ni l'auth, ni le workspace context, ni la navigation. Périmètre worker backend isolé. Validation par tests unitaires + IT backend.

---

## Dépendances

### Subfeatures bloquantes

- Aucune. S'appuie sur l'existant F-121-01/03/04 et F-147 (tous `done`).

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` impactée.

---

## Notes et décisions

- **Job `DONE` et non `FAILED` sur échec partiel** : avec `≥ 1` document analysé, la phase documentaire
  est bien terminée — l'avocat doit pouvoir lancer la synthèse sur les documents exploitables. Le
  compteur « N non analysables / M » (SF-121-04) porte déjà la nuance côté UI. Marquer le job `FAILED`
  rebloquerait le parcours.
- **`@TransactionalEventListener(AFTER_COMMIT)`** retenu (et non `@EventListener` simple) :
  `ExtractionService.extract` est `@Transactional` ; l'after-commit garantit que l'extraction `FAILED`
  est committée et visible par la transaction de ré-évaluation du job.
- **Idempotence** : les deux points de ré-évaluation (`finalizeAnalysis` et listener) appliquent la même
  garde de statut (`PROCESSING` uniquement) — exécutions multiples sans effet de bord.
