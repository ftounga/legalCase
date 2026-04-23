# Mini-spec — F-IA-04 / SF-IA-04-04 Panel se refresh après analyse IA

## Identifiant
`F-IA-04 / SF-IA-04-04` — clôture de F-IA-04.

## Feature parente
`F-IA-04` — Moteur d'affichage conditionnel des outils décisionnels.

## Statut `draft` · Date `2026-04-24` · Branche `feat/SF-IA-04-04-panel-refresh-on-analysis`

---

## Objectif

Brancher `<app-decisional-tools-panel>` sur le bus d'événements `CaseDashboardRefreshService` (F-IA-02) : quand une analyse IA se termine (ou qu'un outil valide une action), le panel recharge automatiquement sa visibilité afin de faire apparaître/disparaître les outils CONTEXTUAL récemment détectés. Clôt F-IA-04.

---

## Comportement attendu

### Cas nominal
1. `case-file-detail` provider `CaseDashboardRefreshService` (déjà le cas depuis F-IA-02).
2. Le panel injecte ce service en `@Optional()` (pour pouvoir l'utiliser ailleurs sans le provider, ex. mini-widget).
3. À `ngOnInit`, après le fetch initial, le panel s'abonne à `refresh$` avec `debounceTime(300)` + `takeUntilDestroyed` (même pattern que `CaseDashboardComponent`).
4. À chaque émission, le panel appelle son `loadVisibility()` en mode sans spinner (reload silencieux).
5. Quand `CaseFileDetailComponent.dashboardRefreshService.triggerRefresh()` est appelé (à la fin d'une ré-analyse complète — ligne 913), le panel se recharge.

### Cas d'erreur
| Situation | Comportement |
|---|---|
| HTTP échoue durant le reload | snackbar + lists vides (comportement existant) |
| Service non fourni | aucun abonnement → comportement actuel (fetch unique au ngOnInit) |

---

## Analyse de cohérence transversale

| Cible | Applicable ? | Traitement |
|---|---|---|
| `CaseDashboardComponent` | N/A — n'est pas touché. Continue d'utiliser le même bus. | — |
| `CaseDashboardRefreshService` | Oui — consommateur supplémentaire | **Intégré** |
| `CaseFileDetailComponent` | Non — `triggerRefresh()` reste au même endroit (ligne 913) | — |
| Autres composants consommateurs potentiels | Non — le panel est le 2ᵉ consommateur, pas besoin de refactor du service | — |

### Décision
- [x] Étendu : abonnement panel au refresh service
- [x] Non applicable ailleurs

### Nouveau pattern UI ou service partagé
Pas de nouveau pattern — réutilisation de `CaseDashboardRefreshService` existant (F-IA-02). Pas de dette de convergence : 1 seul service, 2 consommateurs (dashboard + panel).

---

## Impact par domaine métier

Transversal (moteur), aucune spécificité domaine/pays.

## Parité des domaines métier

Niveau 0 (infrastructure). Règle non applicable.

---

## Critères d'acceptation

- [ ] `DecisionToolsPanelComponent` injecte `CaseDashboardRefreshService` en `@Optional()`
- [ ] À `ngOnInit`, après fetch initial, abonnement à `refresh$` avec `debounceTime(300)` et `takeUntilDestroyed(destroyRef)`
- [ ] À chaque émission → `loadVisibility()` sans spinner (nouvelle surcharge privée ou flag)
- [ ] Cohabitation inchangée avec `CaseDashboardComponent` — les deux se rechargent en parallèle sur le même event
- [ ] Test Jest ajouté : `reloads visibility when CaseDashboardRefreshService emits`
- [ ] Build prod Angular vert
- [ ] 1168+ tests frontend verts

---

## Périmètre

### Hors scope
- V2 : websockets / push temps réel
- V2 : refresh côté dashboard F-IA-02 déclenché par le panel (flux inverse)

### Déjà fait
- Le service `CaseDashboardRefreshService` existe depuis F-IA-02
- Le `triggerRefresh()` est déjà appelé à la fin des ré-analyses (`case-file-detail.component.ts:913`)

---

## Technique

### Fichiers modifiés
- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` — ajout de l'injection + subscribe
- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.spec.ts` — 1 test supplémentaire

### Aucun changement backend, aucune migration, aucun endpoint modifié.

---

## Plan de test

### Tests unitaires Jest
- `reloads visibility silently when CaseDashboardRefreshService emits` : mock le service, trigger refresh, vérifier 2ᵉ appel HTTP

### Tests intégration
Non applicable — changement purement frontend sur un flux déjà testé.

### Tests E2E smoke
Non applicable — pas de changement de route ni de layout.

### Isolation workspace
Inchangée — l'endpoint vérifie déjà `workspace_id`.

---

## Analyse d'impact

### Préoccupations transversales
- [ ] Auth / Principal : N/A
- [ ] Workspace context : N/A
- [ ] Plans / limites : N/A
- [ ] Navigation / routing : N/A
- [ ] Outil décisionnel : Oui — le panel les porte. Pas de changement de règles, seulement un reload plus réactif.

### Composants impactés
- `DecisionToolsPanelComponent` (seul)

### Smoke tests E2E
Non applicable.

---

## Dépendances
- SF-IA-04-03 done ✓

---

## Notes et décisions

### Pourquoi ne pas réutiliser l'instance existante à un niveau module ?
Le service est `@Injectable()` simple (pas `providedIn: 'root'`) — instance unique par dossier. On reste cohérent avec ce design : 1 event bus par case-file. Inject en `@Optional()` côté panel au cas où il serait utilisé hors contexte case-file-detail.

### Pourquoi pas de spinner sur le reload ?
UX : l'avocat vient de cliquer "Réanalyser", il n'a pas besoin de voir un spinner supplémentaire dans le panel. Les cards des composants décisionnels gèrent eux-mêmes leur état de chargement interne.
