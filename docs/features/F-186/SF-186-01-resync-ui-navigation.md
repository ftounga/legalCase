# SF-186-01 — Resync UI après navigation / focus + reconnexion SSE robuste

## Objectif (1 phrase)
Corriger le bug "barres de progression rouges après navigation entre /case-files/:id et /case-files/:id/synthesis" observé staging 2026-05-03 sur Chen 8 : la cause racine est que `AnalysisSseService.stream()` appelle `observer.complete()` sur la moindre erreur réseau, ce qui tue le stream SSE pour de bon → pas d'event `CASE_ANALYSIS_DONE` reçu → l'UI reste figée dans un état incorrect que seul un hard refresh résout.

## Diagnostic

### Symptôme observé (utilisateur, staging Chen 8 2026-05-03)

1. Avocat clique "Analyser le dossier"
2. Avocat clique "Voir la synthèse (en cours…)" → bascule vers `/synthesis`
3. La synthèse partielle s'affiche correctement (fix SF-185-07 fonctionne — 30 sections détectées sur 465 chunks Sonnet)
4. Avocat revient sur la page dossier `/case-files/:id`
5. Les barres de progression sont d'abord en `PROCESSING` (correct)
6. Puis "plus rien ne bouge"
7. Finalement les barres deviennent **rouges** (état FAILED) alors que côté backend l'analyse est `DONE` (logs `Case analysis DONE … 8150/15961 tokens`, email envoyé, 8 questions générées)
8. **Hard refresh (Ctrl+Shift+R) résout immédiatement** : refetch des `analysis_jobs` depuis la DB → état correct `DONE`

Reproduction : trivial — il suffit de naviguer entre /detail et /synthesis quelques fois pendant qu'une analyse tourne.

### Cause racine (cible du fix)

`frontend/src/app/core/services/analysis-sse.service.ts:38-41` :

```typescript
source.onerror = () => {
  source.close();
  observer.complete();
};
```

