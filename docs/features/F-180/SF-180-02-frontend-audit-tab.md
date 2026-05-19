# Mini-spec — F-180 / SF-180-02 — Frontend tab « Audit dashboard »

> Étape 1 du cycle. Validée AVANT le dev.

---

## Identifiant

`F-180 / SF-180-02`

## Feature parente

`F-180` — Audit dashboard tiles F-167 (mappers en erreur + tiles dormantes vs actives)

## Statut

`ready`

## Date de création

2026-05-19

## Branche Git

`feat/SF-180-02-frontend-audit-tab`

---

## Objectif

Ajouter une 3e tab « Audit dashboard » dans `/super-admin/backlog` affichant les 3 panels (🔴 mappers en erreur / 🟡 tiles dormantes / 🟢 tiles actives) du rapport d'audit runtime des tiles F-167.

---

## Comportement attendu

### Cas nominal

1. Le super-admin ouvre `/super-admin/backlog`. Le `mat-tab-group` porte désormais 3 tabs : « Produit », « Marketing », « Audit dashboard ».
2. Au clic sur « Audit dashboard » (index 2), le composant charge **en lazy** le rapport via `GET /api/v1/super-admin/dashboard-audit/latest` (pattern `onTabChange` déjà présent pour la tab Marketing).
3. Header de la tab : « Dernier audit : <timestamp lisible> » + bouton « Relancer maintenant ».
4. 3 panels empilés verticalement, ordre = priorité d'action décroissante :
   - **🔴 Mappers en erreur** — `MatTable` triable : colonnes `toolId`, `crashCount`, `lastExceptionClass`, `lastExceptionMessage`, `lastOccurredAt`, + commande `kubectl logs` suggérée par ligne. Vide → message « Aucun mapper en erreur sur les 7 derniers jours ✓ ».
   - **🟡 Tiles dormantes** — liste simple : `tableName` (count 0). Vide → message « Aucune tile dormante ».
   - **🟢 Tiles actives** — `MatTable` triable par `rowCount` desc : colonnes `tableName`, `rowCount`. Vide → message « Aucune analyse produite ».
5. Bouton « Relancer maintenant » → `POST /api/v1/super-admin/dashboard-audit/run`, spinner pendant l'appel, `MatSnackBar` succès au retour, rafraîchit les 3 panels.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Appel API renvoie 403 | Redirection `/case-files` (pattern existant du composant) | 403 |
| Appel API renvoie 401 | Interceptor global → redirection login | 401 |
| Appel API renvoie 5xx / réseau | `MatSnackBar` erreur « Erreur lors du chargement de l'audit », panels en état vide | 5xx |
| Run en cours (bouton déjà cliqué) | Bouton désactivé, spinner affiché — pas de double POST | — |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : non applicable — SF-180-02 ne livre pas un composant décisionnel métier (pas de section `<app-XXX-section>`, pas d'endpoint POST/GET décisionnel par dossier). C'est une tab d'administration.
- [x] **Autres pays / domaines** : non applicable — écran de pilotage transversal, aucune dimension FR/BE ni domaine.
- [x] **Autres UI patterns** : la tab réutilise les patterns déjà présents dans `SuperAdminBacklogComponent` — `mat-tab-group`, `MatTable`, `MatSnackBar`, bouton `mat-stroked-button` + spinner (pattern « Resync now »), `signal()`/`computed()`. Aucun pattern nouveau introduit.
- [x] **Autres flows transversaux** : pas de nouvelle route, pas de guard modifié (la tab vit dans la route `/super-admin/backlog` existante).

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] **Service `DashboardAuditService` (frontend)** : un petit service Angular dédié aux 2 appels HTTP du contrat SF-180-01. Pas réutilisable ailleurs (spécifique à F-180). Pas de pattern concurrent. Alternative envisagée : étendre `BacklogAdminService` — écartée car l'audit dashboard n'a aucun rapport avec le backlog MD→DB ; un service distinct respecte « un composant = une responsabilité ».
- [x] **Aucun composant partagé `shared/`** introduit — tout le rendu vit dans `SuperAdminBacklogComponent` et son template.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `SuperAdminBacklogComponent` | Oui | Étendu : 3e tab + signals/handlers audit |
| `mat-tab-group` existant | Oui | 3e `mat-tab` ajoutée — pas de second niveau d'onglets |
| Pattern « Resync now » bouton+spinner | Oui | Répliqué pour « Relancer maintenant » |
| Modèles TypeScript | Oui | Nouveau fichier `dashboard-audit.model.ts` |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature.
- [x] Non applicable aux autres cibles (pays / domaines / outils métier) — justification : tab d'administration transversale.

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : SF-180-02 livre une **tab d'administration super-admin**, pas un composant décisionnel métier. Pas de section `<app-XXX-section>`, pas d'entrée `TOOL_REGISTRY`, pas de pré-fill IA, pas de validation F-IA-03, pas de `caseFileId`. Le panel F-IA-04 n'est pas touché. Les 5 blocs F-IA-04 (cohérence visuelle décisionnelle, pré-fill, validation F-IA-03, TOOL_REGISTRY, parité domaines) ne s'appliquent qu'aux outils décisionnels par dossier — F-180 n'en est pas un.

