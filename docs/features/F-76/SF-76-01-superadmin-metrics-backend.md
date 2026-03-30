# Mini-spec — F-76 / SF-76-01 — Backend métriques super-admin

## Identifiant

`F-76 / SF-76-01`

## Feature parente

`F-76` — Tableau de bord super-admin — métriques produit

## Statut

`draft`

## Date de création

2026-03-31

## Branche Git

`feat/SF-76-01-superadmin-metrics-backend`

---

## Objectif

Exposer un endpoint `GET /api/v1/super-admin/metrics` retournant les métriques agrégées de la plateforme, accessible au super-admin uniquement.

---

## Comportement attendu

### Cas nominal

Le super-admin appelle `GET /api/v1/super-admin/metrics` → réponse JSON avec les métriques suivantes :

| Champ | Description |
|-------|-------------|
| `totalWorkspaces` | Nombre total de workspaces |
| `activeWorkspaces30d` | Workspaces avec au moins 1 analyse DONE dans les 30 derniers jours |
| `inactiveWorkspaces30d` | Workspaces sans aucune analyse DONE dans les 30 derniers jours |
| `trialWorkspaces` | Workspaces sur plan FREE |
| `paidWorkspaces` | Workspaces sur plan payant (SOLO / TEAM / PRO) |
| `conversionRatePct` | `paidWorkspaces * 100.0 / totalWorkspaces` — double, 0 si totalWorkspaces = 0 |
| `analysesLast7Days` | Nombre d'analyses DONE créées dans les 7 derniers jours |
| `analysesLast30Days` | Nombre d'analyses DONE créées dans les 30 derniers jours |
| `newWorkspacesLast30Days` | Workspaces créés dans les 30 derniers jours |

Toutes les métriques sont calculées depuis les tables existantes (`workspaces`, `subscriptions`, `case_analyses`, `case_files`). Aucune table nouvelle.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Appelant non super-admin | Accès refusé | 403 |
| Appelant non authentifié | Non autorisé | 401 |

---

## Critères d'acceptation

- [ ] `GET /api/v1/super-admin/metrics` → 200 avec les 9 champs pour un super-admin
- [ ] `GET /api/v1/super-admin/metrics` → 403 pour un utilisateur standard authentifié
- [ ] `conversionRatePct` = 0.0 si `totalWorkspaces` = 0
- [ ] `activeWorkspaces30d` + `inactiveWorkspaces30d` = `totalWorkspaces`
- [ ] `analysesLast7Days` ≤ `analysesLast30Days`

---

## Périmètre

### Hors scope (explicite)

- Pas de pagination ni de filtrage par période personnalisée
- Pas de graphique / série temporelle — compteurs uniquement
- Pas de nouvelle table ni migration Liquibase
- SF-76-02 (frontend) est une subfeature séparée

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/super-admin/metrics` | Oui | `is_super_admin = true` |

### Nouveau DTO

```java
record SuperAdminMetricsResponse(
    long totalWorkspaces,
    long activeWorkspaces30d,
    long inactiveWorkspaces30d,
    long trialWorkspaces,
    long paidWorkspaces,
    double conversionRatePct,
    long analysesLast7Days,
    long analysesLast30Days,
    long newWorkspacesLast30Days
) {}
```

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `workspaces` | SELECT | totalWorkspaces, newWorkspacesLast30Days |
| `subscriptions` | SELECT | trialWorkspaces (FREE), paidWorkspaces (SOLO/TEAM/PRO) |
| `case_analyses` | SELECT | analyses DONE par période |
| `case_files` | SELECT JOIN | pour remonter au workspace depuis case_analysis |

### Migration Liquibase

- [x] Non applicable — aucune nouvelle table

### Composants Angular (si applicable)

- Non applicable — SF-76-01 est backend uniquement

---

## Plan de test

### Tests unitaires

- [ ] `SuperAdminService.getMetrics()` — cas nominal : valeurs cohérentes
- [ ] `SuperAdminService.getMetrics()` — conversionRatePct = 0.0 si aucun workspace
- [ ] `SuperAdminService.getMetrics()` — 403 si appelant non super-admin

### Tests d'intégration

- [ ] `GET /api/v1/super-admin/metrics` → 200 + 9 champs pour super-admin
- [ ] `GET /api/v1/super-admin/metrics` → 403 pour utilisateur standard
- [ ] `activeWorkspaces30d` + `inactiveWorkspaces30d` = `totalWorkspaces`

### Isolation workspace

- [ ] Non applicable — endpoint super-admin, agrégation globale intentionnelle

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Auth / Principal** — utilise `isSuperAdmin()` comme les endpoints existants — pattern inchangé
- [ ] **Workspace context** — non applicable
- [ ] **Plans / limites** — non applicable
- [ ] **Navigation / routing frontend** — non applicable (SF-76-02)

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `SuperAdminController` | Nouveau endpoint ajouté — endpoints existants inchangés | Tests IT existants doivent rester verts |
| `SuperAdminService` | Nouvelle méthode — méthodes existantes inchangées | `SuperAdminServiceTest` existant doit rester vert |

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — pas de changement de routing ni d'auth

---

## Dépendances

### Subfeatures bloquantes

- Aucune

### Questions ouvertes impactées

- Aucune

---

## Notes et décisions

- Les workspaces sans subscription sont comptés comme FREE (trial par défaut)
- `inactiveWorkspaces30d` = workspaces qui n'apparaissent pas dans la liste des workspaces actifs
- Pas de cache — endpoint super-admin à faible fréquence d'appel
