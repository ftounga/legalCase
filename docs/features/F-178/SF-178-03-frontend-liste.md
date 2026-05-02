# Mini-spec — F-178 / SF-178-03 Frontend liste + filtres + freshness + Resync

## Identifiant

`F-178 / SF-178-03`

## Feature parente

`F-178` — Visualiseur de backlog dans super-admin

## Statut

`ready`

## Date de création

2026-05-02

## Branche Git

`feat/SF-178-03-frontend-liste`

---

## Objectif

Livrer l'écran Angular standalone `/super-admin/backlog` avec tabs **Produit** (`backlog_features`) et **Marketing** (`backlog_marketing_tasks`), liste filtrable, indicateur de fraîcheur de la sync et bouton "Resync now" — pour permettre à l'utilisateur de répondre en < 10 s à "qu'est-ce qui est bloqué / ready to dev / Terminé ?" sans ouvrir un MD.

---

## Comportement attendu

### Cas nominal

1. L'utilisateur authentifié super-admin accède à `/super-admin/backlog` (lazy-loaded). Si non super-admin → redirection `/case-files` (pattern de `super-admin.component.ts`).
2. Au chargement initial, l'écran récupère :
   - `GET /api/v1/super-admin/backlog/freshness` → indicateur fraîcheur affiché en haut.
   - `GET /api/v1/super-admin/backlog/features?page=0&size=50` → tab Produit pré-chargée.
3. L'utilisateur peut basculer Tab **Produit** ↔ **Marketing**. La tab Marketing déclenche son premier chargement à la première bascule (lazy).
4. Sur la tab **Produit** :
   - Filtres MatSelect : Statut (`PLANNED` / `READY` / `IN_PROGRESS` / `BLOCKED` / `DONE` / `PARTIAL` / `ABSORBED` / `UNKNOWN`), Domaine (`DROIT_TRAVAIL` / `IMMIGRATION` / `FAMILLE` / `TRANSVERSAL` / `MARKETING`), Priorité (`HIGH` / `MEDIUM` / `LOW` / `UNKNOWN`).
   - Recherche texte (champ MatInput) : appliquée sur titre et code (debounce 250 ms).
   - Tableau MatTable colonnes : `code` (JetBrains Mono), `title`, `targetVersion`, `status` (badge), `domain` (badge), `priority` (badge), `updatedAt` (date relative).
   - MatPaginator (50/100/200, défaut 50).
5. Sur la tab **Marketing** :
   - Filtre MatSelect : Statut (`A_FAIRE` / `REDIGE` / `EN_COURS` / `TERMINE` / `BLOQUE` / `UNKNOWN`).
   - Recherche texte (debounce 250 ms).
   - Tableau colonnes : `code` (JetBrains Mono), `title`, `status` (badge), `category`, `updatedAt`.
6. Indicateur fraîcheur en haut de la page :
   - Couleur verte `#27AE60` + texte "Synchronisé il y a X minutes" si `freshness.status = OK`.
   - Couleur orange `#C9973A` + "Sync obsolète depuis X minutes" si `STALE`.
   - Couleur rouge `#C0392B` + "Dernière sync en erreur" si `ERROR`.
7. Bouton **"Resync now"** (mat-stroked-button + icon `sync`) à côté de l'indicateur fraîcheur :
   - Click → POST `/api/v1/super-admin/backlog/sync` → snackbar succès "Resync OK ({featuresCount} features, {marketingCount} marketing, {durationMs} ms)" + reload des listes + de la freshness.
   - Erreur → snackbar erreur "Échec resync — voir les logs".
   - Bouton désactivé pendant la requête (spinner inline).

### Cas d'erreur

| Situation | Comportement | Trace |
|-----------|--------------|-------|
| 403 sur n'importe quel endpoint | redirect `/case-files` (déjà géré par AuthService + handleur HTTP) | — |
| 500 / network sur `freshness` | bandeau rouge "Indicateur fraîcheur indisponible", tableau quand même chargé | snackbar |
| 500 / network sur `features` | snackbar "Erreur lors du chargement", tableau vide affiché | snackbar |
| Resync click pendant un resync en cours | bouton désactivé, click ignoré | — |
| Filtre + search texte combinés sans résultat | tableau vide + message "Aucun élément ne correspond aux filtres" | — |

---

## Analyse de cohérence transversale