`EventSource` est conçu pour se reconnecter automatiquement (le navigateur retente toutes les ~3s par défaut). Notre wrapper appelle `source.close()` + `observer.complete()` à la moindre erreur (déconnexion réseau temporaire, restart pod backend lors d'un déploiement, blip Cloud, retour de tab après mise en veille, etc.) — ça tue le stream **définitivement**.

Conséquence : tous les events SSE suivants (`CASE_ANALYSIS_DONE`, `QUESTION_GENERATION_DONE`, etc.) ne sont jamais reçus par le frontend. La page dossier ne sait pas que l'analyse a fini. Le polling `managePolling` à 3s peut compenser MAIS s'arrête dès que la condition `hasPendingOrProcessing` devient fausse (et reste arrêté), et il dépend du fait que le composant soit monté à ce moment-là.

### Cause secondaire (cible du fix)

Quand l'utilisateur revient sur la page dossier après navigation, `case-file-detail.component.ts:ngOnInit` est bien refire (les routes /detail et /synthesis sont sœurs, pas parent/enfant), donc `loadAnalysisJobs(id)` est appelé. **Mais** :
- Si la page reste ouverte et que l'utilisateur change de tab navigateur → revient → aucune action côté frontend, l'état peut rester stale si le SSE est mort.
- Le timing d'arrivée des events SSE peut créer une fenêtre où le state local affiche `PROCESSING` puis n'est plus jamais updaté (SSE mort).

## Approche du fix

### Fix 1 (cible principale) — Reconnexion SSE robuste

Modifier `AnalysisSseService.stream()` pour :
1. **Ne pas appeler `source.close()`** sur `onerror` quand `readyState` ne vaut pas `EventSource.CLOSED` (2). Laisser le navigateur retenter automatiquement.
2. **Ne pas appeler `observer.complete()`** sauf si l'erreur est terminale (`readyState === CLOSED`).
3. Sur erreur fatale (404, 403, fermeture explicite serveur), fermer proprement et `complete()`.

```typescript
source.onerror = () => {
  // EventSource auto-reconnect : si readyState=CONNECTING (0), le navigateur
  // retente automatiquement (~3s). Ne pas close() ni complete() — sinon on tue
  // le stream définitivement et l'UI rate les events suivants (DONE, FAILED).
  // SF-186-01 — bug observé staging 2026-05-03 sur Chen 8 : navigation
  // detail ↔ synthesis ↔ detail → SSE meurt → barres restent en PROCESSING
  // jusqu'au timeout puis basculent FAILED → hard refresh obligatoire.
  if (source.readyState === EventSource.CLOSED) {
    observer.complete();
  }
  // Sinon (readyState === CONNECTING), on laisse le browser retenter.
};
```

### Fix 2 (defense in depth) — Refetch au retour de visibilité

Ajouter dans `case-file-detail.component.ts` un listener `document.visibilitychange` :
- Quand `document.visibilityState === 'visible'` : `loadAnalysisJobs(id)` + `loadDocuments(id)`.
- Cleanup au `ngOnDestroy`.

Ce 2e fix est défensif : si jamais le SSE meurt malgré le fix 1 (cas pathologique), l'utilisateur récupère un état correct dès qu'il refocus la page.

## Comportement nominal après fix

1. Avocat clique "Analyser le dossier" → SSE EventSource ouvert
2. Avocat navigue vers /synthesis → SSE reste ouvert (notification service au scope `root`)
3. Network blip ou restart pod backend → `EventSource` détecte, retente automatiquement (~3s), `observer.complete()` n'est PAS appelé
4. Backend termine l'analyse → SSE event `CASE_ANALYSIS_DONE` arrive → `events$` notifie tous les composants abonnés (synthesis ET detail si remonté)
5. Si avocat revient sur /detail entre-temps : `ngOnInit` → `loadAnalysisJobs` fetche état `DONE` → barres vertes
6. Si tab navigateur perd focus puis revient : `visibilitychange` → `loadAnalysisJobs` refetch défensif → état toujours synced

## Cas d'erreur

- **EventSource error fatale** (404, 403) : `readyState === CLOSED` → `complete()` propre, le composant peut afficher un fallback.
- **Network down prolongé** : EventSource retente toutes les 3s indéfiniment, polling 3s côté composant détaillé prend le relais.
- **Tab fermé puis réouvert avec session expirée** : SSE 401 → CLOSED → `complete()` → l'utilisateur voit un état stale jusqu'à F5 (acceptable, login expired = autre flow).

## Critères d'acceptation

1. ✅ `AnalysisSseService.stream()` n'appelle plus `source.close()` ni `observer.complete()` sur `onerror` non-fatal (readyState !== CLOSED).
2. ✅ `AnalysisSseService.stream()` `complete()` quand readyState === CLOSED (cas terminal).
3. ✅ `case-file-detail.component.ts` écoute `document.visibilitychange` et refetch `loadAnalysisJobs` + `loadDocuments` au retour visible. Cleanup au ngOnDestroy.
4. ✅ Tests Jest :
   - T-01 `analysis-sse.service.spec.ts` — onerror avec readyState=CONNECTING → observable reste actif
   - T-02 `analysis-sse.service.spec.ts` — onerror avec readyState=CLOSED → observable complete
   - T-03 `case-file-detail.component.spec.ts` — visibilitychange visible → loadAnalysisJobs appelé
   - T-04 `case-file-detail.component.spec.ts` — visibilitychange hidden → loadAnalysisJobs PAS appelé
   - T-05 (régression) `analysis-sse.service.spec.ts` — events normaux toujours reçus
5. ✅ Build npm vert, suite Jest verte sur les fichiers touchés.
6. ✅ Smoke staging post-deploy : naviguer detail ↔ synthesis ↔ detail pendant qu'une analyse tourne → barres se mettent à jour correctement sans hard refresh requis.

## Plan de test

### Frontend (Jest)
- T-SF-186-01-01 : `analysis-sse.service.spec.ts` — `MockEventSource.dispatchError({ readyState: CONNECTING })` → l'observable continue d'émettre (pas complete)
- T-SF-186-01-02 : `analysis-sse.service.spec.ts` — `MockEventSource.dispatchError({ readyState: CLOSED })` → observable complete
- T-SF-186-01-03 : `case-file-detail.component.spec.ts` — émet `document.visibilitychange` avec `visibilityState=visible` → `loadAnalysisJobs` appelé
- T-SF-186-01-04 : `case-file-detail.component.spec.ts` — émet `document.visibilitychange` avec `visibilityState=hidden` → pas d'appel
- T-SF-186-01-05 : non-régression — events SSE normaux toujours reçus, unsubscribe propre

### Smoke staging post-deploy
1. Aller sur Chen 8 (ou n'importe quel dossier avec ≥ 5 docs)
2. Cliquer "Analyser le dossier"
3. Cliquer immédiatement "Voir la synthèse (en cours…)"
4. Attendre 30s, retour à la page dossier
5. Re-cliquer "Voir la synthèse" puis revenir
6. **Attendu** : barres restent en `PROCESSING` puis basculent en `DONE` (vertes) sans hard refresh quand l'analyse termine
7. **Attendu bonus** : changer de tab navigateur 30s puis revenir → barres up-to-date immédiatement

## Tables / endpoints / composants impactés

- **Frontend** :
  - `frontend/src/app/core/services/analysis-sse.service.ts` (logique onerror — ~5 lignes modifiées)
  - `frontend/src/app/case-files/case-file-detail/case-file-detail.component.ts` (listener visibilitychange — ngOnInit + ngOnDestroy ~10 lignes)
- **Tests** :
  - `frontend/src/app/core/services/analysis-sse.service.spec.ts` (3 nouveaux tests)
  - `frontend/src/app/case-files/case-file-detail/case-file-detail.component.spec.ts` (2 nouveaux tests)
- **Backend** : aucun impact.

## Hors périmètre

- Fix du 404 backend `getPartial → "No in-flight analysis"` observé à 21:18:25 sur Chen 8 (race condition timing transactionnel ou cache stale — à investiguer en SF-186-02 si récurrent)
- Mécanisme de fallback polling complet si SSE échoue indéfiniment (le polling existant dans `managePolling` à 3s suffit en V1)
- Refonte du `GlobalAnalysisNotificationService` (track/untrack par caseFileId — V1 garde le pattern actuel)
- Reconnexion explicite avec backoff exponentiel custom (le browser-native EventSource auto-reconnect suffit en V1)

## Analyse de cohérence transversale

- **Auth/Principal** : non concerné — modification interne d'un service existant.
- **Workspace context** : non concerné.
- **Plans/limites** : non concerné.
- **Navigation/routing** : touché — visibilitychange est lié à la navigation/focus mais ne change pas le routing.
- **Outil décisionnel métier** : non concerné — infra UX du pipeline IA.
- **Nouveau pattern partagé** : non — modification interne de 2 fichiers existants.

## Impact par domaine métier

Transversal — aucune adaptation par domaine (Travail / Immigration / Famille) ni par pays (FR / BE). C'est un fix d'infra UX du pipeline IA qui s'applique à toutes les analyses dossier.

## Risques

- **Reconnexion infinie sur erreur permanente** : si EventSource retente indéfiniment sur 401/403 (peu probable car readyState passe à CLOSED), bruit réseau. Mitigation : check `readyState === CLOSED` → complete propre.
- **Refetch trop agressif sur visibilitychange** : si l'utilisateur switche entre tabs très souvent, on refetch à chaque retour. Coût : 1 GET `/analysis-jobs` par retour focus — négligeable. Pas de debounce nécessaire en V1.
- **Régression sur les tests SSE existants** : les mocks `MockEventSource` doivent supporter `readyState` simulé. À vérifier dans la suite Jest existante.
