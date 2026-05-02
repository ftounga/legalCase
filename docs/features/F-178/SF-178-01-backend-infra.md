# Mini-spec — F-178 / SF-178-01 Backend infra (tables + parser + endpoints super-admin)

> Première subfeature du visualiseur de backlog (F-178).
> Source de vérité = Markdown (PRODUCT_SPEC.md + MARKETING_BACKLOG.md). DB = cache de lecture (Option A).

---

## Identifiant

`F-178 / SF-178-01`

## Feature parente

`F-178` — Visualiseur de backlog dans super-admin (produit + marketing)

## Statut

`done`

## Date de création

2026-05-01

## Branche Git

`feat/SF-178-01-backend-infra`

---

## Objectif

Poser les fondations backend (tables + parser MD + endpoints super-admin) qui alimenteront l'écran F-178 — sans encore activer le `@Scheduled` (SF-178-02).

---

## Comportement attendu

### Cas nominal

1. **`POST /api/v1/super-admin/backlog/sync`** appelé par un super-admin.
2. Le service `BacklogSyncService` lit `docs/PRODUCT_SPEC.md` et `docs/MARKETING_BACKLOG.md` depuis le filesystem.
3. Le parser découpe en sections, extrait les **tableaux de features** (regex `| F-XX | titre | … |`), parse en `ParsedFeature` (code, title, target_version, status, description, domain, priority).
4. Le parser identifie les **subfeatures** mentionnées dans les notes via regex `SF-XX-YY` + statut explicite (`SF-XX-YY mergée`, etc.).
5. Idem pour MARKETING_BACKLOG.md (sections M-XX, statuts `À faire / Rédigé / En cours / Terminé / Bloqué`).
6. Upsert idempotent par `code` (clé fonctionnelle) dans 3 tables. Suppressions : codes absents → `is_orphaned=true` (pas DELETE).
7. Audit dans `backlog_sync_runs`.
8. Retour HTTP 200 avec `BacklogSyncResult { runId, durationMs, featuresCount, subfeaturesCount, marketingCount, orphansMarked, success }`.

### GET endpoints (super-admin only)

- `GET /features?status=&domain=&priority=&search=&page=&size=` → `Page<BacklogFeatureSummary>`.
- `GET /features/{code}` → `BacklogFeatureDetail` (subfeatures imbriquées).
- `GET /marketing-tasks?status=&search=&page=&size=` → `Page<BacklogMarketingTaskSummary>`.
- `GET /sync-runs?limit=10` → `List<BacklogSyncRunSummary>`.
- `GET /freshness` → `{ lastSyncAt, lastSuccessAt, status: 'OK'|'STALE'|'ERROR', minutesSinceLastSync }`.

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|--------------|-----------|
| Utilisateur non super-admin | `assertSuperAdmin` rejette | 403 |
| Pas authentifié | Spring Security rejette | 401 |
| Fichier MD introuvable | `BacklogSyncRun.success=false`, exception levée, audit row persistée | 500 |
| Code feature dupliqué dans MD | Dernière occurrence l'emporte (warn log) | 200 |
| Code dans DB absent du MD courant | `is_orphaned=true` | — |
| Feature détail inconnue | 404 | 404 |

---

## Analyse de cohérence transversale

- [x] **Autres outils métier / pays / domaines** : non concerné — feature transversale interne.
- [x] **Patterns UI** : N/A (backend only).
- [x] **`assertSuperAdmin` (F-76)** : réutilisé tel quel sur les 6 endpoints.
- [x] **Pagination `Page<T>` + `@PageableDefault` (F-79)** : réutilisé sur les 3 GET paginés.
- [x] **SF-178-02** : `BacklogSyncService.sync(SyncTrigger)` exposé pour appel par futur `@Scheduled`.

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [x] SF parallèles : SF-178-02 (cron), SF-178-03/04/05 (UI)

---

## Critères d'acceptation

- [x] Migration Liquibase crée 4 tables (`backlog_features`, `backlog_subfeatures`, `backlog_marketing_tasks`, `backlog_sync_runs`) avec index documentés
- [x] `POST /sync` parse PRODUCT_SPEC.md + MARKETING_BACKLOG.md et upsert de manière idempotente (2 appels successifs ne créent pas de doublons)
- [x] Le parser extrait toutes les features F-XX, leurs cibles (V1/V2/V3/V8+), statuts, domaines, priorités
- [x] Le parser extrait les subfeatures mentionnées dans les notes (regex `SF-XX-YY`) avec leur statut quand explicite
- [x] Le parser extrait correctement toutes les tâches M-XX
- [x] `GET /features?status=&domain=&priority=&search=&page=` filtré et paginé
- [x] `GET /features/{code}` renvoie le détail avec subfeatures imbriquées (404 si inconnu)
- [x] `GET /marketing-tasks` paginé et filtré
- [x] `GET /sync-runs?limit=10` ordre desc
- [x] `GET /freshness` renvoie OK/STALE/ERROR
- [x] Tous les endpoints rejettent un appel non super-admin avec 403
- [x] Tests UT parser (15 tests : nominal, statuts, domaines, priorités, subfeatures, dupliqués, malformés, marketing)
- [x] Tests UT sync service (5 tests : nominal, idempotence, orphans, audit trigger, fichier manquant)
- [x] Tests IT controller (11 tests : 401/403/200, filtres, détail, marketing, sync-runs, freshness, rejet super-admin sur tous endpoints)

