# Mini-spec — F-39 / SF-39-02 Notification globale d'analyse

## Identifiant

`F-39 / SF-39-02`

## Feature parente

`F-39` — Notifications temps réel

## Statut

`ready`

## Date de création

2026-03-25

## Branche Git

`feat/SF-39-02-global-analysis-notification`

---

## Objectif

Afficher un toast de notification visible depuis n'importe quelle page de l'application quand le pipeline d'analyse documents se termine (après upload) ou quand l'analyse dossier se termine (après "Analyser le dossier").

---

## Comportement attendu

### Cas nominal — pipeline document (après upload)

1. L'utilisateur clique "Uploader les documents (N)".
2. L'upload réussit → le composant appelle `GlobalAnalysisNotificationService.startDocumentStream(caseFileId)`.
3. L'utilisateur peut naviguer librement.
4. Quand le job `DOCUMENT_ANALYSIS` passe à DONE côté backend, un événement SSE est émis.
5. Toast affiché : `"Documents analysés, vous pouvez lancer l'analyse du dossier"` (snack-success, 5s).

### Cas nominal — case analysis (après "Analyser le dossier")

1. L'utilisateur clique "Analyser le dossier".
2. Le composant appelle `GlobalAnalysisNotificationService.startCaseStream(caseFileId)`.
3. L'utilisateur peut naviguer librement.
4. Quand `CaseAnalysis` passe à DONE, un événement SSE est émis.
5. Toast affiché : `"Analyse du dossier terminée"` (snack-success, 4s).
6. Si l'utilisateur est toujours sur `case-file-detail` : l'UI se rafraîchit automatiquement (`loadAnalysisJobs` + `loadSynthesis`).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `DOCUMENT_ANALYSIS` FAILED | Toast `"L'analyse des documents a échoué"` (snack-error, 5s) |
| `CASE_ANALYSIS` FAILED | Toast `"L'analyse du dossier a échoué"` (snack-error, 5s) |
| SSE timeout (180s) | Fermeture silencieuse, pas de toast, polling continue |
| Upload partiel (certains fichiers échouent) | Comportement inchangé — géré par le composant upload existant |

---

## Critères d'acceptation

- [ ] Toast `"Documents analysés, vous pouvez lancer l'analyse du dossier"` visible depuis `/case-files` après upload
- [ ] Toast `"Analyse du dossier terminée"` visible depuis `/case-files` après case analysis
- [ ] Les deux toasts visibles depuis n'importe quelle page Angular (navigation libre)
- [ ] Si l'utilisateur est sur `case-file-detail` : UI rafraîchie automatiquement à la réception du CASE_ANALYSIS event
- [ ] Si SSE timeout : pas de toast d'erreur intempestif
- [ ] Aucune régression sur les tests existants (229 backend + frontend)

---

## Périmètre

### Hors scope

- Centre de notifications persistant
- Browser push notifications (Notification API)
- Affichage du nom du dossier dans le toast
- Notification pour les autres étapes (CHUNK_ANALYSIS, QUESTION_GENERATION)

---

## Technique

### Backend — nouveau : publication event DOCUMENT_ANALYSIS DONE

Dans `DocumentAnalysisService.updateDocumentAnalysisJob()`, quand le job passe à DONE :
- Publier `AnalysisStatusEvent` avec un nouveau statut `DOCUMENT_ANALYSIS_DONE`

Ou alternative : enrichir `AnalysisStatusEvent` avec un champ `jobType` pour distinguer les deux types.

**Décision retenue :** ajouter `jobType` à `AnalysisStatusEvent` → `AnalysisStatusEvent(UUID caseFileId, AnalysisStatus status, JobType jobType)`.

`SseNotificationService` envoie l'event name en fonction du jobType :
- `CASE_ANALYSIS_DONE` / `CASE_ANALYSIS_FAILED`
- `DOCUMENT_ANALYSIS_DONE` / `DOCUMENT_ANALYSIS_FAILED`

### Backend — publication depuis DocumentAnalysisService

`DocumentAnalysisService.finalizeAnalysis()` appelle déjà `updateDocumentAnalysisJob()`. Quand le job passe à DONE dans `updateDocumentAnalysisJob()`, publier l'event via `ApplicationEventPublisher` avec `afterCommit` (comme CaseAnalysisService).

Mais `DocumentAnalysisService` n'a pas `ApplicationEventPublisher`. Il faut l'injecter.

### Frontend — GlobalAnalysisNotificationService

