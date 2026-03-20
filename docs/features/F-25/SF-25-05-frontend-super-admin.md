# Mini-spec — F-25 / SF-25-05 Frontend super-admin

## Identifiant

`F-25 / SF-25-05`

## Feature parente

`F-25` — Super-admin plateforme

## Statut

`ready`

## Date de création

2026-03-20

## Branche Git

`feat/SF-25-05-frontend-super-admin`

---

## Objectif

Exposer la page `/super-admin` accessible uniquement aux utilisateurs `is_super_admin`,
avec un lien conditionnel dans le header, un tableau des workspaces (plan, membres, usage),
un tableau des utilisateurs, et des actions de suppression avec dialog de confirmation.

---

## Comportement attendu

### Cas nominal

1. Un utilisateur `is_super_admin = true` voit un lien "Super-admin" dans le header.
2. Il navigue vers `/super-admin`.
3. La page charge en parallèle :
   - `GET /api/v1/super-admin/workspaces` → liste des workspaces
   - `GET /api/v1/super-admin/usage` → consommation LLM par workspace
   - `GET /api/v1/super-admin/users` → liste de tous les utilisateurs
4. Tableau **Workspaces** : nom, plan, membres, tokens input/output, coût total, date création, action Supprimer.
5. Tableau **Utilisateurs** : email, prénom/nom, nb workspaces, action Supprimer.
6. Suppression workspace : MatDialog de confirmation → `DELETE /api/v1/super-admin/workspaces/{id}` → snack succès → rechargement.
7. Suppression utilisateur : MatDialog de confirmation → `DELETE /api/v1/super-admin/users/{id}` → snack succès → rechargement.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Utilisateur non `is_super_admin` accède à `/super-admin` | Redirection vers `/case-files` |
| Erreur réseau au chargement | Snack erreur |
| 403 sur un endpoint super-admin | Redirection vers `/case-files` |

---

## Critères d'acceptation

- [ ] Le lien "Super-admin" dans le header est visible si `is_super_admin = true`, absent sinon
- [ ] La route `/super-admin` redirige vers `/case-files` si non super-admin
- [ ] Le tableau des workspaces affiche : nom, plan, membres, coût total, date création
- [ ] Le tableau des utilisateurs affiche : email, nom complet, nb workspaces
- [ ] Suppression workspace : dialog confirmation → DELETE → snack → rechargement
- [ ] Suppression utilisateur : dialog confirmation → DELETE → snack → rechargement
- [ ] `isSuperAdmin` est exposé par `/api/me` et porté par le modèle `User` frontend

---

## Périmètre

### Hors scope

- Pagination des tableaux (nombre limité en V1)
- Tri des colonnes
- Actions en masse (multi-sélection)

---

## Technique

### Endpoints backend (nouveaux)

| Méthode | URL | Auth | Notes |
|---------|-----|------|-------|
| GET | `/api/v1/super-admin/users` | Oui, `is_super_admin` | Nouveau — liste tous les users avec workspaceCount |

### Modifications backend

- `MeResponse` : ajout du champ `isSuperAdmin`
- `MeController.me()` : expose `user.isSuperAdmin()`
- `SuperAdminUserResponse` : nouveau record `(UUID id, String email, String firstName, String lastName, int workspaceCount)`
- `SuperAdminService.listAllUsers()` : charge tous les users, compte leurs workspaces
- `SuperAdminController` : `GET /api/v1/super-admin/users`

### Modifications frontend

- `User` model : ajout `isSuperAdmin: boolean`
- `SuperAdminService` (Angular) : 5 méthodes HTTP (listWorkspaces, getUsage, listUsers, deleteWorkspace, deleteUser)
- `SuperAdminComponent` : page `/super-admin` — deux tableaux + dialogs de confirmation
- `ShellComponent` : lien "Super-admin" conditionnel si `auth.currentUser()?.isSuperAdmin`
- `app.routes.ts` : route `super-admin` dans les children du shell

### Migration Liquibase

- [x] Non applicable

---

## Plan de test

### Tests unitaires (backend)

- [ ] `SuperAdminService.listAllUsers()` — retourne tous les users avec workspaceCount correct
- [ ] `SuperAdminService.listAllUsers()` — non super-admin → 403

### Tests d'intégration (backend)

- [ ] `GET /api/v1/super-admin/users` avec super-admin → 200, liste des users
- [ ] `GET /api/v1/super-admin/users` avec non super-admin → 403
- [ ] `GET /api/me` → `isSuperAdmin` présent dans la réponse

### Tests Karma (frontend)

- [ ] `SuperAdminComponent` — chargement nominal : tableaux affichés
- [ ] `SuperAdminComponent` — 403 → redirection vers `/case-files`
- [ ] `ShellComponent` — lien super-admin visible si `isSuperAdmin = true`
- [ ] `ShellComponent` — lien super-admin absent si `isSuperAdmin = false`

### Isolation workspace

- [ ] Non applicable — page super-admin cross-workspace intentionnelle

---

## Dépendances

### Subfeatures bloquantes

- SF-25-01 — statut : done
- SF-25-02 — statut : done
- SF-25-03 — statut : done
- SF-25-04 — statut : done

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- Le guard de la route `/super-admin` est basé sur `currentUser().isSuperAdmin` (signal déjà chargé par le shell).
- `GET /api/v1/super-admin/users` est nécessaire pour lister les utilisateurs dans la page.
- La suppression d'un workspace ou d'un utilisateur déclenche un rechargement complet de la page.
- Pas de pagination en V1 : le nombre de workspaces et d'utilisateurs est limité.