---

## Périmètre

### Hors scope

- `@Scheduled` cron — SF-178-02
- UI Angular — SF-178-03/04/05
- Édition depuis l'UI (Étape 7 CLAUDE.md)
- Drag-and-drop kanban — V2
- Exports CSV/JSON — V2
- Notifications — V2
- Intégration GitHub — V2
- Parsing graphe de dépendances — V2

---

## Technique

### Endpoints

| Méthode | URL | Auth |
|---------|-----|------|
| POST | `/api/v1/super-admin/backlog/sync` | SUPER_ADMIN |
| GET | `/api/v1/super-admin/backlog/features` | SUPER_ADMIN |
| GET | `/api/v1/super-admin/backlog/features/{code}` | SUPER_ADMIN |
| GET | `/api/v1/super-admin/backlog/marketing-tasks` | SUPER_ADMIN |
| GET | `/api/v1/super-admin/backlog/sync-runs` | SUPER_ADMIN |
| GET | `/api/v1/super-admin/backlog/freshness` | SUPER_ADMIN |

### Tables impactées

| Table | Opération |
|-------|-----------|
| `backlog_features` | CREATE + INSERT/UPDATE |
| `backlog_subfeatures` | CREATE + INSERT/UPDATE |
| `backlog_marketing_tasks` | CREATE + INSERT/UPDATE |
| `backlog_sync_runs` | CREATE + INSERT |

### Migration Liquibase

`db/changelog/migrations/199-create-backlog-tables.xml` — 4 changeSets avec PK UUID, index sur status/domain/parent_feature_id/started_at.

### Composants Java

- Enums : `BacklogStatus` (PLANNED, READY, IN_PROGRESS, BLOCKED, DONE, PARTIAL, ABSORBED, UNKNOWN), `BacklogMarketingStatus`, `BacklogDomain`, `BacklogPriority`, `SyncTrigger`
- Entités : `BacklogFeatureEntity`, `BacklogSubfeatureEntity`, `BacklogMarketingTaskEntity`, `BacklogSyncRunEntity`
- Repositories : 4 (avec méthodes `findByCode`, `search`, `findByOrphanedFalse`, etc.)
- DTOs : `BacklogDtos.java` (records imbriqués)
- `BacklogProperties` (`@ConfigurationProperties("app.backlog")`) — chemins MD configurables
- `BacklogMarkdownParser` — service stateless
- `BacklogSyncService` — orchestre parser + upsert + run audit
- `BacklogQueryService` — endpoints GET (filtres + pagination + freshness)
- `BacklogController` — REST `/api/v1/super-admin/backlog/*`

### Mapping statuts (parser)

**Technical** : ABSORBED ("Absorbé par F-") > DONE ("Terminée") > IN_PROGRESS > PARTIAL > READY > PLANNED ("À planifier") > BLOCKED ("Bloqué"). Ordre : marqueurs spécifiques d'abord car "Bloqué" peut apparaître en dépendance dans n'importe quelle description.

**Marketing** : DONE / IN_PROGRESS / BLOCKED / DRAFTED / TODO.

**Domaine** : détecté depuis section Markdown parente ("Droit du travail" → WORK, etc.).

**Priorité** : 🔴 → HIGH, 🟡 → MEDIUM, 🟢 → LOW.

---

## Plan de test

### Tests UT (20)

Parser (15) — voir `BacklogMarkdownParserTest` :
nominal, statuts (Terminée/En cours/Bloqué/Absorbée/Planifié), domaines, priorités, target version, subfeatures, statut subfeature dans contexte, marketing nominal+5 statuts, catégorie depuis heading, code dupliqué, vide, malformé, real PRODUCT_SPEC.md sans crash.

Sync (5) — voir `BacklogSyncServiceTest` :
persiste, idempotent, orphan, audit trigger, fichier manquant.

### Tests IT (11)

`BacklogControllerIT` :
401 sans auth, 403 user normal, 200 super-admin, paginé, filtre status, détail+subfeatures, 404 unknown, marketing, sync-runs, freshness, rejet 403 sur 5 endpoints.

### Isolation workspace

N/A — feature super-admin transversale.

---

## Analyse d'impact

- [x] Aucune préoccupation transversale touchée.
- Modification annexe : `SuperAdminService.assertSuperAdmin` annoté `@Transactional(readOnly=true)` pour résoudre `LazyInitializationException` quand appelé depuis le BacklogController (qui n'est pas lui-même transactional). Tests existants `SuperAdminControllerIT` (21) et `SuperAdminServiceTest` (10) restent verts.

---

## Résultats

- 3161/3161 tests verts (parser 15 + sync 5 + controller IT 11 + reste 3130)
- 1148 sources compilées sans warning nouveau
- Build vert