---

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — justification : SF-180-02 n'a aucun champ saisissable. La tab affiche un rapport en lecture seule + un bouton de relance. Aucun formulaire, aucune analyse IA.

---

## Contrat API (importé de SF-180-01)

Contrat figé dans `docs/features/F-180/SF-180-01-backend-audit-service.md` section « Contrat API ». Rappel :

- `GET /api/v1/super-admin/dashboard-audit/latest` → `200` `DashboardAuditReport`
- `POST /api/v1/super-admin/dashboard-audit/run` → `200` `DashboardAuditReport`
- `403` si non super-admin, `401` si non authentifié.

Modèle TypeScript `dashboard-audit.model.ts` :

```ts
export interface CrashedMapper {
  toolId: string;
  crashCount: number;
  lastExceptionClass: string;
  lastExceptionMessage: string;
  lastOccurredAt: string;
}
export interface TileTableCount {
  tableName: string;
  rowCount: number;
}
export interface DashboardAuditReport {
  ranAt: string;
  crashedMappers: CrashedMapper[];
  dormantTiles: TileTableCount[];
  activeTiles: TileTableCount[];
}
```

Les tests Jest utilisent un **mock du service** — pas besoin du backend mergé (parallélisation, cf. feature-lifecycle § Étape 4).

---

## Critères d'acceptation

- [ ] Le `mat-tab-group` de `/super-admin/backlog` porte une 3e tab « Audit dashboard » après « Marketing ».
- [ ] Le rapport est chargé en lazy à la première ouverture de la tab (pas avant).
- [ ] Le header affiche le timestamp du dernier run via `DatePipe` et un bouton « Relancer maintenant ».
- [ ] Le panel 🔴 affiche un `MatTable` triable des mappers en erreur ; vide → message explicite « Aucun mapper en erreur ✓ ».
- [ ] Le panel 🟡 liste les tables dormantes ; vide → message explicite.
- [ ] Le panel 🟢 affiche un `MatTable` triable par `rowCount` desc des tiles actives ; vide → message explicite.
- [ ] Le bouton « Relancer maintenant » poste sur `/run`, affiche un spinner, désactivé pendant l'appel, snackbar succès au retour, rafraîchit les panels.
- [ ] Une erreur 403 redirige vers `/case-files` ; une erreur réseau affiche un snackbar erreur.
- [ ] Badges aux couleurs DESIGN_SYSTEM : 🔴 `#FFEBEE`/`#C0392B`, 🟡 `#FFF8E1`/`#F9A825`, 🟢 `#E8F5E9`/`#27AE60`.
- [ ] Tests Jest : chargement nominal, états vides, erreur, relance.

---

## Périmètre

### Hors scope (explicite)