- [x] **Autres outils / pays / domaines** : N/A — feature transversale (super-admin interne, indépendante des dossiers).
- [x] **Pattern super-admin existant** : `super-admin.component.ts` (route `/super-admin`) sert de modèle. Mêmes imports `MatTable`, `MatPaginator`, `MatSnackBar`. Pas de duplication — composant indépendant lazy-loaded.
- [x] **Pattern UI tabs** : `MatTabsModule` standard Material, déjà utilisé ailleurs dans l'app (case-file-detail).
- [x] **Pattern badge statut** : créer 1 composant `<app-backlog-status-badge>` réutilisable (Produit + Marketing) plutôt que dupliquer le mapping couleur dans 2 templates. Pré-fab pour SF-178-04 (modal détail) + SF-178-05 (kanban) qui réutiliseront le même badge.
- [x] **Pattern relative date** : utiliser `DatePipe` Angular standard (format court court) — pas de "il y a X minutes" custom (laisse au composant freshness).
- [x] **Cohérence freshness vs RxJS** : pas de polling — refresh manuel uniquement sur Resync ou rechargement. Acceptable pour un outil interne (pas de live update requis V1).

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] **Pattern `<app-backlog-status-badge>`** : composant standalone partagé (`frontend/src/app/super-admin/backlog/shared/backlog-status-badge.component.ts`).
  - Inputs : `status: BacklogStatus | BacklogMarketingStatus | string` + `kind: 'feature' | 'marketing'`.
  - Mapping couleur : DONE/TERMINE → vert ; IN_PROGRESS/EN_COURS → bleu primary ; BLOCKED/BLOQUE → rouge ; READY → or ; PLANNED/A_FAIRE → gris ; PARTIAL/REDIGE → bleu pâle ; ABSORBED → violet (rare) ; UNKNOWN → gris très clair.
  - **Zones de réutilisation** : (1) liste features SF-178-03, (2) liste marketing SF-178-03, (3) modal détail SF-178-04, (4) cards kanban SF-178-05. **Classement** : harmonisation immédiate dans cette SF.
  - **Patterns concurrents existants** : aucun — `case-files-list` utilise des chips inline de Material direct sans composant dédié. Pas de risque de divergence ici, on reste isolé dans le scope `super-admin/backlog/*`.
- [x] **Pattern `BacklogAdminService`** : service Angular `@Injectable({providedIn: 'root'})` dans `frontend/src/app/core/services/backlog-admin.service.ts`. **Zones de réutilisation** : SF-178-03 + SF-178-04 + SF-178-05 (toutes les SF restantes). Centralisation immédiate. Pas de duplication avec `SuperAdminService` existant (endpoints différents `/super-admin/backlog/*`).

### Décision

- [x] Étendu à toutes les cibles applicables (les 3 SF restantes réutiliseront le badge + service).

---

## Critères d'acceptation

- [ ] Route `/super-admin/backlog` fonctionne (lazy-loaded depuis `app.routes.ts`)
- [ ] Si user non super-admin → redirect `/case-files` (pattern existant)
- [ ] Tabs **Produit** et **Marketing** présentes, switch fonctionnel
- [ ] Tab Produit : filtres status/domain/priority + recherche texte + MatTable + MatPaginator opérationnels
- [ ] Tab Marketing : filtre status + recherche texte + MatTable + MatPaginator opérationnels
- [ ] Indicateur fraîcheur affiché en haut avec couleur conforme à `status` (OK / STALE / ERROR)
- [ ] Bouton "Resync now" déclenche POST `/sync`, snackbar succès/erreur, recharge données
- [ ] Composant partagé `<app-backlog-status-badge>` créé, utilisé sur les 2 tabs
- [ ] Service `BacklogAdminService` créé, exposant les 5 méthodes (`searchFeatures`, `searchMarketingTasks`, `getFreshness`, `triggerSync`, `getFeatureDetail` — la dernière sera consommée par SF-178-04 mais exposée dès maintenant pour éviter une PR-dépendance)
- [ ] Modèle TS `backlog.model.ts` aligné sur les records DTOs backend (voir Contrat API ci-dessous)
- [ ] Tests Jest (≥ 8) : composant chargé, switch tab, filtre appliqué, search debounce, resync succès, resync erreur, redirect non super-admin, badge mappings
- [ ] Build frontend `npm run build` vert
- [ ] Tous tests existants Jest verts

---

## Contrat API (consommé)

Ces endpoints existent côté backend (SF-178-01 + SF-178-02). Documentés ici pour traçabilité front.

### `GET /api/v1/super-admin/backlog/features`

Query params (tous optionnels) : `status`, `domain`, `priority`, `search`, `page` (défaut 0), `size` (défaut 50).

