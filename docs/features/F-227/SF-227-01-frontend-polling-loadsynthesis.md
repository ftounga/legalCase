# Mini-spec — F-227 / SF-227-01 Frontend — Polling tick recharge la synthèse à la transition CASE_ANALYSIS DONE

## Identifiant

`F-227 / SF-227-01`

## Statut

`draft` — 2026-05-07

## Branche Git

`feat/SF-227-01-frontend-polling-loadsynthesis`

## Pattern de référence

Aucun pattern à dupliquer — fix ciblé sur `case-file-detail.component.ts`.

---

## Objectif

Quand l'avocat navigue `/case-files/:id` → `/synthesis` → `/case-files/:id` pendant une analyse, le SSE peut rater l'événement `DONE` (re-tracking côté `SseNotificationService` ne replay pas les événements). Le polling tick (toutes les 3s) détecte la transition `CASE_ANALYSIS PROCESSING → DONE` mais n'appelle pas `loadSynthesis()` — résultat : la synthèse reste figée en UI jusqu'à un refresh manuel. Ce fix corrige cette omission.

---

## Comportement attendu

### Avant fix (bug)

1. Avocat sur `/case-files/:id` → `loadAnalysisJobs()` détecte CASE_ANALYSIS DONE → appelle `loadSynthesis()` ✅
2. Avocat clique "Voir la synthèse en cours…" → navigation `/synthesis` → `SseNotificationService.track()` re-souscrit (le précédent listener détail est orphelin)
3. Avocat revient `/case-files/:id` → `track()` re-souscrit → SSE reconnecté
4. Si l'analyse a fini DONE pendant la navigation, l'événement DONE n'est pas reçu (perdu lors du re-track)
5. `loadAnalysisJobs()` au mount voit **encore PROCESSING** (race) → polling démarre
6. Polling détecte `CASE_ANALYSIS DONE` au tick suivant → **MAIS n'appelle PAS `loadSynthesis()`** → synthèse reste à `null` ou à l'ancienne version
7. Avocat doit refresh manuellement

### Après fix

1-5 : identique
6. Polling détecte `CASE_ANALYSIS DONE` au tick suivant → **appelle `loadSynthesis()`** → synthèse rafraîchie automatiquement
7. UI se met à jour sans intervention

`loadSynthesis()` a déjà sa propre déduplication via `lastCompletedSynthesisVersion` — pas de risque de spam.

---

## Critères d'acceptation

- [ ] **CA-01** : dans `case-file-detail.component.ts:670-718` (polling tick), si `caseAnalysisDone === true`, appeler `this.loadSynthesis(caseFileId)` (idempotent grâce à `lastCompletedSynthesisVersion`)
- [ ] **CA-02** : symétrie avec ENRICHED_ANALYSIS — détecter aussi le DONE pour ce job_type et appeler `loadSynthesis()`
- [ ] **CA-03** : test Jest `case-file-detail.component.spec.ts` — simuler polling tick avec jobs `CASE_ANALYSIS DONE` après précédent tick `PROCESSING` → `loadSynthesis` appelé
- [ ] **CA-04** : test Jest — simuler polling tick avec jobs `CASE_ANALYSIS DONE` répété (déjà chargé) → `loadSynthesis` peut être appelé mais `dashboardRefreshService.triggerRefresh` n'est PAS appelé deux fois (déduplication via `lastCompletedSynthesisVersion`)
- [ ] **CA-05** : test Jest — symétrie ENRICHED_ANALYSIS DONE → `loadSynthesis` appelé
- [ ] **CA-06** : aucune régression sur les tests existants (le polling continue à stop quand `!stillRunning && !waitingForQuestions && !stillVision`)
- [ ] **CA-07** : ajouter un commentaire explicite dans le polling tick justifiant le fix : `// F-227 — re-load synthesis on PROCESSING→DONE transition (fallback si SSE DONE event missed during navigation)`

---

## Périmètre

### Hors scope V1

- (a) Refonte de `SseNotificationService` pour replayer les événements manqués — overkill, le polling fait office de fallback
- (b) Polling plus rapide (2s, 1s) — dégrade UX inutilement, 3s est OK
- (c) WebSocket bidirectionnel — V2 si besoin

---

## Technique

### Fichiers à modifier

1. `frontend/src/app/case-files/case-file-detail/case-file-detail.component.ts` :
   - Lignes ~705-716 : dans le bloc polling tick, après `const caseAnalysisDone = ...`, ajouter `const enrichedAnalysisDone = updated.some(j => j.jobType === 'ENRICHED_ANALYSIS' && j.status === 'DONE');`
   - Ajouter `if (caseAnalysisDone || enrichedAnalysisDone) { this.loadSynthesis(caseFileId); }` AVANT le `if (!stillRunning && ...) { this.stopPolling(); }`
   - Ajouter commentaire `// F-227 — fallback si SSE DONE event missed during navigation`

2. `frontend/src/app/case-files/case-file-detail/case-file-detail.component.spec.ts` :
   - Ajouter 3 tests CA-03, CA-04, CA-05

---

## Plan de test

### Tests unitaires Jest (~3)

- `case-file-detail.component.spec.ts` :
  - T-CA-03 : tick polling N+1 avec `CASE_ANALYSIS DONE` (vs N PROCESSING) → `loadSynthesis` appelé
  - T-CA-04 : tick polling N+2 avec mêmes jobs DONE → `loadSynthesis` peut être appelé MAIS `dashboardRefreshService.triggerRefresh` non re-appelé
  - T-CA-05 : tick polling N+1 avec `ENRICHED_ANALYSIS DONE` → `loadSynthesis` appelé

### Test manuel post-merge staging

1. Lancer une analyse longue sur un dossier
2. Naviguer vers `/synthesis` mid-stream
3. Attendre la fin de l'analyse (sur l'écran synthèse, voir DONE)
4. Revenir à `/case-files/:id`
5. **Sans refresh navigateur** → la section synthèse doit afficher le résultat dans les 3-6s (1-2 ticks de polling)
6. Vérifier les logs console : pas de 404 nouveau, pas d'erreur

---

## Dépendances

- F-185 SF-185-01 ✅ (streaming + partial)
- F-190 SF-190-03 ✅ (polling tick existant)

---

## Impact par domaine métier

Transversal — flux UI navigation, aucune adaptation par domaine.

---

## Analyse de cohérence transversale

- **Auth/Principal** : non concerné — frontend.
- **Workspace context** : non concerné.
- **Plans/limites** : non concerné.
- **Navigation/routing** : ✅ concerné mais pas de modification de route — juste meilleure gestion du re-mount.
- **Outil décisionnel métier** : non concerné — flux synthèse.
- **Pattern partagé** : aucun nouveau pattern. Patch local sur le polling tick.

---

## Risques

- **Spam de loadSynthesis** : mitigé par déduplication interne (`lastCompletedSynthesisVersion` signal — l'appel re-fetch mais ne déclenche pas `triggerRefresh` ni `loadQuestions` redondant)
- **Régression tests existants** : faible — ajout d'un appel idempotent

---

## Notes

- **Décision 2026-05-07** : V1 = patch ciblé du polling tick (3 lignes + 3 tests). Pas de refonte du SseNotificationService — overkill et risqué.
- **Décision 2026-05-07** : couvrir CASE_ANALYSIS et ENRICHED_ANALYSIS — mêmes branches que le handler SSE existant pour cohérence.
- **Origine** : bug récurrent rapporté staging 2026-05-07 sur Immigration Chen 16 — "ce bug arrive de plus en plus".
