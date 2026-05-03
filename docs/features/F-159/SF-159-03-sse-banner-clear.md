# Mini-spec — F-159 / SF-159-03 — Bannière "Analyse en cours" qui ne se clear pas après fin d'analyse

## Identifiant

`F-159 / SF-159-03`

## Feature parente

`F-159` — Suivi visuel des analyses asynchrones (bannière + flash + toast post-analyse)

## Statut

`in-progress`

## Date de création

2026-05-03

## Branche Git

`feat/SF-159-03-sse-banner-clear`

---

## Objectif

Faire disparaître la bannière "Analyse des documents en cours…" (et "Analyse du dossier en cours…") **automatiquement** dès que l'analyse correspondante est terminée — sans nécessiter un refresh manuel de la page.

---

## Comportement attendu

### Cas nominal

**Avant** (bug observé en staging 2026-05-03) :
1. Avocat upload des documents sur un dossier qui a déjà eu une `CaseAnalysis` `DONE` antérieure
2. Bannière "Analyse des documents en cours…" apparaît (correct)
3. Backend complète l'analyse documents → publie `AnalysisStatusEvent(caseFileId, DONE, DOCUMENT_ANALYSIS)`
4. **La bannière reste visible indéfiniment** ; seul un refresh navigateur la fait disparaître

**Après** :
1. Mêmes étapes 1-3
2. La bannière disparaît dans les 3-5 secondes après la fin réelle de l'analyse (au pire un cycle de polling après la fin), sans interaction utilisateur

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| SSE stream coupé (réseau / proxy) | Le polling 3 s du frontend détecte la fin et clear la bannière |
| Analyse échoue (status FAILED) | La bannière disparaît identiquement (job sort de `PENDING/PROCESSING`) |
| Plusieurs jobs en parallèle (DOCUMENT_ANALYSIS + ENRICHED_ANALYSIS) | La bannière reste visible tant qu'au moins un job tracké est actif, disparaît quand le dernier finit |

---

## Cause racine

### Côté backend (`AnalysisStatusStreamController.java:65-79`)

Le contrôleur SSE court-circuite l'enregistrement de l'emitter **dès qu'une `CaseAnalysis` avec status `DONE` existe** pour le dossier :

```java
var done = caseAnalysisRepository
    .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(id, AnalysisStatus.DONE)
    .orElse(null);
if (done != null) {
    emitter.send(SseEmitter.event().name("ANALYSIS_DONE").data("..."));
    emitter.complete();
    return emitter;   // ← jamais enregistré
}
```

