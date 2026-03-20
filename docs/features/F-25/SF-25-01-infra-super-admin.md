# Mini-spec — F-25 / SF-25-01 Infrastructure super-admin

## Identifiant

`F-25 / SF-25-01`

## Feature parente

`F-25` — Super-admin plateforme

## Statut

`ready`

## Date de création

2026-03-20

## Branche Git

`feat/SF-25-01-infra-super-admin`

---

## Objectif

Ajouter la colonne `is_super_admin` sur la table `users`, créer le garde Spring Security associé, et exposer un endpoint `GET /api/v1/super-admin/workspaces` listant tous les workspaces de la plateforme.

---

## Comportement attendu

### Cas nominal

Un utilisateur avec `is_super_admin = true` appelle `GET /api/v1/super-admin/workspaces` et reçoit la liste de tous les workspaces de la plateforme avec, pour chacun : `id`, `name`, `slug`, `planCode`, `status`, `expiresAt`, `memberCount`, `createdAt`.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Utilisateur non `is_super_admin` | Accès refusé | 403 |
| Utilisateur non authentifié | Non autorisé | 401 |

---

## Critères d'acceptation

- [ ] La colonne `is_super_admin BOOLEAN NOT NULL DEFAULT FALSE` existe sur la table `users`
- [ ] Un utilisateur sans `is_super_admin = true` reçoit 403 sur tous les endpoints `/super-admin/**`
- [ ] `GET /api/v1/super-admin/workspaces` retourne tous les workspaces de la plateforme (pas filtrés par workspace courant)
- [ ] Chaque workspace retourné inclut : `id`, `name`, `slug`, `planCode`, `status`, `expiresAt`, `memberCount`, `createdAt`
- [ ] L'endpoint est protégé par Spring Security (`@PreAuthorize` ou filtre dédié)

---

## Périmètre

### Hors scope

- Consommation LLM par workspace (SF-25-02)
- Suppression workspace (SF-25-03)
- Suppression utilisateur (SF-25-04)
- Frontend (SF-25-05)
- Interface pour attribuer `is_super_admin` (opération manuelle en base)

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| is_super_admin | false | Toujours false à la création — attribution manuelle en base uniquement |

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/super-admin/workspaces` | Oui | `is_super_admin = true` |

### Réponse `GET /api/v1/super-admin/workspaces`

```json
[
  {
    "id": "uuid",
    "name": "Cabinet Alpha",
    "slug": "alpha",
    "planCode": "STARTER",
    "status": "ACTIVE",
    "expiresAt": null,
    "memberCount": 3,
    "createdAt": "2026-01-01T10:00:00Z"
  }
]
```

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| users | ALTER — ajout colonne `is_super_admin` | Migration Liquibase |
| workspaces | SELECT | Pas de filtre workspace_id |
| workspace_members | SELECT COUNT | Pour `memberCount` |

### Migration Liquibase

- [x] Oui — `V{version}__add_is_super_admin_to_users.sql`

### Composants Angular (si applicable)

N/A — backend uniquement

---

## Plan de test

### Tests unitaires

- [ ] `SuperAdminService.listAllWorkspaces()` — retourne tous les workspaces avec memberCount
- [ ] `SuperAdminService.listAllWorkspaces()` — retourne liste vide si aucun workspace

### Tests d'intégration

- [ ] `GET /api/v1/super-admin/workspaces` → 200 avec utilisateur `is_super_admin = true`
- [ ] `GET /api/v1/super-admin/workspaces` → 403 avec utilisateur `is_super_admin = false`
- [ ] `GET /api/v1/super-admin/workspaces` → 401 sans authentification
- [ ] Response body contient `memberCount` correct pour chaque workspace

### Isolation workspace

- [ ] Non applicable — l'objectif est précisément de voir TOUS les workspaces sans filtre

---

## Dépendances

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- `is_super_admin` est attribué manuellement en base — pas d'interface d'administration pour cette action en V1
- Le garde est implémenté via un `@PreAuthorize("@superAdminGuard.check(authentication)")` ou via Spring Security filter chain avec un check sur l'entité `User`
- `SuperAdminController` dans le package `fr.ailegalcase.superadmin`
- Pas de filtre `workspace_id` sur cet endpoint — c'est intentionnel et documenté
