# SF-159-04 — Instrumentation détection job FAILED inattendu

## Objectif

Logger côté frontend toute transition d'un `AnalysisJob` vers `status === 'FAILED'`, avec contexte complet, pour diagnostiquer le bug intermittent observé sur Chen 13 (2026-05-05) : la pipeline tile affichait `SYNTHÈSE DU DOSSIER` en rouge alors que côté backend l'analyse était en cours puis DONE.

## Contexte

Bug observé staging 2026-05-05 sur le dossier Immigration Chen 13 : la barre `SYNTHÈSE DU DOSSIER 0/1` apparue en rouge (FAILED) pendant que le backend était PROCESSING. Refresh navigateur a résolu. Backend logs confirment `Case analysis DONE` ~53s après le screenshot, sans aucun FAILED transitoire côté serveur.

Hypothèses :
- L'API `GET /analysis-jobs` a renvoyé un `CASE_ANALYSIS FAILED` pour une raison transitoire (race DB ?)
- Un état signal Angular stale a été affiché brièvement
- Ordre d'arrivée d'events SSE incohérent

Sans repro fiable et sans log, on ne peut pas trancher. Cette SF ajoute de l'observabilité minimale.

## Comportement nominal

1. À chaque appel de `loadAnalysisJobs()` (`case-file-detail.component.ts`), comparer les `status` des jobs reçus aux statuts précédemment observés (mémoire locale au composant : `Map<jobType, lastStatus>`).
2. Si un job apparaît en `FAILED` alors que le précédent statut connu n'était pas `FAILED` (transition `PROCESSING → FAILED` ou `PENDING → FAILED` ou première observation `FAILED`), écrire un `console.warn` enrichi :
   - `caseFileId`, `jobType`, `previousStatus`, `newJob` (objet entier), `allJobs` (réponse complète), `timestamp`, `source: 'loadAnalysisJobs' | 'pollingTick'`.
3. Idempotent : une seule entrée par transition (les polls suivants qui re-renvoient `FAILED` ne re-loguent pas tant que la dernière transition observée est déjà `FAILED`).
4. Aucune remontée Sentry V1 (éviter le bruit). Si confirmé récurrent, on étendra en V2.

## Cas d'erreur / edge cases

- Premier load (aucun statut précédent) + job déjà FAILED → log (cas légitime "vraie panne", mais utile pour confirmer).
- Reset de la map à chaque montage du composant (pas de persistance cross-page).

## Critères d'acceptation

- [ ] Map `previousJobStatuses: Map<jobType, status>` privée dans `case-file-detail.component.ts`.
- [ ] Méthode privée `detectAndLogFailedTransition(jobs: AnalysisJob[], source: string): void` qui compare l'état précédent à `jobs` et `console.warn` toute transition vers `FAILED`.
- [ ] Appelée dans `loadAnalysisJobs.next` ET dans le polling tick (`managePolling.next`).
- [ ] Map réinitialisée à `ngOnInit` (pas de persistance cross-instance).
- [ ] 2 tests Jest : (a) transition PROCESSING → FAILED loggée 1× ; (b) re-réception FAILED idempotente (pas de double log).

## Plan de test minimal

- **Frontend Jest** : 2 tests dans `case-file-detail.component.spec.ts` :
  - U1 : `loadAnalysisJobs` reçoit `[CASE_ANALYSIS PROCESSING]` puis `[CASE_ANALYSIS FAILED]` → `console.warn` appelé 1×.
  - U2 : 2 réponses successives `[CASE_ANALYSIS FAILED]` → `console.warn` appelé 1× seulement.
- **Manuel staging** : si le bug réapparaît, ouvrir la console DevTools et copier le warning enrichi.

## Tables / endpoints / composants impactés

- **Frontend** : `frontend/src/app/case-files/case-file-detail/case-file-detail.component.ts` + `.spec.ts` uniquement.
- **Backend** : aucun.
- **Aucune migration**.

## Hors périmètre

- Remontée Sentry (à activer en V2 si bug confirmé récurrent).
- Fix proactif du bug (cette SF est purement diagnostique).
- Élargissement à d'autres composants (`synthesis.component`, etc.).

## Analyse de cohérence transversale

- **Préoccupations transversales** :
  - Auth / Principal : aucun changement.
  - Workspace context : aucun changement.
  - Plans / limites : aucun changement.
  - Routing / guards : aucun changement.
- **Nouveau pattern UI / service partagé** : aucun — instrumentation locale au seul composant `case-file-detail`. Si un pattern de log défensif émerge sur d'autres composants, on extraira un helper partagé (pas la peine pour 1 occurrence).
- **Impact par domaine métier** : transversal — infra de diagnostic, aucune adaptation par domaine ni pays.