L'event `ANALYSIS_DONE` envoyé n'est **pas** dans la liste des `eventNames` écoutés côté frontend (`analysis-sse.service.ts:25-31` n'écoute que `CASE_ANALYSIS_*`, `DOCUMENT_ANALYSIS_*`, `ENRICHED_ANALYSIS_*`, `QUESTION_GENERATION_*`). Cette branche est donc à la fois cassée pour les nouveaux cycles d'analyse **et** silencieuse pour le frontend.

### Côté frontend (`case-file-detail.component.ts:557-560`)

`loadAnalysisJobs()` n'appelle `progressService.syncFromJobs(jobs)` qu'une seule fois (gardé par `!this.progressSynced`). Le polling 3 s rafraîchit `analysisJobs` mais ne touche jamais à l'état du `DecisionalToolsProgressService`. Sans réception SSE, aucun mécanisme alternatif ne clear la bannière.

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — non applicable, le bug est dans le mécanisme SSE/polling de notification, pas dans les outils décisionnels
- [x] **Autres pays** — non applicable (mécanisme transversal)
- [x] **Autres domaines** — non applicable (mécanisme transversal)
- [x] **Autres UI patterns** — non applicable (touche uniquement la bannière `decisional-tools-progress-banner`)
- [x] **Autres flows transversaux** — l'event SSE est consommé par `GlobalAnalysisNotificationService` qui sert :
  - `synthesis.component.ts:258, 694` → toast "Synthèse terminée"
  - `decisional-tools-progress.service.ts:54` → bannière (cible du fix)
  - Les autres consommateurs ne sont pas affectés par le bug (ils utilisent les events s'ils arrivent ; ils ne se basent pas sur l'absence de réception)

### Cas spécifique : nouveau pattern UI ou service partagé

Pas de nouveau composant partagé. Le fix touche :
- 1 contrôleur backend (`AnalysisStatusStreamController`)
- 1 service frontend (`DecisionalToolsProgressService`)
- 1 garde dans `case-file-detail.component.ts`

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Bannière progress F-159 | Oui | Fix dans cette SF |
| Toast synthèse F-159 | Non | Pas affecté — toast déclenché à la réception, pas à la non-réception |
| Flash + toast diff SF-159-02 | Non | Pas affecté — déclenché par diff snapshot, pas par event SSE |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature

---

## Critères d'acceptation

### Backend

- [x] **AC1** : Si **aucun** job (`AnalysisJob`) du dossier n'est en `PENDING` ou `PROCESSING` au moment de l'ouverture du stream → comportement court-circuit conservé (préserve la sémantique "analyse déjà faite, rien à attendre")
- [x] **AC2** : Si **au moins un** job (`AnalysisJob`) du dossier est en `PENDING` ou `PROCESSING` au moment de l'ouverture du stream → l'emitter est **enregistré** (pas de court-circuit), même si une `CaseAnalysis` `DONE` antérieure existe
- [x] **AC3** : Quand le job actif termine, l'event `<JOB_TYPE>_<STATUS>` (ex `DOCUMENT_ANALYSIS_DONE`) est bien envoyé sur l'emitter enregistré
- [x] **AC4** : Test d'intégration `AnalysisStatusStreamControllerIT` couvre les scénarios "DONE existe + nouveau job PROCESSING/PENDING → emitter enregistré, pas de short-circuit" et "DONE existe + jobs DONE/FAILED → court-circuit conservé"
- [x] **AC5** : Isolation workspace conservée (le contrôle existant ligne 58-62 reste en place et testé)

### Frontend

- [x] **AC6** : `DecisionalToolsProgressService.initFromJobs(jobs)` (nouvelle méthode) fait l'écrasement initial autoritaire au montage de la page (réapparition correcte de la bannière sur reload pendant analyse)
- [x] **AC7** : `DecisionalToolsProgressService.syncFromJobs(jobs)` ne **retire** que les job types absents des jobs PENDING/PROCESSING. N'**ajoute** rien (le seul moyen d'ajouter reste `start()` ou `initFromJobs`)
- [x] **AC8** : Dans `case-file-detail.component.ts`, le 1er `loadAnalysisJobs` appelle `initFromJobs` (gardé par `!progressSynced`) et le polling appelle `syncFromJobs` à chaque tour
- [x] **AC9** : Test unitaire `decisional-tools-progress.service.spec.ts` couvre :
  - `start('CASE_ANALYSIS')` puis `syncFromJobs([{jobType:'CASE_ANALYSIS', status:'PROCESSING'}])` → active = `{CASE_ANALYSIS}` (inchangé)
  - `start('CASE_ANALYSIS')` puis `syncFromJobs([{jobType:'CASE_ANALYSIS', status:'DONE'}])` → active = `{}` (retiré par sync)
  - `syncFromJobs([{jobType:'DOCUMENT_ANALYSIS', status:'PROCESSING'}])` sans `start()` préalable → active = `{}` (sync n'ajoute pas)
  - `initFromJobs([{jobType:'CASE_ANALYSIS', status:'PROCESSING'}])` → active = `{CASE_ANALYSIS}` (set replace autoritaire)

---

## Périmètre

### Hors scope (explicite)

- Refonte du protocole SSE (toujours basé sur `EventSource` + Spring `SseEmitter`)
- Renommage de l'event "ANALYSIS_DONE" générique (resté tel quel pour éviter un changement transversal hors périmètre du bug)
- Réécriture de `decisional-tools-progress.service` (pas de migration vers signal computed à partir de `analysisJobs` — changement architectural trop large pour une SF de fix)

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/case-files/{id}/analysis-status/stream` | Oui | MEMBER |

(endpoint existant, modification de comportement uniquement)

### Tables impactées

Aucune. Lecture seule sur `analysis_jobs` (déjà utilisée).

### Migration Liquibase

- [x] Non applicable

### Composants Angular impactés

- `DecisionalToolsProgressService` — ajout de `initFromJobs`, modification de `syncFromJobs` pour ne faire que retirer
- `CaseFileDetailComponent` — appel `initFromJobs` au 1er sync, `syncFromJobs` dans le polling

---

## Plan de test

### Tests unitaires (frontend)

- [x] `DecisionalToolsProgressService.syncFromJobs` — start manuel + sync avec jobs PROCESSING → active inchangée
- [x] `DecisionalToolsProgressService.syncFromJobs` — start manuel + sync avec jobs DONE → active vidée
- [x] `DecisionalToolsProgressService.syncFromJobs` — sync seul (sans start) avec jobs PROCESSING → active reste vide
- [x] `DecisionalToolsProgressService.initFromJobs` — replace autoritaire avec jobs PROCESSING → active populée

### Tests d'intégration (backend)

- [x] `AnalysisStatusStreamControllerIT.stream_doneAnalysisWithActiveJob_doesNotShortCircuit` — INSERT analysis DONE + INSERT job PROCESSING → GET stream → asyncStarted (registered)
- [x] `AnalysisStatusStreamControllerIT.stream_doneAnalysisWithPendingJob_doesNotShortCircuit` — INSERT analysis DONE + job PENDING → asyncStarted
- [x] `AnalysisStatusStreamControllerIT.stream_doneAnalysisAllJobsTerminal_emitsAnalysisDoneEventImmediately` — INSERT analysis DONE + jobs DONE → court-circuit conservé

### Isolation workspace

- [x] Applicable — test existant dans `AnalysisStatusStreamControllerIT` conservé (workspace différent → emitter complete sans registration)

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale** — fix isolé sur le mécanisme SSE/polling de la bannière

### Smoke tests E2E concernés

- [x] Aucun smoke test E2E ne couvre la bannière (justification : la bannière est un détail UI temporel sans impact fonctionnel sur la navigation, l'auth ou le workspace ; les tests Jest unitaires + IT backend couvrent le bug ciblé)

---

## Dépendances

### Subfeatures bloquantes

- `SF-159-01` — done
- `SF-159-02` — done

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

### Pourquoi backend + frontend dans la même SF

Le bug a une cause racine côté backend (court-circuit erroné). Le fix frontend est une **défense en profondeur** : si le SSE échoue pour une autre raison (proxy, timeout, race), le polling reste un mécanisme fiable pour clear la bannière.

Les deux fixes sont indépendants l'un de l'autre mais résolvent le même symptôme. Les séparer en 2 SF parallèles aurait été acceptable (contrats clairs), mais la SF reste de petite taille (< 1 jour) et la cohésion fonctionnelle est forte → 1 SF.

### Pourquoi `initFromJobs` séparé de `syncFromJobs`

L'écrasement de l'active set est dangereux dans le polling continu : entre l'instant où l'avocat clique "Re-analyser" (`start('CASE_ANALYSIS')` qui ajoute immédiatement à l'active set) et la création effective de la ligne `analysis_jobs` côté backend, un sync écraseur retirerait CASE_ANALYSIS et la bannière disparaîtrait prématurément.

Solution : 2 méthodes distinctes :
- `initFromJobs` — écrasement autoritaire, **une seule fois** au montage (pour réapparition correcte sur reload pendant analyse)
- `syncFromJobs` — "remove only", appelé à chaque polling 3 s (sécurise le clear si SSE rate)

### Pourquoi ne pas écouter "ANALYSIS_DONE" côté frontend

Tentation : ajouter `'ANALYSIS_DONE'` à la liste des `eventNames` dans `analysis-sse.service.ts`. Rejeté car :
- L'event ne porte pas le `jobType` (data ne contient que `caseFileId` + `status`)
- Sans `jobType`, le frontend ne peut pas savoir quel job retirer du progress service
- C'est un signal "globalement fini" qui n'est plus pertinent pour la sémantique multi-jobs introduite par F-159

La vraie réponse est de ne pas court-circuiter quand un job actif existe.
