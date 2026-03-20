# Mini-spec — F-25 / SF-25-02 Consommation LLM par workspace (super-admin)

## Identifiant

`F-25 / SF-25-02`

## Feature parente

`F-25` — Super-admin plateforme

## Statut

`ready`

## Date de création

2026-03-20

## Branche Git

`feat/SF-25-02-usage-super-admin`

---

## Objectif

Exposer `GET /api/v1/super-admin/usage` retournant l'agrégation des tokens et coûts LLM par workspace, tous workspaces confondus, accessible aux super-admins uniquement.

---

## Comportement attendu

### Cas nominal

Un super-admin appelle `GET /api/v1/super-admin/usage` et reçoit la liste de tous les workspaces avec leur consommation agrégée : tokens input, tokens output, coût total. Les workspaces sans événements d'usage retournent des totaux à 0.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Utilisateur non `is_super_admin` | Accès refusé | 403 |
| Utilisateur non authentifié | Non autorisé | 401 |

---

## Critères d'acceptation

- [ ] `GET /api/v1/super-admin/usage` retourne 200 avec un super-admin
- [ ] Chaque entrée contient : `workspaceId`, `workspaceName`, `totalTokensInput`, `totalTokensOutput`, `totalCost`
- [ ] L'agrégation est correcte : un workspace avec 2 usage_events voit ses tokens et coûts sommés
- [ ] Un workspace sans usage_events apparaît dans la liste avec des totaux à 0
- [ ] 403 si `is_super_admin = false`, 401 si non authentifié

---

## Périmètre

### Hors scope

- Détail par dossier ou par utilisateur (géré dans `/workspace/admin` via SF-18)
- Filtrage par période
- Export CSV

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/super-admin/usage` | Oui | `is_super_admin = true` |

### Réponse

```json
[
  {
    "workspaceId": "uuid",
    "workspaceName": "Cabinet Alpha",
    "totalTokensInput": 12000,
    "totalTokensOutput": 4500,
    "totalCost": 0.0842
  }
]
```

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| usage_events | SELECT + agrégation | GROUP BY workspace_id |
| workspaces | SELECT | JOIN pour récupérer le nom |

### Migration Liquibase

- [x] Non applicable

### Composants Angular (si applicable)

N/A — backend uniquement

---

## Plan de test

### Tests unitaires

- [ ] `SuperAdminService.getUsageByWorkspace()` — 2 workspaces avec usage → agrégation correcte
- [ ] `SuperAdminService.getUsageByWorkspace()` — workspace sans usage → totalCost = 0

### Tests d'intégration

- [ ] `GET /api/v1/super-admin/usage` → 403 avec is_super_admin=false
- [ ] `GET /api/v1/super-admin/usage` → 200 avec is_super_admin=true, données agrégées correctes par workspace
- [ ] Workspace sans usage_events → totalCost = 0.0 dans la réponse

### Isolation workspace

- [ ] Non applicable — accès intentionnel à tous les workspaces

---

## Dépendances

### Subfeatures bloquantes

- SF-25-01 — statut : done

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- La requête d'agrégation est une JPQL native sur `usage_events` avec LEFT JOIN sur `workspaces`
- Les workspaces sans usage sont inclus via LEFT JOIN
- Nouveau DTO `SuperAdminUsageResponse` dans le package `superadmin`
- Réutilise le contrôle super-admin déjà en place dans `SuperAdminService`