Réponse : `Page<BacklogFeatureSummary>` :

```json
{
  "content": [
    {
      "id": "uuid",
      "code": "F-178",
      "title": "Visualiseur de backlog dans super-admin",
      "targetVersion": "V8+",
      "status": "IN_PROGRESS",
      "domain": "TRANSVERSAL",
      "priority": "MEDIUM",
      "updatedAt": "2026-05-02T00:00:00Z"
    }
  ],
  "totalElements": 184,
  "totalPages": 4,
  "size": 50,
  "number": 0
}
```

### `GET /api/v1/super-admin/backlog/marketing-tasks`

Query params : `status`, `search`, `page`, `size`. Réponse : `Page<BacklogMarketingTaskSummary>` :

```json
{
  "content": [
    {
      "id": "uuid",
      "code": "M-71",
      "title": "Cadrage budget marketing 2026 H2",
      "status": "TERMINE",
      "category": "Cadrage stratégique",
      "updatedAt": "2026-04-30T00:00:00Z"
    }
  ]
}
```

### `GET /api/v1/super-admin/backlog/freshness`

Réponse `BacklogFreshness` :

```json
{
  "lastSyncAt": "2026-05-02T00:55:00Z",
  "lastSuccessAt": "2026-05-02T00:55:00Z",
  "status": "OK",
  "minutesSinceLastSync": 3
}
```

`status` ∈ {`OK`, `STALE`, `ERROR`, `UNKNOWN`}.

### `POST /api/v1/super-admin/backlog/sync`

Body : aucun. Réponse `BacklogSyncResult` :

```json
{
  "runId": "uuid",
  "durationMs": 312,
  "featuresCount": 184,
  "subfeaturesCount": 612,
  "marketingCount": 76,
  "orphansMarked": 0,
  "success": true
}
```

### `GET /api/v1/super-admin/backlog/features/{code}`

(Consommé en SF-178-04 — ajouté dans le service maintenant pour éviter une PR-dépendance plus tard.)

Réponse `BacklogFeatureDetail` (record) :

```json
{
  "id": "uuid",
  "code": "F-178",
  "title": "...",
  "targetVersion": "V8+",
  "status": "IN_PROGRESS",
  "description": "<markdown long>",
  "domain": "TRANSVERSAL",
  "priority": "MEDIUM",
  "sourceFile": "docs/PRODUCT_SPEC.md",
  "sourceLine": 335,
  "parsedAt": "2026-05-02T00:55:00Z",
  "updatedAt": "2026-05-02T00:55:00Z",
  "subfeatures": [
    {
      "id": "uuid",
      "code": "SF-178-01",
      "title": "Backend infra",
      "status": "DONE",
      "description": "...",
      "sourceLine": null,
      "updatedAt": "2026-05-01T00:00:00Z"
    }
  ]
}
```

---

## Périmètre

### Hors scope (volontaire)

- **Modal/drawer détail au clic sur une ligne** → SF-178-04
- **Vue kanban** → SF-178-05
- **Édition depuis l'UI** → règle gouvernance Étape 7 CLAUDE.md (interdite)
- **Polling automatique freshness** → manuel via Resync ou F5 ; outil interne pas critique
- **Export CSV/JSON** → hors scope V1 (mentionné PRODUCT_SPEC.md)
- **Tri custom des colonnes** → tri par `code` côté backend par défaut, suffit V1
- **Drag-and-drop** → hors scope V1

---

## Technique

### Arborescence frontend

```
frontend/src/app/super-admin/backlog/
├── super-admin-backlog.component.ts        (component standalone, route /super-admin/backlog)
├── super-admin-backlog.component.html
├── super-admin-backlog.component.scss
├── super-admin-backlog.component.spec.ts
└── shared/
    ├── backlog-status-badge.component.ts   (composant partagé status + marketing-status)
    └── backlog-status-badge.component.spec.ts
```

### Modèle TS

Nouveau fichier `frontend/src/app/core/models/backlog.model.ts` aligné sur les records backend :