```typescript
@Injectable({ providedIn: 'root' })
export class GlobalAnalysisNotificationService {
  startDocumentStream(caseFileId: string): void
  startCaseStream(caseFileId: string): void
}
```

- Utilise `AnalysisSseService` (existant) pour ouvrir le stream
- Affiche le toast via `MatSnackBar`
- Expose un `Observable<void>` `caseAnalysisDone$` pour que `case-file-detail` puisse déclencher le refresh UI

### Frontend — case-file-detail.component

- Remplace `startSseStream()` par `globalAnalysisNotificationService.startCaseStream(caseFileId)`
- S'abonne à `caseAnalysisDone$` pour déclencher `loadAnalysisJobs` + `loadSynthesis`
- Après upload réussi : appelle `globalAnalysisNotificationService.startDocumentStream(caseFileId)`
- Supprime `sseSub`, `startSseStream()`, et les toasts SSE locaux

### AnalysisSseService (Angular)

Écoute les event names `CASE_ANALYSIS_DONE`, `CASE_ANALYSIS_FAILED`, `DOCUMENT_ANALYSIS_DONE`, `DOCUMENT_ANALYSIS_FAILED` (en plus de l'existant `ANALYSIS_DONE` / `ANALYSIS_FAILED` pour rétrocompatibilité).

---

### Endpoints

Aucun nouvel endpoint. L'endpoint SSE existant est réutilisé :
`GET /api/v1/case-files/{id}/analysis-status/stream`

### Tables impactées

Aucune.

### Migration Liquibase

Non applicable.

### Composants Angular impactés

| Composant | Modification |
|-----------|-------------|
| `GlobalAnalysisNotificationService` (nouveau) | Gère les 2 streams SSE + toasts |
| `case-file-detail.component.ts` | Délègue SSE au service global, s'abonne à `caseAnalysisDone$` |
| `analysis-sse.service.ts` | Ajoute les nouveaux event names |

### Classes backend impactées

| Classe | Modification |
|--------|-------------|
| `AnalysisStatusEvent` | Ajout champ `jobType` |
| `SseNotificationService` | Event name selon `jobType` |
| `DocumentAnalysisService` | Injection `ApplicationEventPublisher`, publication event afterCommit quand DOCUMENT_ANALYSIS DONE |
| `CaseAnalysisService` | Mise à jour appel `AnalysisStatusEvent` avec `jobType` |
| `EnrichedAnalysisService` | Mise à jour appel `AnalysisStatusEvent` avec `jobType` |

---

## Plan de test

### Tests unitaires backend

- [ ] `SseNotificationServiceTest` — event `DOCUMENT_ANALYSIS_DONE` → event name correct
- [ ] `SseNotificationServiceTest` — event `CASE_ANALYSIS_DONE` → event name correct
- [ ] `DocumentAnalysisServiceTest` — job DONE → `AnalysisStatusEvent` publié afterCommit
- [ ] `CaseAnalysisServiceTest` — event publié avec `jobType = CASE_ANALYSIS`

### Tests unitaires frontend

- [ ] `GlobalAnalysisNotificationService` — `startCaseStream` DONE → toast `"Analyse du dossier terminée"`
- [ ] `GlobalAnalysisNotificationService` — `startDocumentStream` DONE → toast `"Documents analysés..."`
- [ ] `GlobalAnalysisNotificationService` — FAILED → toast error
- [ ] `case-file-detail.component` — après upload réussi → `startDocumentStream` appelé
- [ ] `case-file-detail.component` — `caseAnalysisDone$` → `loadAnalysisJobs` + `loadSynthesis` appelés

### Isolation workspace

Non applicable — l'endpoint SSE valide déjà le workspace.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — non
- [ ] Workspace context — non
- [ ] Plans / limites — non
- [ ] Navigation / routing frontend — non
- [x] **Aucune préoccupation transversale** — extension d'un service existant, impact limité

### Smoke tests E2E

- [ ] Aucun smoke test concerné (SSE non couvert par les smoke tests existants)

---

## Dépendances

### Subfeatures bloquantes

- SF-39-01 — statut : done
- SF-52-01 — statut : done (bouton "Uploader les documents")

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- `AnalysisStatusEvent` étendu avec `jobType` plutôt que créer deux event classes distinctes → moins de surface à modifier
- `GlobalAnalysisNotificationService.caseAnalysisDone$` est un `Subject<void>` — permet à `case-file-detail` de s'abonner sans couplage fort
- Le polling (3s) reste en place comme fallback si SSE timeout