- Backend (instrumentation, service, endpoints, cron) — c'est SF-180-01.
- Drill-down crash → caseFile, export CSV, graphique d'évolution — V2 (spec).
- Polling automatique du rapport — chargement lazy + bouton manuel uniquement.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `auditReport` signal | `null` | rempli au premier `onTabChange` vers l'index 2 |
| `auditLoaded` signal | `false` | passe `true` après le premier chargement réussi |
| `auditRunning` signal | `false` | `true` pendant le POST `/run` |

---

## Contraintes de validation

| Champ | Obligatoire | Format |
|-------|-------------|--------|
| — | — | SF d'affichage en lecture seule, aucun champ saisi, aucune validation d'entrée |

---

## Technique

### Endpoint(s) consommés

| Méthode | URL | Auth |
|---------|-----|------|
| GET | `/api/v1/super-admin/dashboard-audit/latest` | Oui (super-admin) |
| POST | `/api/v1/super-admin/dashboard-audit/run` | Oui (super-admin) |

### Tables impactées

Aucune (SF frontend).

### Migration Liquibase

- [x] Non applicable.

### Composants Angular

- `SuperAdminBacklogComponent` — étendu : 3e tab, signals `auditReport` / `auditLoaded` / `auditRunning` / `auditError`, handlers `loadAudit()` / `triggerAuditRun()`, intégration dans `onTabChange()`.
- `DashboardAuditService` (`core/services/`) — nouveau, 2 méthodes : `getLatest()`, `runAudit()`.
- `dashboard-audit.model.ts` (`core/models/`) — nouveau, interfaces du contrat.

---

## Plan de test

### Tests Jest (`super-admin-backlog.component.spec.ts` étendu + `dashboard-audit.service.spec.ts`)

- [ ] `onTabChange(2)` déclenche `loadAudit()` une seule fois (lazy, pas de rechargement aux ouvertures suivantes).
- [ ] Chargement nominal : `auditReport` peuplé, 3 panels rendus.
- [ ] Panel 🔴 vide → message « Aucun mapper en erreur » ; panel 🟡 vide ; panel 🟢 vide.
- [ ] `triggerAuditRun()` : POST appelé, `auditRunning` true→false, snackbar succès, panels rafraîchis.
- [ ] Erreur 403 → `router.navigate(['/case-files'])`.
- [ ] Erreur réseau → snackbar erreur, pas de crash.
- [ ] `DashboardAuditService.getLatest()` / `runAudit()` → bons verbes/URLs (test `HttpTestingController`).

### Tests d'intégration

- [x] Non applicable côté frontend — l'intégration réelle GET/POST est validée après merge des 2 PRs (parallélisation). Les tests Jest utilisent un mock du service.

### Isolation workspace

- [x] Non applicable — tab super-admin transversale, aucune donnée par workspace.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — SF-180-02 n'ajoute pas de route Angular (la tab vit dans `/super-admin/backlog` existant), ne modifie aucun guard, ne touche pas l'auth ni le contexte workspace ni les plans. Le composant `SuperAdminBacklogComponent` est étendu sans changement de son routing ni de son guard `isSuperAdmin` existant.

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — justification : pas de nouvelle route, pas de guard modifié, pas d'impact auth/workspace/navigation. La tab est un ajout interne au composant.

---

## Dépendances

### Subfeatures bloquantes

- SF-180-01 — fournit les endpoints. Développée **en parallèle** (contrat API figé ci-dessus). La tab n'est utilisable en prod qu'après merge de SF-180-01, mais les tests Jest passent avec un mock.

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` impactée.

---

## Notes et décisions

- **Parallélisation backend/frontend** : SF-180-01 et SF-180-02 sont développées en parallèle sur des branches isolées (`feat/SF-180-01-backend-audit`, `feat/SF-180-02-frontend-audit-tab`). Contrat API figé dans SF-180-01 § « Contrat API » et rappelé ici. Mode autorisé par `feature-lifecycle.md` § Étape 4.
- **Service dédié vs extension `BacklogAdminService`** : service `DashboardAuditService` distinct retenu — l'audit dashboard n'a aucun lien fonctionnel avec la sync backlog MD→DB.
- **Lazy-load** : pattern `onTabChange` déjà éprouvé pour la tab Marketing — répliqué pour la tab Audit.