```ts
export type BacklogStatus =
  | 'PLANNED' | 'READY' | 'IN_PROGRESS' | 'BLOCKED'
  | 'DONE' | 'PARTIAL' | 'ABSORBED' | 'UNKNOWN';

export type BacklogMarketingStatus =
  | 'A_FAIRE' | 'REDIGE' | 'EN_COURS' | 'TERMINE' | 'BLOQUE' | 'UNKNOWN';

export type BacklogDomain =
  | 'DROIT_TRAVAIL' | 'IMMIGRATION' | 'FAMILLE'
  | 'TRANSVERSAL' | 'MARKETING' | 'UNKNOWN';

export type BacklogPriority = 'HIGH' | 'MEDIUM' | 'LOW' | 'UNKNOWN';

export interface BacklogFeatureSummary { ... }
export interface BacklogFeatureDetail { ... }
export interface BacklogSubfeature { ... }
export interface BacklogMarketingTaskSummary { ... }
export interface BacklogFreshness {
  lastSyncAt: string | null;
  lastSuccessAt: string | null;
  status: 'OK' | 'STALE' | 'ERROR' | 'UNKNOWN';
  minutesSinceLastSync: number | null;
}
export interface BacklogSyncResult { ... }
```

### Service

`frontend/src/app/core/services/backlog-admin.service.ts` :
- `searchFeatures(filters, page, size): Observable<PageResponse<BacklogFeatureSummary>>`
- `getFeatureDetail(code: string): Observable<BacklogFeatureDetail>` (consommé SF-178-04 — exposé dès maintenant)
- `searchMarketingTasks(filters, page, size): Observable<PageResponse<BacklogMarketingTaskSummary>>`
- `getFreshness(): Observable<BacklogFreshness>`
- `triggerSync(): Observable<BacklogSyncResult>`

### Composant principal

`super-admin-backlog.component.ts` (standalone) :
- Imports : `MatTabsModule`, `MatTableModule`, `MatPaginatorModule`, `MatSelectModule`, `MatFormFieldModule`, `MatInputModule`, `MatButtonModule`, `MatIconModule`, `MatProgressSpinnerModule`, `MatSnackBar`, `DatePipe`, `BacklogStatusBadgeComponent`.
- Signals pour state : `features` (`BacklogFeatureSummary[]`), `marketing` (...), `freshness`, `loading`, `resyncing`, paginations indépendantes par tab, filtres par tab.
- `RxJS Subject<string>` + `debounceTime(250)` sur les inputs search.
- `effect()` ou simplement reload manuel sur changement de filtre (préférer `effect` avec `signal` pour les filters).
- ngOnInit : redirect si non-super-admin, charge freshness + features tab par défaut.

### Lien depuis super-admin actuel

Ajouter un bouton ou lien `<a routerLink="/super-admin/backlog">Backlog</a>` dans le template `super-admin.component.html` (header de page super-admin) — non bloquant : la route est utilisable directement.

### Route

Ajouter dans `app.routes.ts` après la route `/super-admin` :

```ts
{
  path: 'super-admin/backlog',
  loadComponent: () => import('./super-admin/backlog/super-admin-backlog.component')
    .then(m => m.SuperAdminBacklogComponent)
}
```

---

## Plan de test

### Tests Jest (composant) ≥ 8

- T-01 : `ngOnInit` → si user non super-admin → redirect `/case-files`
- T-02 : `ngOnInit` → si super-admin → charge freshness + features (verify HttpClient calls)
- T-03 : Switch tab Marketing déclenche premier chargement marketing-tasks
- T-04 : Filtre status/domain/priority appliqué → reload features avec params
- T-05 : Search input debounced 250 ms → reload après debounce
- T-06 : Click "Resync now" → POST /sync, snackbar succès, reload listes
- T-07 : Click "Resync now" en erreur → snackbar erreur, listes inchangées
- T-08 : Indicateur fraîcheur affiche couleur correcte selon status (OK / STALE / ERROR)

### Tests Jest (badge) ≥ 3

- T-09 : badge feature DONE → classe `.badge--done` + label "Terminée"
- T-10 : badge marketing TERMINE → classe `.badge--done` + label "Terminé"
- T-11 : badge BLOCKED / BLOQUE → classe `.badge--blocked` + label correct

### Isolation workspace

N/A — feature super-admin transversale, pas de `workspace_id` dans les requêtes.

---

## Analyse d'impact

- [x] Aucune préoccupation transversale touchée (auth Principal inchangé, workspace context inchangé, plans inchangés, navigation : ajout d'1 route lazy ne casse rien).
- [x] Le route `/super-admin/backlog` est lazy-loaded → bundle initial inchangé.
- [x] Pas de modification du shell ou du header.

### Composants existants potentiellement impactés

- `app.routes.ts` : ajout d'1 route lazy après `/super-admin`.
- `super-admin.component.html` : ajout d'1 lien optionnel "Backlog" (non bloquant).
- Pas d'autre fichier modifié.
